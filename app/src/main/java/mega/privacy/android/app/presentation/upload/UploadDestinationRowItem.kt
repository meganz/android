package mega.privacy.android.app.presentation.upload

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import mega.privacy.android.shared.original.core.ui.controls.images.ThumbnailView
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Upload Row Item
 */
@Composable
fun UploadDestinationRowItem(
    importUiItem: ImportUiItem,
    editFileName: (ImportUiItem?) -> Unit,
    updateFileName: (String) -> Unit,
    isEditMode: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        focusManager.clearFocus()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        ThumbnailView(
            data = importUiItem.filePath,
            contentScale = ContentScale.Crop,
            defaultImage = importUiItem.fileIcon ?: iconPackR.drawable.ic_generic_medium_solid,
            contentDescription = "Edit",
            modifier = Modifier
                .padding(end = 16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        GenericTextField(
            modifier = Modifier
                .onFocusChanged {
                    if (it.isFocused) {
                        editFileName(importUiItem)
                    }
                },
            placeholder = importUiItem.fileName,
            text = importUiItem.fileName.replace("\n", " "),
            errorText = importUiItem.error,
            singleLine = true,
            onTextChange = {
                updateFileName(it)
            },
            keyboardActions = KeyboardActions(
                onDone = {
                    editFileName(null)
                },
            ),
            imeAction = ImeAction.Done,
            trailingIcon = if (isEditMode.not()) {
                {
                    Icon(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(16.dp),
                        painter = painterResource(iconPackR.drawable.ic_pen_02_medium_regular_outline),
                        contentDescription = "Edit",
                        tint = MaterialTheme.colors.onPrimary,
                    )
                }
            } else null,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun UploadRowItemPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        Column {
            UploadDestinationRowItem(
                importUiItem = ImportUiItem(
                    originalFileName = "IMG_20171123_17220.jpg",
                    filePath = "file_path",
                    fileName = "IMG_20171123_17220.jpg",
                ),
                isEditMode = false,
                editFileName = {},
                updateFileName = {},
            )
            UploadDestinationRowItem(
                importUiItem = ImportUiItem(
                    filePath = "file_path",
                    originalFileName = "IMG_20171123_17220.jpg",
                    fileName = "IMG_20171123_17220.jpg",
                ),
                isEditMode = true,
                editFileName = { },
                updateFileName = { },
            )
        }
    }
}