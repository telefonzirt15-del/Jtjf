package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuthMainApp(viewModel: AuthViewModel) {
    val context = LocalContext.current
    val allUsers by viewModel.allEntities.collectAsStateWithLifecycle()

    // Reference to the webView so we can notify it of updates
    var webViewRef: WebView? = null

    // Notify HTML web-app in real-time when Room database changes
    LaunchedEffect(allUsers) {
        webViewRef?.post {
            webViewRef?.evaluateJavascript("if (typeof onDatabaseUpdated === 'function') onDatabaseUpdated();", null)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                // Initial trigger to update records once page is fully loaded
                                evaluateJavascript("if (typeof onDatabaseUpdated === 'function') onDatabaseUpdated();", null)
                            }
                        }

                        // Configure standard WebSettings safely for local assets
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        // Register our robust secure Kotlin JS interface
                        addJavascriptInterface(
                            WebAppInterface(ctx, viewModel),
                            "AndroidBridge"
                        )

                        // Load the beautiful local Single Page Application
                        loadUrl("file:///android_asset/index.html")

                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )
        }
    }
}

@Composable
fun ShieldIcon(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    color: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(width * 0.5f, height * 0.1f)
            cubicTo(width * 0.75f, height * 0.1f, width * 0.9f, height * 0.15f, width * 0.9f, height * 0.35f)
            cubicTo(width * 0.9f, height * 0.65f, width * 0.5f, height * 0.9f, width * 0.5f, height * 0.9f)
            cubicTo(width * 0.5f, height * 0.9f, width * 0.1f, height * 0.65f, width * 0.1f, height * 0.35f)
            cubicTo(width * 0.1f, height * 0.15f, width * 0.25f, height * 0.1f, width * 0.5f, height * 0.1f)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )

        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = width * 0.15f,
            center = center
        )
    }
}

