package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

const val TEST_TAG_GALLERY_LIST = "chat_gallery_list"

/**
 * Chat toolbar bottom sheet
 *
 * @param modifier
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatToolbarBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
) {
    Column(modifier = modifier) {
        ChatGallery(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            sheetState = sheetState
        )
    }
}

/**
 * Chat gallery
 *
 * @param modifier
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatGallery(
    sheetState: ModalBottomSheetState,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.testTag(TEST_TAG_GALLERY_LIST),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item("camera_button") {
            ChatCameraButton(modifier = Modifier.size(88.dp), sheetState = sheetState)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChatToolbarBottomSheetPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatToolbarBottomSheet(sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded))
    }
}