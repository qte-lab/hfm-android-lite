package com.chronie.homemoneylite.ui.expense

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.*
import com.chronie.homemoneylite.ui.components.AppDatePickerDialog
import com.chronie.homemoneylite.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberImagePainter
import com.chronie.homemoneylite.R
import com.chronie.homemoneylite.ui.components.ExpressiveLoadingIndicator
import com.chronie.homemoneylite.ui.components.CircularIconButton
import com.chronie.homemoneylite.domain.model.AIExpenseRecord
import com.chronie.homemoneylite.domain.model.ExpenseType
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.yalantis.ucrop.UCrop
import android.content.Intent

/**
 * AI 智能记录界面
 */
@Composable
fun AIExpenseScreen(
    context: Context,
    onNavigateBack: () -> Unit,
    onRecordsSaved: () -> Unit,
    viewModel: AIExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 裁剪图片启动器
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            // 从uCrop获取裁剪后的图片URI
            val outputUri = UCrop.getOutput(it.data ?: Intent())
            outputUri?.let { it ->
                viewModel.addImages(listOf(it))
                // 删除临时文件
                val file = File(it.path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
    
    // 用于已有图片裁剪的启动器
    val existingImageCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            // 从uCrop获取裁剪后的图片URI
            val outputUri = UCrop.getOutput(it.data ?: Intent())
            outputUri?.let {
                viewModel.addImages(listOf(it))
                // 删除临时文件
                val file = File(it.path ?: "")
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) {
        it.forEach { uri ->
            // 启动裁剪
            try {
                // 创建临时文件用于保存裁剪结果
                val timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                val imageFileName = "CROP_${timeStamp}_"
                val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val image = File(storageDir, "$imageFileName.jpg")
                val outputUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    image
                )
                // 配置uCrop
                val options = UCrop.Options()
                options.setCompressionQuality(90)
                options.setHideBottomControls(false)
                options.setFreeStyleCropEnabled(true)
                // 设置工具栏和状态栏颜色，避免与状态栏重叠
                options.setToolbarColor(android.graphics.Color.parseColor("#6750A4"))
                options.setActiveControlsWidgetColor(android.graphics.Color.WHITE)
                // 确保裁剪界面正确处理状态栏空间
                options.setToolbarTitle("")
                options.setToolbarWidgetColor(android.graphics.Color.WHITE)
                // 为顶部工具栏添加额外padding，确保不占用状态栏空间
                options.setDimmedLayerColor(android.graphics.Color.parseColor("#80000000"))
                options.setShowCropGrid(false)
                options.setShowCropFrame(true)
                // 启动裁剪
                val uCrop = UCrop.of(uri, outputUri)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(1080, 1080)
                    .withOptions(options)
                cropLauncher.launch(uCrop.getIntent(context))
            } catch (e: Exception) {
                Log.e("AIExpenseScreen", "Failed to start crop", e)
            }
        }
    }

    // 相机拍摄临时文件URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // 相机拍摄启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) {
        if (it) {
            // 拍摄成功，将图片添加到选择列表
            cameraImageUri?.let { uri ->
                // 启动裁剪
                try {
                    // 创建临时文件用于保存裁剪结果
                    val timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                    val imageFileName = "CROP_${timeStamp}_"
                    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val image = File(storageDir, "$imageFileName.jpg")
                    val outputUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        image
                    )
                    // 配置uCrop
                    val options = UCrop.Options()
                    options.setCompressionQuality(90)
                    options.setHideBottomControls(false)
                    options.setFreeStyleCropEnabled(true)
                    // 设置工具栏和状态栏颜色，避免与状态栏重叠
                    options.setToolbarColor(android.graphics.Color.parseColor("#6750A4"))
                    options.setActiveControlsWidgetColor(android.graphics.Color.WHITE)
                    // 确保裁剪界面正确处理状态栏空间
                    options.setToolbarTitle("")
                    options.setToolbarWidgetColor(android.graphics.Color.WHITE)
                    // 为顶部工具栏添加额外padding，确保不占用状态栏空间
                    options.setDimmedLayerColor(android.graphics.Color.parseColor("#80000000"))
                    options.setShowCropGrid(false)
                    options.setShowCropFrame(true)
                    // 启动裁剪
                    val uCrop = UCrop.of(uri, outputUri)
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(1080, 1080)
                        .withOptions(options)
                    cropLauncher.launch(uCrop.getIntent(context))
                } catch (e: Exception) {
                    Log.e("AIExpenseScreen", "Failed to start crop", e)
                }
            }
        }
    }
    
    // 处理已有图片裁剪
    fun handleCropExistingImage(uri: Uri) {
        try {
            // 从列表中移除旧图片
            viewModel.removeImage(uri)
            // 创建临时文件用于保存裁剪结果
            val timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
            val imageFileName = "CROP_${timeStamp}_"
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val image = File(storageDir, "$imageFileName.jpg")
            val outputUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                image
            )
            // 配置uCrop
            val options = UCrop.Options()
            options.setCompressionQuality(90)
                options.setHideBottomControls(false)
                options.setFreeStyleCropEnabled(true)
                options.setToolbarColor(android.graphics.Color.parseColor("#6750A4"))
                options.setActiveControlsWidgetColor(android.graphics.Color.WHITE)
            // 确保裁剪界面正确处理状态栏空间
            options.setToolbarTitle("")
            options.setToolbarWidgetColor(android.graphics.Color.WHITE)
            // 为顶部工具栏添加额外padding，确保不占用状态栏空间
            options.setDimmedLayerColor(android.graphics.Color.parseColor("#80000000"))
            options.setShowCropGrid(false)
            options.setShowCropFrame(true)
            // 启动裁剪
            val uCrop = UCrop.of(uri, outputUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(1080, 1080)
                .withOptions(options)
            existingImageCropLauncher.launch(uCrop.getIntent(context))
        } catch (e: Exception) {
            Log.e("AIExpenseScreen", "Failed to start crop", e)
        }
    }

    // 创建临时文件用于相机拍摄
    fun createImageFile(context: Context): Uri? {
        val TAG = "AIExpenseScreen"
        return try {
            val timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            
            Log.d(TAG, "Storage dir: $storageDir")
            
            // 确保存储目录存在
            if (storageDir?.exists() != true) {
                Log.d(TAG, "Creating storage dir: ${storageDir?.mkdirs()}")
            }
            
            // 创建文件
            val image = File(storageDir, "$imageFileName.jpg")
            
            Log.d(TAG, "Image file path: ${image.absolutePath}")
            
            // 如果文件已存在，删除它
            if (image.exists()) {
                Log.d(TAG, "Deleting existing file: ${image.delete()}")
            }
            
            // 确保文件被正确创建
            if (image.createNewFile()) {
                Log.d(TAG, "File created successfully")
                // 使用FileProvider创建URI，避免FileUriExposedException
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    image
                )
                Log.d(TAG, "Created URI: $uri")
                uri
            } else {
                Log.e(TAG, "Failed to create file")
                null
            }
        } catch (ex: IOException) {
            Log.e(TAG, "IOException in createImageFile: ${ex.message}", ex)
            null
        } catch (ex: Exception) {
            Log.e(TAG, "Exception in createImageFile: ${ex.message}", ex)
            null
        }
    }

    // 相机权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            // 权限授予，启动相机
            cameraImageUri = createImageFile(context)
            cameraImageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.ai_expense_title)) },
                navigationIcon = {
                    CircularIconButton(onClick = onNavigateBack, modifier = Modifier.padding(start = 8.dp, end = 4.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp))
                },
                backgroundColor = MaterialTheme.colors.background
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 图片选择区域
            ImageSelectionSection(
                context = context,
                selectedImages = uiState.selectedImages,
                onCameraSelected = {
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasCameraPermission) {
                        cameraImageUri = createImageFile(context)
                        cameraImageUri?.let {
                            cameraLauncher.launch(it)
                        }
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                },
                onGallerySelected = {
                    imagePickerLauncher.launch("image/*")
                },
                onRemoveImage = viewModel::removeImage,
                onCropImage = ::handleCropExistingImage
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 文本输入区域
            TextInputSection(
                context = context,
                textInput = uiState.textInput,
                onTextChange = viewModel::updateTextInput
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 识别按钮
            Button(
                onClick = { viewModel.startRecognition() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && 
                         (uiState.selectedImages.isNotEmpty() || uiState.textInput.isNotBlank())
            ) {
                if (uiState.isLoading) {
                    ExpressiveLoadingIndicator(size = 20.dp, containerVisible = false)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (uiState.isLoading) 
                        context.getString(R.string.ai_expense_recognizing) 
                    else 
                        context.getString(R.string.ai_expense_start_recognition)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 识别结果列表
            if (uiState.recognizedRecords.isNotEmpty()) {
                RecognizedRecordsSection(
                    context = context,
                    records = uiState.recognizedRecords,
                    onUpdateRecord = viewModel::updateRecord,
                    onDeleteRecord = viewModel::deleteRecord,
                    onSaveAll = { viewModel.saveAllRecords(onRecordsSaved) },
                    isSaving = uiState.isSaving
                )
            }
            
            // 错误提示
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * 图片选择区域
 */
@Composable
private fun ImageSelectionSection(
    context: Context,
    selectedImages: List<Uri>,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onCropImage: (Uri) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.ai_expense_select_images),
                style = MaterialTheme.typography.titleMedium
            )
            Box {
                TextButton(onClick = { showDropdown = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(context.getString(R.string.ai_expense_add_images))
                }
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            showDropdown = false
                            onCameraSelected()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(context.getString(R.string.ai_expense_take_photo))
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            showDropdown = false
                            onGallerySelected()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(context.getString(R.string.ai_expense_choose_from_gallery))
                        }
                    }
                }
            }
        }
        
        if (selectedImages.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(selectedImages, key = { _, uri -> uri }) { index, uri ->
                    ImagePreviewCard(
                        imageUri = uri,
                        onRemove = { onRemoveImage(uri) },
                        onCrop = { onCropImage(uri) }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { showDropdown = true },
                backgroundColor = MaterialTheme.colors.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colors.outline)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurfaceVariant
                        )
                        Text(
                            context.getString(R.string.ai_expense_click_to_add),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图片预览卡片
 */
@Composable
private fun ImagePreviewCard(
    imageUri: Uri,
    onRemove: () -> Unit,
    onCrop: () -> Unit
) {
    Card(
        modifier = Modifier.size(100.dp)
    ) {
        Box {
            Image(
                painter = rememberImagePainter(imageUri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        onCrop()
                    },
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

/**
 * 文本输入区域
 */
@Composable
private fun TextInputSection(
    context: Context,
    textInput: String,
    onTextChange: (String) -> Unit
) {
    Column {
        Text(
            text = context.getString(R.string.ai_expense_or_input_text),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = textInput,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text(context.getString(R.string.ai_expense_text_hint)) },
            maxLines = 5
        )
    }
}

/**
 * 识别结果区域
 */
@Composable
private fun RecognizedRecordsSection(
    context: Context,
    records: List<AIExpenseRecord>,
    onUpdateRecord: (Int, AIExpenseRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    onSaveAll: () -> Unit,
    isSaving: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.ai_expense_records_count, records.size),
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onSaveAll,
                enabled = !isSaving && records.any { it.isValid }
            ) {
                if (isSaving) {
                    ExpressiveLoadingIndicator(size = 16.dp, containerVisible = false)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isSaving) 
                        context.getString(R.string.ai_expense_saving) 
                    else 
                        context.getString(R.string.ai_expense_save_all)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(records, key = { _, record -> record.id }) { index, record ->
                RecordEditCard(
                    context = context,
                    record = record,
                    onUpdate = { updated -> onUpdateRecord(index, updated) },
                    onDelete = { onDeleteRecord(index) }
                )
            }
        }
    }
}


/**
 * 记录编辑卡片
 */
@Composable
private fun RecordEditCard(
    context: Context,
    record: AIExpenseRecord,
    onUpdate: (AIExpenseRecord) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (record.isValid) 
            MaterialTheme.colors.surface 
        else 
            MaterialTheme.colors.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ExpenseTypeLocalizer.getLocalizedName(context, record.type),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = context.getString(R.string.currency_format, context.getString(R.string.currency_symbol), record.amount),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                    if (record.remark.isNotBlank()) {
                        Text(
                            text = record.remark,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (record.isEdited) {
                        Text(
                            text = context.getString(R.string.ai_expense_edited),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Column {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = context.getString(R.string.ai_expense_edit_record))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = context.getString(R.string.ai_expense_delete_record),
                            tint = MaterialTheme.colors.error
                        )
                    }
                }
            }
        }
    }
    
    if (showEditDialog) {
        RecordEditDialog(
            context = context,
            record = record,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                onUpdate(updated)
                showEditDialog = false
            }
        )
    }
}

/**
 * 记录编辑对话框
 */
@Composable
private fun RecordEditDialog(
    context: Context,
    record: AIExpenseRecord,
    onDismiss: () -> Unit,
    onConfirm: (AIExpenseRecord) -> Unit
) {
    var selectedType by remember { mutableStateOf(record.type) }
    var amount by remember { mutableStateOf(record.amount.toString()) }
    var remark by remember { mutableStateOf(record.remark) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.parse(record.date)) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.ai_expense_edit_record)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 类型选择
                OutlinedButton(
                    onClick = { showTypePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(ExpenseTypeLocalizer.getLocalizedName(context, selectedType))
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                // 金额输入
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(context.getString(R.string.ai_expense_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text(context.getString(R.string.currency_symbol)) }
                )
                
                // 日期选择
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
                
                // 备注输入
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text(context.getString(R.string.ai_expense_remark)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedRecord = record.copy(
                        type = selectedType,
                        amount = amount.toDoubleOrNull() ?: record.amount,
                        date = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        remark = remark,
                        isEdited = true
                    )
                    onConfirm(updatedRecord)
                }
            ) {
                Text(context.getString(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
    
    if (showTypePicker) {
        ExpenseTypePickerDialog(
            context = context,
            selectedType = selectedType,
            onDismiss = { showTypePicker = false },
            onTypeSelected = { type ->
                selectedType = type
                showTypePicker = false
            }
        )
    }
    
    if (showDatePicker) {
        val initialMillis = selectedDate.toEpochDay() * 86400000L
        AppDatePickerDialog(
            initialDateMillis = initialMillis,
            onDateSelected = { millis ->
                selectedDate = java.time.LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * 支出类型选择对话框 - 支持搜索功能
 */
@Composable
private fun ExpenseTypePickerDialog(
    context: Context,
    selectedType: ExpenseType,
    onDismiss: () -> Unit,
    onTypeSelected: (ExpenseType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter types based on search query
    val filteredTypes = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            ExpenseType.values().toList()
        } else {
            ExpenseType.values().filter { type ->
                val displayName = ExpenseTypeLocalizer.getLocalizedName(context, type)
                displayName.contains(searchQuery, ignoreCase = true) ||
                    type.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(context.getString(R.string.ai_expense_select_type))
                if (filteredTypes.size != ExpenseType.values().size) {
                    Text(
                        text = context.getString(R.string.search_results_count, filteredTypes.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(context.getString(R.string.search_category)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = context.getString(R.string.clear))
                            }
                        }
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category list
                if (filteredTypes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.no_results_found),
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        itemsIndexed(filteredTypes, key = { _, type -> type.name }) { index, type ->
                            TextButton(
                                onClick = { onTypeSelected(type) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ExpenseTypeLocalizer.getLocalizedName(context, type),
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (type == selectedType)
                                        MaterialTheme.colors.primary
                                    else
                                        MaterialTheme.colors.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}
