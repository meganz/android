package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@Composable
internal fun ImagePreviewBottomBar(
    modifier: Modifier = Modifier,
    imageName: String,
    imageIndex: String,
    backgroundColour: Color,
) {
    BottomAppBar(
        modifier = modifier,
        backgroundColor = backgroundColour,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiddleEllipsisText(
                text = imageName,
                color = TextColor.Secondary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_BAR_TEXT_IMAGE_NAME),
            )
            MiddleEllipsisText(
                text = imageIndex,
                color = TextColor.Secondary,
                modifier = Modifier.testTag(IMAGE_PREVIEW_BOTTOM_BAR_TEXT_IMAGE_COUNT),
            )
        }
    }
}