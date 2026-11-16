package com.tkasmp2027.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import java.net.URI
import java.util.Locale

private const val PUSMENDIK_BASE_URL = "https://pusmendik.kemendikdasmen.go.id/"
private const val PUSMENDIK_HOST = "pusmendik.kemendikdasmen.go.id"
private const val RENOVATION_IMAGE_ASSET = "cbt_images/reference_renovation_building.png"
private const val MATEMATIKA_PAKET_2_ASSET_PREFIX = "cbt_images/matematika_paket_2/"
private const val IMAGE_LOG_TAG = "TkaImageLoad"

private val CACHED_OFFICIAL_IMAGE_ASSETS = buildMap {
    listOf(
        "/tka/cbt_images/95316_dae269e04f8b0f9af69d3e8749c3f29b.png",
        "/tka/cbt_images/90400_dae269e04f8b0f9af69d3e8749c3f29b.png",
        "/tka/cbt_images/30999_dae269e04f8b0f9af69d3e8749c3f29b.png"
    ).forEach { path -> put(path, RENOVATION_IMAGE_ASSET) }

    listOf(
        "56579_f243848bfdf25d16000c8d54e9a10b6c.jpg",
        "56579_2acd540dddff3758f83e84fc4cf37932.jpg",
        "56579_0319187eee890ee75d6bed18baad59f4.jpg",
        "56579_f70e4743bb1152db90898e2a5d9f3f60.jpg",
        "56579_478f9cb2311328eee0271ccf697beb9b.jpg",
        "45215_ad80fefdd28886cfac6e144f2d04fc62.jpg",
        "74495_5dd13f231102ae213e1e415778941ef1.jpg",
        "59990_5e2e8f5da5c7000b28ea76db33139345.jpg",
        "59990_93616fe93e4b426cd4616f180ec749bd.jpg",
        "59990_9562b3cc10f2a24941a7d51140570711.jpg",
        "59990_f307ed6cbe35344643e3249a8ab7cfb7.jpg",
        "59990_3a0cb35210de3fc69253eecefb4f5f43.jpg"
    ).forEach { name ->
        put("/tka/cbt_images/$name", "$MATEMATIKA_PAKET_2_ASSET_PREFIX$name")
    }
}

