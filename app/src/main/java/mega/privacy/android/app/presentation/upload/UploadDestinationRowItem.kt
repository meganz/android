package mega.privacy.android.app.presentation.upload

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Upload Row Item
 */
@Composable
fun UploadDestinationRowItem(
    fileName: String,
    filePath: String = "",
    isEditMode: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        ThumbnailView(
            data = filePath,
            contentScale = ContentScale.Crop,
            defaultImage = iconPackR.drawable.ic_generic_medium_solid,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentDescription = "Edit",
        )
        if (isEditMode) {
            GenericTextField(
                placeholder = fileName,
                textFieldValue = TextFieldValue(fileName),
                onTextChange = {},
                keyboardActions = KeyboardActions(
                    onDone = {
                        //handle edit operation
                    },
                ),
                imeAction = ImeAction.Done,
            )
        } else {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scroll = rememberScrollState(0)
                MegaText(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scroll),
                    text = fileName,
                    textColor = TextColor.Primary,
                    overflow = LongTextBehaviour.Visible(1)
                )
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp),
                    painter = painterResource(iconPackR.drawable.ic_pen_2_medium_regular_solid),
                    contentDescription = "Edit",
                    tint = MaterialTheme.colors.onPrimary,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun UploadRowItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        Column {
            UploadDestinationRowItem(
                isEditMode = false,
                fileName = "IMG_20171123_17220.jpg"
            )
            UploadDestinationRowItem(
                isEditMode = true,
                fileName = "IMG_20171123_17220.jpg"
            )
        }
    }
}