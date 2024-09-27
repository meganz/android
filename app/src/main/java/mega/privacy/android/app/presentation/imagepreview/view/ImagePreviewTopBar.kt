package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white

@Composable
internal fun ImagePreviewTopBar(
    modifier: Modifier = Modifier,
    imageNode: ImageNode,
    showSlideshowMenu: suspend (ImageNode) -> Boolean,
    showForwardMenu: suspend (ImageNode) -> Boolean,
    showSaveToDeviceMenu: suspend (ImageNode) -> Boolean,
    showManageLinkMenu: suspend (ImageNode) -> Boolean,
    showMagnifierMenu: suspend (ImageNode) -> Boolean,
    showSendToMenu: suspend (ImageNode) -> Boolean,
    showMoreMenu: suspend (ImageNode) -> Boolean,
    onClickBack: () -> Unit,
    onClickSlideshow: () -> Unit,
    onClickForward: () -> Unit,
    onClickSaveToDevice: () -> Unit,
    onClickGetLink: () -> Unit,
    onClickMagnifier: () -> Unit,
    onClickSendTo: () -> Unit,
    onClickMore: () -> Unit,
    backgroundColour: Color,
) {
    TopAppBar(
        title = {},
        modifier = modifier,
        backgroundColor = backgroundColour,
        navigationIcon = {
            IconButton(onClick = onClickBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = "Image Preview Back",
                    tint = MaterialTheme.colors.black_white,
                    modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_BACK),
                )
            }
        },
        actions = {
            val isSlideshowMenuVisible by produceState(false, imageNode) {
                value = showSlideshowMenu(imageNode)
            }

            val isForwardMenuVisible by produceState(false, imageNode) {
                value = showForwardMenu(imageNode)
            }

            val isSaveToDeviceMenuVisible by produceState(false, imageNode) {
                value = showSaveToDeviceMenu(imageNode)
            }

            val isManageLinkMenuVisible by produceState(false, imageNode) {
                value = showManageLinkMenu(imageNode)
            }

            val isMagnifierMenuVisible by produceState(false, imageNode) {
                value = showMagnifierMenu(imageNode)
            }

            val isSendToMenuVisible by produceState(false, imageNode) {
                value = showSendToMenu(imageNode)
            }

            val isMoreMenuVisible by produceState(false, imageNode) {
                value = showMoreMenu(imageNode)
            }

            if (isSlideshowMenuVisible) {
                IconButton(onClick = onClickSlideshow) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_slideshow),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SLIDESHOW),
                    )
                }
            }

            if (isForwardMenuVisible) {
                IconButton(onClick = onClickForward) {
                    Icon(
                        painter = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_corner_up_right_medium_regular_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_FORWARD),
                    )
                }
            }

            if (isSaveToDeviceMenuVisible) {
                IconButton(onClick = onClickSaveToDevice) {
                    Icon(
                        painter = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_download_medium_regular_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SAVE_TO_DEVICE),
                    )
                }
            }

            if (isManageLinkMenuVisible) {
                IconButton(onClick = onClickGetLink) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_link),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_MANAGE_LINK),
                    )
                }
            }

            if (isMagnifierMenuVisible) {
                IconButton(onClick = onClickMagnifier) {
                    Icon(
                        painter = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_magnifier),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_MAGNIFIER),
                    )
                }
            }

            if (isSendToMenuVisible) {
                IconButton(onClick = onClickSendTo) {
                    Icon(
                        painter = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_message_arrow_up_medium_regular_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_SEND_TO),
                    )
                }
            }

            if (isMoreMenuVisible) {
                IconButton(onClick = onClickMore) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = null,
                        tint = MaterialTheme.colors.black_white,
                        modifier = Modifier.testTag(IMAGE_PREVIEW_APP_BAR_MORE),
                    )
                }
            }
        },
        elevation = 0.dp,
    )
}