private val IMAGE_TAG_REGEX = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
private val SRC_ATTRIBUTE_REGEX = Regex(
    """\bsrc\s*=\s*(?:"([^"]*)"|'([^']*)')""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val ALT_ATTRIBUTE_REGEX = Regex(
    """\balt\s*=\s*(?:"([^"]*)"|'([^']*)')""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val TABLE_OPEN_REGEX = Regex("""<table\b""", RegexOption.IGNORE_CASE)
private val TABLE_CLOSE_REGEX = Regex("""</table\s*>""", RegexOption.IGNORE_CASE)

internal fun cachedOfficialImageAssetForUrl(url: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true) ||
        !uri.host.equals(PUSMENDIK_HOST, ignoreCase = true)
    ) {
        return null
    }
    return CACHED_OFFICIAL_IMAGE_ASSETS[uri.path]
}

private fun imageRequestPath(url: String): String =
    runCatching { URI(url).path }.getOrNull() ?: "<unparsed-image-url>"

internal fun readableImageAltFallback(rawAlt: String): String {
    val alt = rawAlt
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .trim()
    if (alt.isBlank()) return "Diagram atau gambar soal tidak dapat dimuat."
    val isMath = alt.any { it == '\\' || it == '^' || it == '_' || it == '{' || it == '}' || it == '$' }
    return if (isMath) {
        val readableMath = LatexAltTextReader(alt).read()
            .replace(Regex("\\s*([+\\-])\\s*"), " $1 ")
            .replace(Regex("\\s+"), " ")
            .trim()
        "Rumus: $readableMath"
    } else {
        "Gambar tidak dapat dimuat: $alt"
    }
}

private class LatexAltTextReader(private val source: String) {
    private var index = 0

    fun read(): String = parseGroup(stopAtClosingBrace = false).trim()

    private fun parseGroup(stopAtClosingBrace: Boolean): String {
        val result = StringBuilder()
        while (index < source.length) {
            when (val char = source[index]) {
                '}' -> {
                    index++
                    if (stopAtClosingBrace) return result.toString()
                    result.append(char)
                }
                '{' -> {
                    index++
                    result.append(parseGroup(stopAtClosingBrace = true))
                }
                '^' -> {
                    index++
                    result.append(toSuperscript(parseArgument()))
                }
                '_' -> {
                    index++
                    result.append(toSubscript(parseArgument()))
                }
                '\\' -> result.append(readCommand())
                '$' -> index++
                else -> {
                    result.append(char)
                    index++
                }
            }
        }
        return result.toString()
    }

    private fun parseArgument(): String {
        while (index < source.length && source[index].isWhitespace()) index++
        if (index >= source.length) return ""
        if (source[index] == '{') {
            index++
            return parseGroup(stopAtClosingBrace = true)
        }
        if (source[index] == '\\') return readCommand()
        return source[index++].toString()
    }

    private fun readCommand(): String {
        index++
        if (index >= source.length) return ""
        if (!source[index].isLetter()) {
            val escaped = source[index++]
            return when (escaped) {
                '{', '}', '_', '%', '&', '#' -> escaped.toString()
                else -> ""
            }
        }
        val start = index
        while (index < source.length && source[index].isLetter()) index++
        return when (val command = source.substring(start, index)) {
            "frac", "dfrac", "tfrac" -> {
                val numerator = parseArgument()
                val denominator = parseArgument()
                val numeratorText = numerator.trim().let { value ->
                    if (value.startsWith("(") && value.endsWith(")")) value else "($value)"
                }
                val denominatorText = denominator.trim().let { value ->
                    if (value.matches(Regex("[\\p{L}\\p{N}⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾]+"))) value else "($value)"
                }
                "$numeratorText / $denominatorText"
            }
            "times" -> " × "
            "cdot" -> " · "
            "le", "leq" -> " ≤ "
            "ge", "geq" -> " ≥ "
            "neq" -> " ≠ "
            "pm" -> " ± "
            "mp" -> " ∓ "
            "div" -> " ÷ "
            "pi" -> "π"
            "infty" -> "∞"
            "sqrt" -> "√(${parseArgument()})"
            "text", "mathrm", "operatorname" -> parseArgument()
            "left", "right", "displaystyle", "textstyle" -> ""
            "alpha" -> "α"
            "beta" -> "β"
            "theta" -> "θ"
            "lambda" -> "λ"
            "mu" -> "μ"
            "sigma" -> "σ"
            "omega" -> "ω"
            else -> command
        }
    }

    private fun toSuperscript(value: String): String {
        val glyphs = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
            '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
            'i' to 'ⁱ', 'n' to 'ⁿ'
        )
        val compact = value.replace(" ", "")
        return compact.map { glyphs[it] ?: return "^($value)" }.joinToString("")
    }

    private fun toSubscript(value: String): String {
        val glyphs = mapOf(
            '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
            '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
            '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
            'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
            'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
            'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
            'v' to 'ᵥ', 'x' to 'ₓ'
        )
        val compact = value.replace(" ", "")
        return compact.map { glyphs[it] ?: return "_($value)" }.joinToString("")
    }
}

private fun sanitizeEducationalHtml(rawHtml: String): String {
    return rawHtml
        .replace(Regex("<\\?xml[\\s\\S]*?\\?>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<!--[\\s\\S]*?-->"), "")
}

private fun resolvedHttpImageUrls(html: String): Set<String> {
    return IMAGE_TAG_REGEX.findAll(html).mapNotNull { tagMatch ->
        val srcMatch = SRC_ATTRIBUTE_REGEX.find(tagMatch.value) ?: return@mapNotNull null
        val rawSrc = (srcMatch.groups[1]?.value ?: srcMatch.groups[2]?.value)
            ?.replace("&amp;", "&", ignoreCase = true)
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return@mapNotNull null
        runCatching {
            val resolved = URI(PUSMENDIK_BASE_URL).resolve(URI(rawSrc))
            resolved.takeIf {
                it.scheme.equals("https", ignoreCase = true) ||
                    it.scheme.equals("http", ignoreCase = true)
            }?.toString()
        }.getOrNull()
    }.toSet()
}

private fun escapeHtml(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

private fun replaceFailedImages(html: String, failedImageUrls: Set<String>): String {
    if (failedImageUrls.isEmpty()) return html
    return IMAGE_TAG_REGEX.replace(html) { tagMatch ->
        val tag = tagMatch.value
        val srcMatch = SRC_ATTRIBUTE_REGEX.find(tag) ?: return@replace tag
        val rawSrc = (srcMatch.groups[1]?.value ?: srcMatch.groups[2]?.value)
            ?.replace("&amp;", "&", ignoreCase = true)
            ?.trim()
            ?: return@replace tag
        val resolvedUrl = runCatching {
            URI(PUSMENDIK_BASE_URL).resolve(URI(rawSrc)).toString()
        }.getOrNull()
        if (resolvedUrl !in failedImageUrls) return@replace tag

        val altMatch = ALT_ATTRIBUTE_REGEX.find(tag)
        val alt = (altMatch?.groups?.get(1)?.value ?: altMatch?.groups?.get(2)?.value)
            ?.trim()
            .orEmpty()
        val message = readableImageAltFallback(alt)
        "<div class=\"image-fallback\" role=\"img\" aria-label=\"${escapeHtml(message)}\">" +
            escapeHtml(message) +
            "</div>"
    }
}

private fun wrapTablesForHorizontalScroll(html: String): String {
    val wrappedOpenTags = TABLE_OPEN_REGEX.replace(html) {
        "<div class=\"table-scroll\">" + it.value
    }
    return TABLE_CLOSE_REGEX.replace(wrappedOpenTags) {
        it.value + "</div>"
    }
}

private fun cssColor(argb: Int): String {
    return String.format(
        Locale.US,
        "#%02X%02X%02X",
        AndroidColor.red(argb),
        AndroidColor.green(argb),
        AndroidColor.blue(argb)
    )
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
        var failedImageUrls by remember(cleanHtml) { mutableStateOf<Set<String>>(emptySet()) }
        var pageLoadFailed by remember(cleanHtml) { mutableStateOf(false) }
        val imageUrls = remember(cleanHtml) { resolvedHttpImageUrls(cleanHtml) }
        val currentImageFailureHandler = rememberUpdatedState(
            newValue = { failedUrl: String ->
                if (failedUrl in imageUrls) {
                    failedImageUrls = failedImageUrls + failedUrl
                }
            }
        )
        val currentPageFailureHandler = rememberUpdatedState(
            newValue = { pageLoadFailed = true }
        )

        val htmlWithFallbacks = remember(cleanHtml, failedImageUrls) {
            replaceFailedImages(cleanHtml, failedImageUrls)
        }
        val htmlWithScrollableTables = remember(htmlWithFallbacks) {
            wrapTablesForHorizontalScroll(htmlWithFallbacks)
        }
        val textColor = cssColor(MaterialTheme.colorScheme.onSurface.toArgb())
        val surfaceColor = cssColor(MaterialTheme.colorScheme.surface.toArgb())
        val borderColor = cssColor(MaterialTheme.colorScheme.outlineVariant.toArgb())
        val subtleBg = cssColor(MaterialTheme.colorScheme.surfaceVariant.toArgb())
        val mutedText = cssColor(MaterialTheme.colorScheme.onSurfaceVariant.toArgb())
        val surfaceColorArgb = MaterialTheme.colorScheme.surface.toArgb()
        val wrappedHtml = remember(
            htmlWithScrollableTables,
            textColor,
            surfaceColor,
            borderColor,
            subtleBg,
            mutedText
        ) {
            """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <base href="%%BASE_URL%%" />
              <style>
                * { box-sizing: border-box; }
                html, body {
                  width: 100%;
                  max-width: 100%;
                  margin: 0;
                  padding: 0;
                  overflow-x: hidden;
                }
                body {
                  padding: 0;
                  font-family: sans-serif;
                  font-size: 16px;
                  line-height: 1.6;
                  color: %%TEXT_COLOR%%;
                  background: %%SURFACE_COLOR%%;
                  overflow-wrap: anywhere;
                }
                p {
                  margin: 0 0 0.9em;
                  line-height: 1.6;
                }
                p:last-child { margin-bottom: 0; }
                .table-scroll {
                  display: block;
                  width: 100%;
                  max-width: 100%;
                  overflow-x: auto;
                  overscroll-behavior-x: contain;
                  -webkit-overflow-scrolling: touch;
                  margin: 8px 0;
                }
                table {
                  width: max-content;
                  min-width: 100%;
                  border-collapse: collapse;
                  margin: 0;
                  font-size: 14px;
                }
                th, td {
                  border: 1px solid %%BORDER_COLOR%%;
                  padding: 7px 9px;
                  text-align: left;
                  overflow-wrap: anywhere;
                }
                th {
                  background: %%SUBTLE_BG%%;
                  font-weight: 600;
                }
                img, svg, math {
                  max-width: 100% !important;
                }
                img, svg {
                  height: auto !important;
                  vertical-align: middle;
                }
                .image-fallback {
                  display: block;
                  width: 100%;
                  max-width: 100%;
                  margin: 8px 0;
                  padding: 12px;
                  border: 1px dashed %%BORDER_COLOR%%;
                  border-radius: 6px;
                  background: %%SUBTLE_BG%%;
                  color: %%MUTED_TEXT%%;
                  font-size: 14px;
                  text-align: center;
                  overflow-wrap: anywhere;
                }
                code {
                  font-family: monospace;
                  background: %%SUBTLE_BG%%;
                  padding: 1px 4px;
                  border-radius: 4px;
                  overflow-wrap: anywhere;
                }
                pre {
                  max-width: 100%;
                  overflow-x: auto;
                  white-space: pre;
                }
                sup { vertical-align: super; }
                sub { vertical-align: sub; }
              </style>
            </head>
            <body>$htmlWithScrollableTables</body>
            </html>
            """.trimIndent()
                .replace("%%BASE_URL%%", PUSMENDIK_BASE_URL)
                .replace("%%TEXT_COLOR%%", textColor)
                .replace("%%SURFACE_COLOR%%", surfaceColor)
                .replace("%%BORDER_COLOR%%", borderColor)
                .replace("%%SUBTLE_BG%%", subtleBg)
                .replace("%%MUTED_TEXT%%", mutedText)
        }

        if (pageLoadFailed) {
            Surface(
                modifier = modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "Konten soal tidak dapat ditampilkan.",
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            AndroidView(
                modifier = modifier.fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(surfaceColorArgb)
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                val assetPath = if (request.method.equals("GET", ignoreCase = true)) {
                                    cachedOfficialImageAssetForUrl(request.url.toString())
                                } else {
                                    null
                                }
                                if (assetPath != null) {
                                    val assetStream = runCatching {
                                        view.context.assets.open(assetPath)
                                    }.getOrNull()
                                    if (assetStream != null) {
                                        val mimeType = when (assetPath.substringAfterLast('.').lowercase(Locale.US)) {
                                            "jpg", "jpeg" -> "image/jpeg"
                                            "png" -> "image/png"
                                            else -> "application/octet-stream"
                                        }
                                        Log.i(
                                            IMAGE_LOG_TAG,
                                            "Served verified local image asset=$assetPath path=${request.url.path}"
                                        )
                                        return WebResourceResponse(mimeType, null, assetStream)
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                super.onReceivedError(view, request, error)
                                Log.e(
                                    IMAGE_LOG_TAG,
                                    "WebView resource error code=${error.errorCode} description=${error.description} path=${imageRequestPath(request.url.toString())}"
                                )
                                if (request.isForMainFrame) {
                                    currentPageFailureHandler.value()
                                } else {
                                    currentImageFailureHandler.value(request.url.toString())
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: WebResourceResponse
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (errorResponse.statusCode >= 400) {
                                    Log.e(
                                        IMAGE_LOG_TAG,
                                        "WebView HTTP error status=${errorResponse.statusCode} reason=${errorResponse.reasonPhrase} path=${imageRequestPath(request.url.toString())}"
                                    )
                                    if (request.isForMainFrame) {
                                        currentPageFailureHandler.value()
                                    } else {
                                        currentImageFailureHandler.value(request.url.toString())
                                    }
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView,
                                handler: SslErrorHandler,
                                error: SslError
                            ) {
                                Log.w(
                                    IMAGE_LOG_TAG,
                                    "Rejected SSL error ${error.primaryError} path=${imageRequestPath(error.url)}"
                                )
                                handler.cancel()
                                if (error.url in imageUrls) {
                                    currentImageFailureHandler.value(error.url)
                                } else if (error.url == PUSMENDIK_BASE_URL) {
                                    currentPageFailureHandler.value()
                                }
                            }
                        }
                    }
                },
                update = { webView ->
                    webView.setBackgroundColor(surfaceColorArgb)
                    if (webView.tag != wrappedHtml) {
                        webView.tag = wrappedHtml
                        webView.loadDataWithBaseURL(
                            PUSMENDIK_BASE_URL,
                            wrappedHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )
        }
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
