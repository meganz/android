package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GiphyPickerActivity
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.core.ui.controls.chat.attachpanel.AttachItem
import mega.privacy.android.core.ui.controls.chat.attachpanel.AttachItemPlaceHolder
import mega.privacy.android.core.ui.preview.CombinedThemePreviews

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
    val context = LocalContext.current
    val galleryPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) {
            //Manage gallery picked files here
        }

    val gifPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            // Manage gif picked files here
        }

    Column(modifier = modifier) {
        ChatGallery(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            sheetState = sheetState
        )

        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AttachItem(
                iconId = R.drawable.ic_attach_from_gallery,
                itemName = stringResource(id = R.string.chat_attach_panel_gallery),
                onItemClick = { galleryPicker.launch(PickVisualMediaRequest()) },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_GALLERY)
            )

            AttachItem(
                iconId = R.drawable.ic_attach_from_file,
                itemName = pluralStringResource(id = R.plurals.general_num_files, count = 1),
                onItemClick = { },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_FILE)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_gif,
                itemName = stringResource(id = R.string.chat_room_toolbar_gif_option),
                onItemClick = { openGifPicker(context, gifPickerLauncher) },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_GIF)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_scan,
                itemName = stringResource(id = R.string.chat_room_toolbar_scan_option),
                onItemClick = { },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_SCAN)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AttachItem(
                iconId = R.drawable.ic_attach_from_location,
                itemName = stringResource(id = R.string.chat_room_toolbar_location_option),
                onItemClick = { },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_LOCATION)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_contact,
                itemName = stringResource(id = R.string.attachment_upload_panel_contact),
                onItemClick = { },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_CONTACT)
            )
            AttachItemPlaceHolder()
            AttachItemPlaceHolder()
        }
    }
}

private fun openGifPicker(
    context: Context,
    pickGifLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, GiphyPickerActivity::class.java).also {
        pickGifLauncher.launch(it)
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
) = LazyRow(
    modifier = modifier.testTag(TEST_TAG_GALLERY_LIST),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
    item("camera_button") {
        ChatCameraButton(modifier = Modifier.size(88.dp), sheetState = sheetState)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChatToolbarBottomSheetPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatToolbarBottomSheet(
            sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)
        )
    }
}

internal const val TEST_TAG_GALLERY_LIST = "chat_gallery_list"
internal const val TEST_TAG_ATTACH_FROM_GALLERY = "chat_view:attach_panel:attach_from_gallery"
internal const val TEST_TAG_ATTACH_FROM_FILE = "chat_view:attach_panel:attach_from_file"
internal const val TEST_TAG_ATTACH_FROM_GIF = "chat_view:attach_panel:attach_from_gif"
internal const val TEST_TAG_ATTACH_FROM_SCAN = "chat_view:attach_panel:attach_from_scan"
internal const val TEST_TAG_ATTACH_FROM_LOCATION = "chat_view:attach_panel:attach_from_location"
internal const val TEST_TAG_ATTACH_FROM_CONTACT = "chat_view:attach_panel:attach_from_contact"


