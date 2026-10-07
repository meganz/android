package mega.privacy.android.feature.chat.giphy.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.indicators.LargeSpinnerIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Full screen preview of a GIF, optionally offering to send it to the chat.
 *
 * @param imageUrl URL of the GIF to show.
 * @param title Title of the GIF, used as its content description.
 * @param showSendButton True to show the button that confirms the GIF has to be sent.
 * @param onSend Called when the user confirms the GIF has to be sent.
 */
@Composable
fun GiphyViewerScreen(
    imageUrl: String?,
    title: String?,
    showSendButton: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember(imageUrl) { mutableStateOf(imageUrl != null) }

    MegaScaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (showSendButton) {
                MegaFab(
                    modifier = Modifier.testTag(GIPHY_VIEWER_SEND_TAG),
                    onClick = onSend,
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.SendHorizontal),
                    contentDescription = stringResource(sharedR.string.context_send),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                onSuccess = { isLoading = false },
                onError = { isLoading = false },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(GIPHY_VIEWER_IMAGE_TAG),
            )

            if (isLoading) {
                LargeSpinnerIndicator(modifier = Modifier.testTag(GIPHY_VIEWER_LOADING_TAG))
            }
        }
    }
}

internal const val GIPHY_VIEWER_IMAGE_TAG = "giphy_viewer:image"
internal const val GIPHY_VIEWER_LOADING_TAG = "giphy_viewer:loading"
internal const val GIPHY_VIEWER_SEND_TAG = "giphy_viewer:send"

@CombinedThemePreviews
@Composable
private fun GiphyViewerScreenPreview() {
    AndroidThemeForPreviews {
        GiphyViewerScreen(
            imageUrl = null,
            title = "GIF",
            showSendButton = true,
            onSend = {},
        )
    }
}
