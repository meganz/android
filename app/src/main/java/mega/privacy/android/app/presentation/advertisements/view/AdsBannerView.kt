package mega.privacy.android.app.presentation.advertisements.view

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.advertisements.model.AdsUIState
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.drawableId
import java.net.URL

/**
 * Compose View to show ads
 * the ads will be shown only if uiState is Loaded with url
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AdsBannerView(
    uiState: AdsUIState,
    onAdClicked: (uri: Uri?) -> Unit,
    onAdsWebpageLoaded: () -> Unit,
    onAdDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val url = uiState.adsBannerUrl

    if (uiState.showAdsView) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .testTag(WEB_VIEW_TEST_TAG)
        ) {
            val webpageBackgroundColor = MaterialTheme.colors.surface.toArgb()
            AndroidView(modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 20.dp)
                .size(width = 320.dp, height = 50.dp),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(webpageBackgroundColor)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                onAdsWebpageLoaded()
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                request?.url?.host?.let { targetDomain ->
                                    val currentDomain = URL(view?.url).host
                                    if (currentDomain.equals(targetDomain)) {
                                        return false
                                    }
                                    onAdClicked(request.url)
                                    return true
                                }
                                return false
                            }
                        }
                    }
                }, update = {
                    it.loadUrl(url)
                }
            )
            Image(
                painter = painterResource(id = R.drawable.ic_ads_close),
                contentDescription = "",
                modifier = Modifier
                    .semantics { drawableId = R.drawable.ic_ads_close }
                    .align(Alignment.TopEnd)
                    .testTag(CLOSE_BUTTON_TEST_TAG)
                    .clickable { onAdDismissed() }
            )
        }
    }
}

internal const val WEB_VIEW_TEST_TAG = "ads_web_view:android_view"
internal const val CLOSE_BUTTON_TEST_TAG = "ads_web_view:close_button"