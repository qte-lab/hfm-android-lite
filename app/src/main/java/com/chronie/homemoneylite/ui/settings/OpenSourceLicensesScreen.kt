package com.chronie.homemoneylite.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chronie.homemoneylite.ui.components.CircularIconButton

data class LibraryInfo(
    val name: String,
    val version: String,
    val license: String,
    val licenseUrl: String,
    val projectUrl: String
)

val libraries = listOf(
    LibraryInfo(
        name = "Kotlin Stdlib",
        version = "2.3.21",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://kotlinlang.org/"
    ),
    LibraryInfo(
        name = "Kotlin Coroutines Android",
        version = "1.11.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    LibraryInfo(
        name = "AndroidX Core KTX",
        version = "1.18.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/core"
    ),
    LibraryInfo(
        name = "AndroidX AppCompat",
        version = "1.7.1",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/appcompat"
    ),
    LibraryInfo(
        name = "AndroidX CoordinatorLayout",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/coordinatorlayout"
    ),
    LibraryInfo(
        name = "AndroidX Core Splashscreen",
        version = "1.2.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/core"
    ),
    LibraryInfo(
        name = "AndroidX Activity Compose",
        version = "1.13.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/activity"
    ),
    LibraryInfo(
        name = "JUnit",
        version = "4.13.2",
        license = "Eclipse Public License 1.0",
        licenseUrl = "https://www.eclipse.org/legal/epl-v10.html",
        projectUrl = "https://junit.org/junit4/"
    ),
    LibraryInfo(
        name = "AndroidX Test JUnit",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/test"
    ),
    LibraryInfo(
        name = "AndroidX Test Espresso",
        version = "3.7.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/test"
    ),
    LibraryInfo(
        name = "Jetpack Compose BOM",
        version = "2026.05.01",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/compose-bom"
    ),
    LibraryInfo(
        name = "M3Color",
        version = "2026.1",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Kyant0/M3Color"
    ),
    LibraryInfo(
        name = "Google Material Components",
        version = "1.14.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/material-components/material-components-android"
    ),
    LibraryInfo(
        name = "AndroidX Material3",
        version = "1.5.0-alpha20",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/compose-material3"
    ),
    LibraryInfo(
        name = "AndroidX Material3 Window Size Class",
        version = "1.5.0-alpha20",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/compose-material3"
    ),
    LibraryInfo(
        name = "AndroidX Lifecycle Runtime Compose",
        version = "2.10.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle"
    ),
    LibraryInfo(
        name = "AndroidX Lifecycle ViewModel Compose",
        version = "2.10.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle"
    ),
    LibraryInfo(
        name = "AndroidX Navigation Compose",
        version = "2.9.8",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/navigation"
    ),
    LibraryInfo(
        name = "Dagger Hilt Android",
        version = "2.59.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://dagger.dev/hilt/"
    ),
    LibraryInfo(
        name = "AndroidX Hilt Navigation Compose",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/hilt"
    ),
    LibraryInfo(
        name = "AndroidX Datastore Preferences",
        version = "1.2.1",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/datastore"
    ),
    LibraryInfo(
        name = "AndroidX Room Runtime",
        version = "2.8.4",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/room"
    ),
    LibraryInfo(
        name = "AndroidX Room KTX",
        version = "2.8.4",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/room"
    ),
    LibraryInfo(
        name = "Retrofit",
        version = "3.0.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/square/retrofit"
    ),
    LibraryInfo(
        name = "Retrofit Gson Converter",
        version = "3.0.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/square/retrofit/tree/master/retrofit-converters/gson"
    ),
    LibraryInfo(
        name = "OkHttp Logging Interceptor",
        version = "5.3.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/square/okhttp"
    ),
    LibraryInfo(
        name = "AndroidX Paging Runtime KTX",
        version = "3.5.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/paging"
    ),
    LibraryInfo(
        name = "AndroidX Paging Compose",
        version = "3.5.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/paging"
    ),
    LibraryInfo(
        name = "Coil Compose",
        version = "2.7.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/coil-kt/coil"
    ),
    LibraryInfo(
        name = "AndroidX Security Crypto",
        version = "1.1.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/security"
    ),
    LibraryInfo(
        name = "SQLCipher Android",
        version = "4.16.0",
        license = "BSD 3-Clause License",
        licenseUrl = "https://opensource.org/licenses/BSD-3-Clause",
        projectUrl = "https://www.zetetic.net/sqlcipher/"
    ),
    LibraryInfo(
        name = "AndroidX SQLite",
        version = "2.6.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/sqlite"
    ),
    LibraryInfo(
        name = "AndroidX Work Runtime KTX",
        version = "2.11.2",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/work"
    ),
    LibraryInfo(
        name = "AndroidX Hilt Work",
        version = "1.3.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/hilt"
    ),
    LibraryInfo(
        name = "FastExcel",
        version = "0.20.1",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/dhatim/fastexcel"
    ),
    LibraryInfo(
        name = "FastExcel Reader",
        version = "0.20.1",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/dhatim/fastexcel"
    ),
    LibraryInfo(
        name = "Aalto XML",
        version = "1.4.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/FasterXML/aalto-xml"
    ),
    LibraryInfo(
        name = "XZ",
        version = "1.12",
        license = "Public Domain",
        licenseUrl = "https://tukaani.org/xz/legal.html",
        projectUrl = "https://tukaani.org/xz/"
    ),
    LibraryInfo(
        name = "UCrop",
        version = "2.2.11",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Yalantis/uCrop"
    ),
    LibraryInfo(
        name = "MockK",
        version = "1.14.9",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://mockk.io/"
    ),
    LibraryInfo(
        name = "Kotlin Coroutines Test",
        version = "1.11.0",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        projectUrl = "https://github.com/Kotlin/kotlinx.coroutines"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    context: Context,
    onNavigateBack: () -> Unit = {}
) {
    val htmlContent = remember(libraries) {
        buildLicenseHtml(libraries)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(com.chronie.homemoneylite.R.string.open_source_licenses)) },
                navigationIcon = {
                    CircularIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = context.getString(com.chronie.homemoneylite.R.string.back)
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { webContext ->
                WebView(webContext).apply {
                    settings.javaScriptEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                view?.context?.startActivity(intent)
                            } catch (_: Exception) {
                            }
                            return true
                        }
                    }
                    loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
            }
        )
    }
}

