package com.chronie.homemoney.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.chronie.homemoney.data.local.dao.ExpenseDao
import com.chronie.homemoney.domain.sync.DeviceInfo
import com.chronie.homemoney.domain.sync.DeviceSyncData
import com.chronie.homemoney.domain.sync.DeviceSyncManager
import com.chronie.homemoney.domain.sync.SyncProgressInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URL
import java.util.Collections
import java.util.Enumeration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 局域网设备间同步管理器
 * 基于TCP/IP实现设备间同步，支持Android 8-16
 */
class LanDeviceSyncManager(
    private val context: Context,
    expenseDao: ExpenseDao,
    gson: Gson,
    private val wifiManager: WifiManager
) : BaseDeviceSyncManager(expenseDao, gson) {
    
    companion object {
        private const val DISCOVERY_BROADCAST_PORT = 12345  // 发现广播端口（发送和监听发现请求）
        private const val DISCOVERY_RESPONSE_PORT = 12347   // 发现响应端口（接收响应）
        private const val HTTP_SYNC_PORT = 8080             // HTTP同步端口
        private const val BROADCAST_INTERVAL = 500L
        private const val BROADCAST_COUNT = 10
        private const val DISCOVERY_TIMEOUT = 30000L
        private const val HTTP_CONNECTION_TIMEOUT = 8000
        private const val HTTP_CONNECTION_RETRIES = 3
        private const val TAG = "LanDeviceSyncManager"
        private const val HTTP_TIMEOUT = 30000
        private const val MULTICAST_GROUP = "239.255.255.250"
        private const val SYNC_ENDPOINT = "/sync"
        private const val PING_ENDPOINT = "/ping"
    }
    
    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var isServerRunning = AtomicBoolean(false)
    private val discoveredDevices = ConcurrentHashMap<String, DeviceInfo>()
    private var broadcastThread: Thread? = null
    private var serverThread: Thread? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // 同步锁，防止同时作为客户端和服务器进行同步
    private val syncLock = Object()
    @Volatile
    private var isSyncing = false

    // 同步进度状态流
    private val _syncProgress = kotlinx.coroutines.flow.MutableStateFlow(SyncProgressInfo())
    override val syncProgress: kotlinx.coroutines.flow.StateFlow<SyncProgressInfo> = _syncProgress.asStateFlow()

    // 同步请求回调
    private var syncRequestCallback: com.chronie.homemoney.domain.sync.SyncRequestCallback? = null
    private var pendingSyncResponse: kotlin.coroutines.Continuation<Boolean>? = null

    // 设备唯一标识
    private val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_sync_id", null) ?: run {
            val newId = "android_${UUID.randomUUID().toString().substring(0, 8)}"
            prefs.edit().putString("device_sync_id", newId).apply()
            newId
        }
    }
    
    // 设备名称（使用用户自定义名称或设备型号）
    private val deviceName: String by lazy {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_custom_name", null) ?: Build.MODEL ?: "Android Device"
    }
    
    /**
     * 设置设备自定义名称
     */
    fun setDeviceCustomName(name: String) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("device_custom_name", name).apply()
    }
    
    /**
     * 获取设备自定义名称
     */
    fun getDeviceCustomName(): String? {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return prefs.getString("device_custom_name", null)
    }

    /**
     * 更新同步进度（用于服务器端通知UI）
     */
    override fun updateSyncProgress(progress: Float, message: String, isActive: Boolean) {
        _syncProgress.value = SyncProgressInfo(
            progress = progress,
            message = message,
            isActive = isActive,
            deviceName = deviceName
        )
    }

    /**
     * 清除同步进度
     */
    override fun clearSyncProgress() {
        _syncProgress.value = SyncProgressInfo()
    }

    /**
     * 设置同步请求回调
     */
    override fun setSyncRequestCallback(callback: com.chronie.homemoney.domain.sync.SyncRequestCallback?) {
        syncRequestCallback = callback
    }

    /**
     * 响应同步请求
     */
    override fun respondToSyncRequest(accepted: Boolean) {
        Log.d(TAG, "respondToSyncRequest called with accepted: $accepted, pendingSyncResponse: ${pendingSyncResponse != null}")
        pendingSyncResponse?.resume(accepted)
        pendingSyncResponse = null
    }

    /**
     * 检查WiFi是否连接
     */
    private fun isWifiConnected(): Boolean {
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo != null && wifiInfo.networkId != -1
        }
    }
    
    /**
     * 获取本地IP地址
     */
    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in Collections.list(interfaces)) {
                val addresses = networkInterface.inetAddresses
                for (address in Collections.list(addresses)) {
                    if (!address.isLoopbackAddress && address is InetAddress && address.hostAddress.indexOf(':') < 0) {
                        val ip = address.hostAddress
                        // 优先返回WiFi地址（通常是192.168.x.x或10.x.x.x）
                        if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                            return ip
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
            null
        }
    }
    
    /**
     * 获取广播地址列表
     */
    private fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        
        try {
            // 添加标准广播地址
            addresses.add(InetAddress.getByName("255.255.255.255"))
            addresses.add(InetAddress.getByName(MULTICAST_GROUP))
            
            // 获取WiFi子网的广播地址
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                val dhcpInfo = wifiManager.dhcpInfo
                if (dhcpInfo != null) {
                    val ip = dhcpInfo.ipAddress
                    val mask = dhcpInfo.netmask
                    val broadcast = ip or (mask.inv())
                    
                    val quads = ByteArray(4)
                    for (k in 0..3) {
                        quads[k] = (broadcast shr (k * 8)).toByte()
                    }
                    
                    try {
                        val subnetBroadcast = InetAddress.getByAddress(quads)
                        if (!addresses.contains(subnetBroadcast)) {
                            addresses.add(subnetBroadcast)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to create subnet broadcast address", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting broadcast addresses", e)
        }
        
        return addresses
    }
    
    override fun searchDevices(): Flow<DeviceInfo> = flow {
        Log.d(TAG, "Starting device search on LAN")
        discoveredDevices.clear()

        if (!isWifiConnected()) {
            Log.w(TAG, "WiFi not connected, cannot search devices")
            return@flow
        }

        val localIp = getLocalIpAddress()
        if (localIp == null) {
            Log.w(TAG, "Cannot get local IP address")
            return@flow
        }

        Log.d(TAG, "Local IP: $localIp")

        // 确保服务器已启动（如果尚未启动）
        startSyncServer()

        // 创建用于发送广播和接收响应的 socket，绑定到固定端口
        val discoverySocket = java.net.DatagramSocket(DISCOVERY_RESPONSE_PORT)

        // 发送广播发现其他设备
        sendDiscoveryBroadcast(localIp, discoverySocket)

        // 监听其他设备的响应
        val startTime = System.currentTimeMillis()

        try {
            discoverySocket.soTimeout = 1000
            val buffer = ByteArray(1024)

            while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT) {
                try {
                    val packet = java.net.DatagramPacket(buffer, buffer.size)
                    discoverySocket.receive(packet)

                    val message = String(packet.data, 0, packet.length)
                    val senderAddress = packet.address.hostAddress

                    // 忽略自己的广播
                    if (senderAddress == localIp) {
                        continue
                    }

                    val deviceInfo = parseDiscoveryResponse(message, senderAddress)
                    if (deviceInfo != null && !discoveredDevices.containsKey(deviceInfo.deviceId)) {
                        // 使用数据包的源地址作为连接地址（更可靠）
                        val deviceWithCorrectAddress = deviceInfo.copy(address = senderAddress)
                        discoveredDevices[deviceInfo.deviceId] = deviceWithCorrectAddress
                        Log.d(TAG, "Discovered device: ${deviceWithCorrectAddress.deviceName} at ${deviceWithCorrectAddress.address} (from packet)")
                        emit(deviceWithCorrectAddress)
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    // 超时继续
                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving discovery response", e)
                }
            }
        } finally {
            discoverySocket.close()
        }

        Log.d(TAG, "Device search completed, found ${discoveredDevices.size} devices")
    }.flowOn(Dispatchers.IO)
    
    /**
     * 发送发现广播
     * @param localIp 本地IP地址
     * @param socket 用于发送广播的socket（已绑定到特定端口）
     */
    private fun sendDiscoveryBroadcast(localIp: String, socket: java.net.DatagramSocket) {
        Thread {
            try {
                val broadcastAddresses = getBroadcastAddresses()
                val message = createDiscoveryMessage(localIp)
                val data = message.toByteArray()

                // 设置广播权限
                socket.broadcast = true

                for (i in 0 until BROADCAST_COUNT) {
                    try {
                        for (address in broadcastAddresses) {
                            try {
                                val packet = java.net.DatagramPacket(
                                    data,
                                    data.size,
                                    address,
                                    DISCOVERY_BROADCAST_PORT
                                )
                                socket.send(packet)
                                Log.d(TAG, "Sent discovery broadcast to ${address.hostAddress}:$DISCOVERY_BROADCAST_PORT from port ${socket.localPort}")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to send to ${address.hostAddress}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending discovery broadcast", e)
                    }

                    if (i < BROADCAST_COUNT - 1) {
                        Thread.sleep(BROADCAST_INTERVAL)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in discovery broadcast thread", e)
            }
        }.start()
    }
    
    /**
     * 创建发现消息
     */
    private fun createDiscoveryMessage(localIp: String): String {
        return "DISCOVERY|$deviceId|$deviceName|$localIp|${System.currentTimeMillis()}"
    }
    
    /**
     * 解析发现响应
     */
    private fun parseDiscoveryResponse(message: String, address: String): DeviceInfo? {
        return try {
            val parts = message.split("|")
            if (parts.size >= 4 && parts[0] == "DISCOVERY") {
                DeviceInfo(
                    deviceId = parts[1],
                    deviceName = parts[2],
                    deviceType = "ANDROID",
                    connectionType = "LAN",
                    address = parts[3],
                    signalStrength = 80
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing discovery response", e)
            null
        }
    }
    
    private var discoveryResponseSocket: java.net.DatagramSocket? = null
    private var discoveryResponseThread: Thread? = null

    /**
     * 启动同步服务器
     */
    fun startSyncServer() {
        if (isServerRunning.get()) {
            return
        }

        isServerRunning.set(true)

        // 启动 HTTP 服务器处理同步请求
        serverThread = Thread {
            try {
                serverSocket = ServerSocket(HTTP_SYNC_PORT)
                Log.d(TAG, "HTTP sync server started on port $HTTP_SYNC_PORT")

                while (isServerRunning.get()) {
                    try {
                        serverSocket?.soTimeout = 5000
                        val clientSocket = serverSocket?.accept()

                        if (clientSocket != null) {
                            GlobalScope.launch(Dispatchers.IO) {
                                handleHttpClientConnection(clientSocket)
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // 超时继续循环
                    } catch (e: SocketException) {
                        if (isServerRunning.get()) {
                            Log.e(TAG, "Server socket error", e)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sync server", e)
            } finally {
                Log.d(TAG, "Sync server stopped")
            }
        }
        serverThread?.start()

        // 启动 UDP 发现响应服务器
        startDiscoveryResponseServer()
    }

    /**
     * 启动发现响应服务器（监听其他设备的发现请求并响应）
     */
    private fun startDiscoveryResponseServer() {
        discoveryResponseThread = Thread {
            try {
                // 监听发现广播端口
                discoveryResponseSocket = java.net.DatagramSocket(DISCOVERY_BROADCAST_PORT)
                discoveryResponseSocket?.soTimeout = 1000
                val buffer = ByteArray(1024)

                Log.d(TAG, "Discovery response server started on port $DISCOVERY_BROADCAST_PORT")

                while (isServerRunning.get()) {
                    try {
                        val packet = java.net.DatagramPacket(buffer, buffer.size)
                        discoveryResponseSocket?.receive(packet)

                        val message = String(packet.data, 0, packet.length)
                        val senderAddress = packet.address.hostAddress
                        val localIp = getLocalIpAddress()

                        // 忽略自己的广播
                        if (senderAddress == localIp) {
                            continue
                        }

                        // 解析发现请求
                        val parts = message.split("|")
                        if (parts.size >= 2 && parts[0] == "DISCOVERY") {
                            Log.d(TAG, "Received discovery request from $senderAddress:${packet.port}")

                            // 发送响应到请求的来源端口（搜索方监听的端口）
                            val responseMessage = createDiscoveryMessage(localIp ?: "")
                            val responseData = responseMessage.toByteArray()
                            val responsePacket = java.net.DatagramPacket(
                                responseData,
                                responseData.size,
                                packet.address,
                                packet.port
                            )
                            discoveryResponseSocket?.send(responsePacket)
                            Log.d(TAG, "Sent discovery response to $senderAddress:${packet.port}")
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // 超时继续循环
                    } catch (e: Exception) {
                        if (isServerRunning.get()) {
                            Log.e(TAG, "Error in discovery response server", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting discovery response server", e)
            } finally {
                discoveryResponseSocket?.close()
                Log.d(TAG, "Discovery response server stopped")
            }
        }
        discoveryResponseThread?.start()
    }
    
    /**
     * 停止同步服务器
     */
    fun stopSyncServer() {
        isServerRunning.set(false)
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sync server", e)
        }
        try {
            discoveryResponseSocket?.close()
            discoveryResponseSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery response server", e)
        }
    }
    
    /**
     * 处理 HTTP 客户端连接
     */
    private suspend fun handleHttpClientConnection(clientSocket: Socket) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            Log.d(TAG, "HTTP client connected: ${clientSocket.inetAddress.hostAddress}")
            clientSocket.soTimeout = HTTP_TIMEOUT

            inputStream = clientSocket.getInputStream()
            outputStream = clientSocket.getOutputStream()

            // 读取 HTTP 请求行和请求头（使用原始 InputStream 避免缓冲问题）
            val requestLine = readLine(inputStream!!)
            if (requestLine == null) {
                Log.w(TAG, "Empty request from client")
                sendHttpResponse(outputStream, 400, "Bad Request", "Empty request")
                return
            }

            Log.d(TAG, "Received HTTP request: $requestLine")

            // 解析 HTTP 请求行
            val requestParts = requestLine.split(" ")
            if (requestParts.size < 3) {
                Log.w(TAG, "Invalid request format: $requestLine")
                sendHttpResponse(outputStream, 400, "Bad Request", "Invalid request format")
                return
            }

            val method = requestParts[0]
            val path = requestParts[1]
            val protocol = requestParts[2]

            // 读取请求头
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (readLine(inputStream!!).also { line = it } != null && line!!.isNotEmpty()) {
                val headerParts = line!!.split(": ", limit = 2)
                if (headerParts.size == 2) {
                    headers[headerParts[0]] = headerParts[1]
                }
            }

            // 处理不同的 HTTP 请求
            when (path) {
                SYNC_ENDPOINT -> {
                    if (method == "POST") {
                        handleHttpSyncRequest(clientSocket, inputStream!!, outputStream, headers)
                    } else {
                        sendHttpResponse(outputStream, 405, "Method Not Allowed", "Only POST method is allowed for /sync")
                    }
                }
                PING_ENDPOINT -> {
                    if (method == "GET") {
                        handleHttpPingRequest(outputStream)
                    } else {
                        sendHttpResponse(outputStream, 405, "Method Not Allowed", "Only GET method is allowed for /ping")
                    }
                }
                else -> {
                    Log.w(TAG, "Unknown path: $path")
                    sendHttpResponse(outputStream, 404, "Not Found", "Path not found")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling HTTP client connection", e)
            try {
                outputStream?.let {
                    sendHttpResponse(it, 500, "Internal Server Error", "Server error: ${e.message}")
                }
            } catch (e2: Exception) {
                // 忽略发送错误
            }
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                clientSocket.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing resources", e)
            }
        }
    }

    /**
     * 从 InputStream 中读取一行
     */
    private fun readLine(inputStream: InputStream): String? {
        val buffer = StringBuilder()
        var byte: Int
        while (inputStream.read().also { byte = it } != -1) {
            val char = byte.toChar()
            if (char == '\r') {
                // 读取下一个字符，如果是 \n 则返回
                val nextByte = inputStream.read()
                if (nextByte == -1) {
                    break
                }
                val nextChar = nextByte.toChar()
                if (nextChar == '\n') {
                    return buffer.toString()
                } else {
                    // 不是 \n，将 \r 添加到缓冲区并继续
                    buffer.append(char)
                    buffer.append(nextChar)
                }
            } else if (char == '\n') {
                return buffer.toString()
            } else {
                buffer.append(char)
            }
        }
        return if (buffer.isNotEmpty()) buffer.toString() else null
    }

    /**
     * 发送 HTTP 响应
     */
    private fun sendHttpResponse(outputStream: OutputStream, statusCode: Int, statusMessage: String, content: String) {
        val response = "HTTP/1.1 $statusCode $statusMessage\r\nContent-Type: text/plain\r\nContent-Length: ${content.length}\r\nConnection: close\r\n\r\n$content"
        outputStream.write(response.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    /**
     * 处理 HTTP 同步请求
     */
    private suspend fun handleHttpSyncRequest(
        clientSocket: Socket,
        inputStream: InputStream,
        outputStream: OutputStream,
        headers: Map<String, String>
    ) {
        // 使用同步锁防止并发同步
        synchronized(syncLock) {
            if (isSyncing) {
                Log.w(TAG, "Already syncing, rejecting request")
                sendHttpResponse(outputStream, 429, "Too Many Requests", "Device is busy syncing")
                return
            }
            isSyncing = true
        }

        try {
            val clientAddress = clientSocket.inetAddress.hostAddress
            Log.d(TAG, "Starting sync as server with client: $clientAddress")

            // 从请求头中获取设备信息
            val clientDeviceId = headers["X-Device-Id"] ?: "unknown"
            val clientDeviceName = headers["X-Device-Name"] ?: "Unknown Device"

            // 如果有回调，先询问用户是否接受同步
            val callback = syncRequestCallback
            if (callback != null) {
                Log.d(TAG, "Asking user to accept sync request from $clientDeviceName")

                // 通知UI显示确认对话框
                val requestInfo = com.chronie.homemoney.domain.sync.SyncRequestInfo(
                    deviceId = clientDeviceId,
                    deviceName = clientDeviceName,
                    address = clientAddress
                )

                // 使用挂起函数等待用户响应
                val accepted = try {
                    Log.d(TAG, "Starting to wait for user response...")
                    kotlinx.coroutines.withTimeout(30000L) { // 30秒超时
                        kotlin.coroutines.suspendCoroutine<Boolean> { continuation ->
                            Log.d(TAG, "Setting pendingSyncResponse")
                            pendingSyncResponse = continuation
                            // 启动协程调用回调
                            kotlinx.coroutines.GlobalScope.launch {
                                Log.d(TAG, "Launching callback coroutine")
                                val result = try {
                                    Log.d(TAG, "Calling callback.onSyncRequest")
                                    callback.onSyncRequest(requestInfo)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in sync request callback", e)
                                    false
                                }
                                Log.d(TAG, "Callback returned result: $result")
                                respondToSyncRequest(result)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Sync request timeout or error", e)
                    false
                }

                Log.d(TAG, "User response received: $accepted")

                if (!accepted) {
                    Log.d(TAG, "User rejected sync request from $clientDeviceName")
                    sendHttpResponse(outputStream, 403, "Forbidden", "Sync request rejected")
                    updateSyncProgress(1f, "同步请求被拒绝", false)
                    return
                }

                Log.d(TAG, "User accepted sync request from $clientDeviceName")
            }

            // 通知UI显示同步进度（服务器端作为被搜索方也需要显示）
            updateSyncProgress(0.1f, "正在接收来自 ${clientDeviceName} 的数据...", true)

            // 读取请求体长度
            val contentLength = headers["Content-Length"]?.toIntOrNull()
            if (contentLength == null || contentLength <= 0) {
                Log.w(TAG, "Invalid content length")
                sendHttpResponse(outputStream, 400, "Bad Request", "Invalid content length")
                updateSyncProgress(1f, "接收失败：未收到数据长度", false)
                return
            }

            Log.d(TAG, "Expecting $contentLength bytes of data")
            updateSyncProgress(0.3f, "正在接收数据 (${contentLength} 字节)...", true)

            // 读取请求体数据
            val dataBytes = ByteArray(contentLength)
            var bytesRead = 0
            try {
                val startTime = System.currentTimeMillis()
                while (bytesRead < contentLength) {
                    // 检查超时（累计时间）
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed > HTTP_TIMEOUT * 2) {
                        throw IOException("Read timeout after ${elapsed}ms")
                    }
                    val read = inputStream.read(dataBytes, bytesRead, contentLength - bytesRead)
                    if (read == -1) {
                        throw IOException("Unexpected end of stream")
                    }
                    bytesRead += read
                }
                Log.d(TAG, "Successfully read $bytesRead bytes")
                updateSyncProgress(0.6f, "数据接收完成，正在处理...", true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read request body", e)
                sendHttpResponse(outputStream, 400, "Bad Request", "Failed to read request body: ${e.message}")
                updateSyncProgress(1f, "接收失败：${e.message}", false)
                return
            }

            val clientDataJson = String(dataBytes, Charsets.UTF_8)
            Log.d(TAG, "Received client data, length: ${clientDataJson.length}")
            val clientData = gson.fromJson(clientDataJson, DeviceSyncData::class.java)
            Log.d(TAG, "Received sync data from ${clientData.deviceName}, entities: ${clientData.entities.size}")

            // 处理客户端数据
            val downloadResult = processDeviceData(clientData)

            updateSyncProgress(0.7f, "正在准备本地数据...", true)

            // 准备本地数据
            val localData = prepareLocalData()
            val localDataJson = gson.toJson(localData)
            val localDataBytes = localDataJson.toByteArray(Charsets.UTF_8)
            Log.d(TAG, "Prepared local data: ${localDataBytes.size} bytes")

            updateSyncProgress(0.8f, "正在发送数据到 ${clientDeviceName}...", true)

            // 发送 HTTP 响应
            val responseHeaders = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${localDataBytes.size}\r\nConnection: close\r\n\r\n"
            try {
                outputStream.write(responseHeaders.toByteArray(Charsets.UTF_8))
                outputStream.write(localDataBytes)
                outputStream.flush()
                Log.d(TAG, "Sent response to client: ${localDataBytes.size} bytes")

                updateSyncProgress(1f, "与 ${clientDeviceName} 同步完成！", false)
                Log.d(TAG, "Sync completed with client")

                // 等待一小段时间确保数据被发送和显示
                kotlinx.coroutines.delay(3000) // 增加等待时间，确保用户能看到完成状态
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send response", e)
                updateSyncProgress(1f, "发送失败：${e.message}", false)
            } finally {
                try {
                    outputStream.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing output stream", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling sync request", e)
            updateSyncProgress(1f, "同步失败：${e.message}", false)
            try {
                sendHttpResponse(outputStream, 500, "Internal Server Error", "Sync error: ${e.message}")
            } catch (e2: Exception) {
                // 忽略写入错误
            }
        } finally {
            isSyncing = false
            // 延迟清除进度，让用户能看到完成状态
            kotlinx.coroutines.delay(2000)
            clearSyncProgress()
        }
    }

    /**
     * 处理 HTTP 心跳请求
     */
    private fun handleHttpPingRequest(outputStream: OutputStream) {
        val responseBody = "{\"deviceId\":\"$deviceId\",\"deviceName\":\"$deviceName\"}"
        val response = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${responseBody.length}\r\nConnection: close\r\n\r\n$responseBody"
        outputStream.write(response.toByteArray(Charsets.UTF_8))
        outputStream.flush()
        Log.d(TAG, "Sent ping response")
    }
    

    
    override suspend fun connect(device: DeviceInfo): Boolean {
        Log.d(TAG, "Connecting to device: ${device.deviceName} at ${device.address}:$HTTP_SYNC_PORT")

        var lastException: Exception? = null

        for (attempt in 1..HTTP_CONNECTION_RETRIES) {
            try {
                val result = withContext(Dispatchers.IO) {
                    // 发送 HTTP GET 请求到 /ping 端点来验证设备是否可达
                    val url = URL("http://${device.address}:$HTTP_SYNC_PORT$PING_ENDPOINT")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = HTTP_CONNECTION_TIMEOUT
                    connection.readTimeout = HTTP_TIMEOUT
                    connection.requestMethod = "GET"

                    val responseCode = connection.responseCode
                    connection.disconnect()
                    responseCode == HttpURLConnection.HTTP_OK
                }

                if (result) {
                    isConnected = true
                    currentDevice = device
                    Log.d(TAG, "Successfully connected to device ${device.deviceName}")
                    return true
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Connection attempt $attempt failed: ${e.message}")

                if (attempt < HTTP_CONNECTION_RETRIES) {
                    delay(1000L * attempt)
                }
            }
        }

        Log.e(TAG, "Failed to connect after $HTTP_CONNECTION_RETRIES attempts", lastException)
        return false
    }
    
    override suspend fun disconnect(): Boolean {
        Log.d(TAG, "Disconnecting from device")

        return try {
            withContext(Dispatchers.IO) {
                // HTTP 是无状态的，不需要关闭连接，只需重置状态
                isConnected = false
                currentDevice = null
                Log.d(TAG, "Disconnected successfully")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
            isConnected = false
            currentDevice = null
            false
        }
    }
    
    override suspend fun sendData(data: DeviceSyncData): Boolean {
        Log.d(TAG, "Sending data to device, entities count: ${data.entities.size}")

        return try {
            withContext(Dispatchers.IO) {
                val device = currentDevice ?: run {
                    Log.e(TAG, "Current device is null, cannot send data")
                    return@withContext false
                }

                // 发送 HTTP POST 请求到 /sync 端点
                val url = URL("http://${device.address}:$HTTP_SYNC_PORT$SYNC_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = HTTP_CONNECTION_TIMEOUT
                connection.readTimeout = HTTP_TIMEOUT
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Device-Id", deviceId)
                connection.setRequestProperty("X-Device-Name", deviceName)
                connection.doOutput = true

                // 准备数据
                val dataJson = gson.toJson(data)
                val dataBytes = dataJson.toByteArray(Charsets.UTF_8)
                connection.setRequestProperty("Content-Length", dataBytes.size.toString())

                // 发送数据
                val outputStream = connection.outputStream
                outputStream.write(dataBytes)
                outputStream.flush()
                outputStream.close()

                Log.d(TAG, "Sent data: ${dataBytes.size} bytes")

                // 获取响应
                val responseCode = connection.responseCode
                Log.d(TAG, "Server response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Data sent successfully")
                    true
                } else {
                    Log.e(TAG, "Failed to send data, server returned: $responseCode")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data", e)
            false
        }
    }
    
    override suspend fun receiveData(): DeviceSyncData? {
        Log.d(TAG, "Receiving data from device")

        return try {
            withContext(Dispatchers.IO) {
                val device = currentDevice ?: return@withContext null

                // 发送 HTTP POST 请求到 /sync 端点并接收响应
                val url = URL("http://${device.address}:$HTTP_SYNC_PORT$SYNC_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = HTTP_CONNECTION_TIMEOUT
                connection.readTimeout = HTTP_TIMEOUT
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Device-Id", deviceId)
                connection.setRequestProperty("X-Device-Name", deviceName)
                connection.doOutput = true

                // 准备本地数据
                val localData = prepareLocalData()
                val dataJson = gson.toJson(localData)
                val dataBytes = dataJson.toByteArray(Charsets.UTF_8)
                connection.setRequestProperty("Content-Length", dataBytes.size.toString())

                // 发送数据
                val outputStream = connection.outputStream
                outputStream.write(dataBytes)
                outputStream.flush()
                outputStream.close()

                // 获取响应
                val responseCode = connection.responseCode
                Log.d(TAG, "Server response code: $responseCode")

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Server returned error: $responseCode")
                    return@withContext null
                }

                // 读取响应数据
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val responseData = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    responseData.append(line)
                }
                reader.close()
                inputStream.close()

                val responseJson = responseData.toString()
                Log.d(TAG, "Received data, length: ${responseJson.length}, parsing...")
                gson.fromJson(responseJson, DeviceSyncData::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to receive data", e)
            null
        }
    }

    /**
     * 执行与设备的双向同步（带同步锁）
     */
    override suspend fun syncWithDevice(device: DeviceInfo): com.chronie.homemoney.domain.model.SyncResult {
        Log.d(TAG, "Starting sync with device: ${device.deviceName} at ${device.address}")

        // 检查是否已经在进行同步
        synchronized(syncLock) {
            if (isSyncing) {
                Log.w(TAG, "Already syncing, cannot start new sync")
                return createFailedSyncResult("Device is busy syncing")
            }
            isSyncing = true
        }

        return try {
            withContext(Dispatchers.IO) {
                // 1. 建立连接
                Log.d(TAG, "Step 1: Connecting to device...")
                if (!connect(device)) {
                    Log.e(TAG, "Failed to connect to device")
                    return@withContext createFailedSyncResult("Failed to connect to device")
                }
                Log.d(TAG, "Connected successfully")

                // 2. 获取本地数据
                Log.d(TAG, "Step 2: Preparing local data...")
                val localData = prepareLocalData()
                Log.d(TAG, "Local data prepared: ${localData.entities.size} entities")

                // 3. 发送本地数据到设备并接收设备数据（HTTP 一次请求完成双向同步）
                Log.d(TAG, "Step 3: Sending data to device and receiving response...")
                
                // 发送 HTTP POST 请求到 /sync 端点
                val url = URL("http://${device.address}:$HTTP_SYNC_PORT$SYNC_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = HTTP_CONNECTION_TIMEOUT * 2 // 增加超时时间以等待用户授权
                connection.readTimeout = HTTP_TIMEOUT * 2 // 增加超时时间以等待用户授权
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Device-Id", deviceId)
                connection.setRequestProperty("X-Device-Name", deviceName)
                connection.doOutput = true

                // 准备数据
                val dataJson = gson.toJson(localData)
                val dataBytes = dataJson.toByteArray(Charsets.UTF_8)
                connection.setRequestProperty("Content-Length", dataBytes.size.toString())

                // 发送数据
                val outputStream = connection.outputStream
                outputStream.write(dataBytes)
                outputStream.flush()
                // 不要在这里关闭输出流，因为还需要读取响应
                // outputStream.close()
                
                // 通知UI显示同步进度（发送数据后）
                updateSyncProgress(0.1f, "正在等待 ${device.deviceName} 响应...", true)
                
                // 4. 获取响应
                Log.d(TAG, "Step 4: Receiving response from device...")
                val responseCode = connection.responseCode
                Log.d(TAG, "Server response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    Log.e(TAG, "Sync request rejected by user")
                    disconnect()
                    return@withContext createFailedSyncResult("Sync request rejected by user")
                } else if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Failed to sync, server returned: $responseCode")
                    disconnect()
                    return@withContext createFailedSyncResult("Server returned error: $responseCode")
                }

                // 5. 读取响应数据
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val responseData = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    responseData.append(line)
                }
                reader.close()
                inputStream.close()
                connection.disconnect()

                val responseJson = responseData.toString()
                Log.d(TAG, "Received data, length: ${responseJson.length}, parsing...")
                val deviceData = gson.fromJson(responseJson, DeviceSyncData::class.java)
                Log.d(TAG, "Data received: ${deviceData.entities.size} entities")

                // 6. 处理设备数据
                Log.d(TAG, "Step 5: Processing device data...")
                updateSyncProgress(0.3f, "正在处理 ${device.deviceName} 的数据...", true)
                val downloadResult = processDeviceData(deviceData)
                Log.d(TAG, "Data processed: ${downloadResult.newItems} new, ${downloadResult.updatedItems} updated")

                // 7. 断开连接
                Log.d(TAG, "Step 6: Disconnecting...")
                disconnect()
                Log.d(TAG, "Disconnected")

                // 8. 返回同步结果
                Log.d(TAG, "Sync completed successfully")
                updateSyncProgress(1f, "同步完成！", false)
                com.chronie.homemoney.domain.model.SyncResult(
                    success = true,
                    uploadResult = com.chronie.homemoney.domain.model.UploadResult(
                        totalItems = localData.entities.size,
                        successCount = localData.entities.size,
                        failedCount = 0
                    ),
                    downloadResult = downloadResult,
                    conflicts = downloadResult.conflicts
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync with device failed", e)
            disconnect()
            createFailedSyncResult(e.message ?: "Unknown error")
        } finally {
            isSyncing = false
        }
    }

    /**
     * 创建失败的同步结果
     */
    private fun createFailedSyncResult(error: String): com.chronie.homemoney.domain.model.SyncResult {
        return com.chronie.homemoney.domain.model.SyncResult(
            success = false,
            uploadResult = com.chronie.homemoney.domain.model.UploadResult(0, 0, 0),
            downloadResult = com.chronie.homemoney.domain.model.DownloadResult(0, 0, 0),
            error = error
        )
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopSyncServer()
        GlobalScope.launch {
            disconnect()
        }
    }
}
