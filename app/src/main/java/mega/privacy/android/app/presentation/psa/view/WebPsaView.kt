package mega.privacy.android.app.presentation.psa.view

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.app.presentation.psa.model.PsaJavascriptInterface
import mega.privacy.android.app.presentation.psa.model.PsaState


/**
 * Web psa view
 *
 * @param psa
 * @param content
 * @param markAsSeen
 */
@Composable
fun WebPsaView(
    psa: PsaState.WebPsa,
    content: @Composable () -> Unit,
    markAsSeen: () -> Unit,
    loadPage: (WebView, String) -> Unit = { view, url -> view.loadUrl(url) },
    javascriptInterfaceFactory: (
        onShowPsa: () -> Unit,
        onHidePsa: () -> Unit,
    ) -> PsaJavascriptInterface = ::PsaJavascriptInterface
) {
    var webViewVisible by remember { mutableStateOf(false) }

    val onShowPsa = {
        webViewVisible = true
    }
    val onHidePsa = {
        markAsSeen()
    }

    BackHandler(webViewVisible) {
        markAsSeen()
    }

    Box(modifier = Modifier.testTag(WebPsaTag)) {
        if (webViewVisible.not()) content()
        WebView(
            webViewVisible = webViewVisible,
            jsInterface = javascriptInterfaceFactory(onShowPsa, onHidePsa),
            loadPage = { webView -> loadPage(webView, psa.url) }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebView(
    webViewVisible: Boolean,
    jsInterface: PsaJavascriptInterface,
    loadPage: (WebView) -> Unit,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    SideEffect {
        webViewRef?.visibility = if (webViewVisible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    AndroidView(
        modifier = Modifier.testTag(WebPsaWebViewTag),
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        Intent(Intent.ACTION_VIEW, request.url).apply {
                            context.startActivity(this)
                        }
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                    }
                }
                addJavascriptInterface(
                    jsInterface,
                    PsaJavascriptInterface.INTERFACE_NAME
                )
                visibility = View.GONE
                webViewRef = this
            }
        }, update = {
            loadPage(it)
        })

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                removeJavascriptInterface(PsaJavascriptInterface.INTERFACE_NAME)
                destroy()
            }
        }
    }
}


internal const val WebPsaTag = "web_psa_view"
internal const val WebPsaWebViewTag = "web_psa_view:web_view"