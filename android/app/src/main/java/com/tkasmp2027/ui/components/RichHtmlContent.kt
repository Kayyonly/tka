package com.tkasmp2027.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

private const val PUSMENDIK_BASE_URL = "https://pusmendik.kemendikdasmen.go.id/"

private fun sanitizeEducationalHtml(rawHtml: String): String {
    return rawHtml
        .replace(Regex("<\\?xml[\\s\\S]*?\\?>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<!--[\\s\\S]*?-->"), "")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichHtmlContent(
    html: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val cleanHtml = remember(html) { sanitizeEducationalHtml(html) }
    val requiresWebView = remember(cleanHtml) {
        cleanHtml.contains("<table", ignoreCase = true) ||
            cleanHtml.contains("<img", ignoreCase = true) ||
            cleanHtml.contains("<svg", ignoreCase = true) ||
            cleanHtml.contains("<math", ignoreCase = true)
    }

    if (requiresWebView) {
        val wrappedHtml = remember(cleanHtml) {
            """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <base href="$PUSMENDIK_BASE_URL" />
              <style>
                body {
                  margin: 0;
                  padding: 0;
                  font-family: sans-serif;
                  font-size: 15px;
                  line-height: 1.55;
                  color: #111827;
                  background: transparent;
                }
                table {
                  width: 100%;
                  border-collapse: collapse;
                  margin: 8px 0;
                  font-size: 13px;
                }
                th, td {
                  border: 1px solid #CBD5E1;
                  padding: 6px 8px;
                  text-align: left;
                }
                th {
                  background: #F1F5F9;
                  font-weight: 600;
                }
                img {
                  max-width: 100% !important;
                  height: auto !important;
                  vertical-align: middle;
                }
                code {
                  font-family: monospace;
                  background: #F1F5F9;
                  padding: 1px 4px;
                  border-radius: 4px;
                }
              </style>
            </head>
            <body>$cleanHtml</body>
            </html>
            """.trimIndent()
        }

        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    PUSMENDIK_BASE_URL,
                    wrappedHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        )
    } else {
        val plain = remember(cleanHtml) {
            HtmlCompat.fromHtml(cleanHtml, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
        }
        Text(
            text = plain,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier
        )
    }
}
