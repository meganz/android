package mega.privacy.android.app.presentation.advertisements.view


import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.advertisements.model.AdsLoadState

/**
 * Compose View to show ads
 * the ads will be shown only if uiState is Loaded with url
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AdsWebView(
    uiState: AdsLoadState,
    onAdClicked: (uri: Uri?) -> Unit,
    onAdDismissed: () -> Unit,
) {
    if (uiState is AdsLoadState.Loaded) {
        Box {
            AndroidView(modifier = Modifier
                .testTag(WEB_VIEW_TEST_TAG)
                .padding(vertical = 20.dp)
                .height(50.dp),
                factory = {
                    WebView(it).apply {
                        settings.javaScriptEnabled = true
                        settings.userAgentString
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                onAdClicked(request?.url)
                                return true
                            }
                        }
                        loadUrl(uiState.url)
                    }
                }
            )

            Image(
                painter = painterResource(id = R.drawable.ic_ads_close),
                contentDescription = null,
                modifier = Modifier
                    .testTag(CLOSE_BUTTON_TEST_TAG)
                    .align(Alignment.TopEnd)
                    .clickable { onAdDismissed() }
            )
        }
    }
}

internal const val WEB_VIEW_TEST_TAG = "ads_web_view:android_view"
internal const val CLOSE_BUTTON_TEST_TAG = "ads_web_view:close_button"