private fun buildLicenseHtml(libraries: List<LibraryInfo>): String {
    val items = libraries.joinToString("") { library ->
        """
            <div class="card">
                <h3>${escapeHtml(library.name)}</h3>
                <p><strong>Version:</strong> ${escapeHtml(library.version)}</p>
                <p><strong>License:</strong> ${escapeHtml(library.license)}</p>
                <div class="actions">
                    <a href="${escapeHtml(library.licenseUrl)}" target="_blank">License</a>
                    <a href="${escapeHtml(library.projectUrl)}" target="_blank">Project</a>
                </div>
            </div>
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                    margin: 0;
                    padding: 16px;
                    background: #f7f7f7;
                    color: #1f1f1f;
                }
                .card {
                    background: white;
                    border-radius: 12px;
                    padding: 16px;
                    margin-bottom: 12px;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.12);
                }
                h3 { margin: 0 0 8px; font-size: 16px; }
                p { margin: 4px 0; font-size: 14px; }
                .actions {
                    margin-top: 8px;
                    display: flex;
                    gap: 8px;
                }
                a {
                    display: inline-block;
                    padding: 6px 10px;
                    border: 1px solid #c7c7c7;
                    border-radius: 999px;
                    text-decoration: none;
                    color: #1565c0;
                    font-size: 13px;
                }
            </style>
        </head>
        <body>
            $items
        </body>
        </html>
    """.trimIndent()
}

private fun escapeHtml(value: String): String {
    return buildString {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }
}