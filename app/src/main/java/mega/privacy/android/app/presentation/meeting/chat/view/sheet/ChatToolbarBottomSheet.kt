package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GiphyPickerActivity
import mega.privacy.android.core.ui.controls.chat.attachpanel.AttachItem
import mega.privacy.android.core.ui.controls.chat.attachpanel.AttachItemPlaceHolder
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme
import nz.mega.documentscanner.DocumentScannerActivity

/**
 * Chat toolbar bottom sheet
 *
 * @param modifier
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatToolbarBottomSheet(
    onAttachFileClicked: () -> Unit,
    onAttachContactClicked: () -> Unit,
    onTakePicture: () -> Unit,
    onPickLocation: () -> Unit,
    modifier: Modifier = Modifier,
    onCameraPermissionDenied: () -> Unit = {},
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    onAttachFiles: (List<Uri>) -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val galleryPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) {
            if (it.isNotEmpty()) {
                onAttachFiles(it)
            }
            coroutineScope.launch { sheetState.hide() }
        }

    val gifPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            // Manage gif picked files here
            coroutineScope.launch { sheetState.hide() }
        }

    val scanDocumentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            // Manage scanned files here
            coroutineScope.launch { sheetState.hide() }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        ChatGallery(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            sheetState = sheetState,
            onTakePicture = onTakePicture,
            onCameraPermissionDenied = onCameraPermissionDenied,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
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
                onItemClick = onAttachFileClicked,
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
                onItemClick = { openDocumentScanner(context, scanDocumentLauncher) },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_SCAN)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AttachItem(
                iconId = R.drawable.ic_attach_from_location,
                itemName = stringResource(id = R.string.chat_room_toolbar_location_option),
                onItemClick = onPickLocation,
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_LOCATION)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_contact,
                itemName = stringResource(id = R.string.attachment_upload_panel_contact),
                onItemClick = {
                    coroutineScope.launch { sheetState.hide() }
                    onAttachContactClicked()
                },
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

private fun openDocumentScanner(
    context: Context,
    scanDocumentLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    DocumentScannerActivity.getIntent(context, arrayOf(context.getString(R.string.section_chat)))
        .also {
            scanDocumentLauncher.launch(it)
        }
}

@OptIn(ExperimentalMaterialApi::class)
@CombinedThemePreviews
@Composable
private fun ChatToolbarBottomSheetPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatToolbarBottomSheet(
            onAttachFileClicked = {},
            onAttachContactClicked = {},
            onPickLocation = {},
            onTakePicture = {},
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
internal const val TEST_TAG_LOADING_GALLERY = "chat_view:attach_panel:loading_gallery"


