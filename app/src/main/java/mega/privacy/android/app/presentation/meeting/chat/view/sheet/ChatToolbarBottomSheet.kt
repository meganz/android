package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GiphyPickerActivity
import mega.privacy.android.app.activities.GiphyPickerActivity.Companion.GIF_DATA
import mega.privacy.android.app.camera.CameraArg
import mega.privacy.android.app.camera.InAppCameraLauncher
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openAttachContactActivity
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.AttachItem
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.AttachItemPlaceHolder
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.ChatConversationContactMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationFileMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationGIFMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationGalleryMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationLocationMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationScanMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatConversationTakePictureMenuItemEvent
import mega.privacy.mobile.analytics.event.ChatImageAttachmentItemSelected
import mega.privacy.mobile.analytics.event.ChatImageAttachmentItemSelectedEvent
import nz.mega.documentscanner.DocumentScannerActivity

/**
 * Chat toolbar bottom sheet
 *
 * @param modifier
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatToolbarBottomSheet(
    closeModal: () -> Unit,
    onPickLocation: () -> Unit,
    onSendGiphyMessage: (GifData?) -> Unit,
    hideSheet: () -> Unit,
    isVisible: Boolean,
    uiState: ChatUiState,
    scaffoldState: ScaffoldState,
    onAttachContacts: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    navigateToFileModal: () -> Unit,
    onAttachFiles: (List<Uri>) -> Unit = {},
    onCameraPermissionDenied: () -> Unit = {},
) {
    val context = LocalContext.current

    val galleryPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) {
            if (it.isNotEmpty()) {
                onAttachFiles(it)
            }
            hideSheet()
        }

    val gifPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            onSendGiphyMessage(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.data?.getParcelableExtra(GIF_DATA, GifData::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    it.data?.getParcelableExtra(GIF_DATA)
                }
            )
            hideSheet()
        }

    val scanDocumentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            it.data?.data?.let { uri ->
                onAttachFiles(listOf(uri))
            }
            hideSheet()
        }

    val attachContactLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result.data?.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                ?.let { contactList ->
                    onAttachContacts(contactList.toList())
                }
            hideSheet()
        }

    val coroutineScope = rememberCoroutineScope()
    val onAttachContactClicked: () -> Unit = {
        if (uiState.hasAnyContact) {
            openAttachContactActivity(context, attachContactLauncher)
        } else {
            coroutineScope.launch {
                scaffoldState.snackbarHostState.showAutoDurationSnackbar(context.getString(R.string.no_contacts_invite))
            }
        }
    }

    val takePictureLauncher =
        rememberLauncherForActivityResult(
            contract = InAppCameraLauncher()
        ) { uri ->
            uri?.let {
                onAttachFiles(listOf(it))
            }
            closeModal()
        }

    val capturePhotoOrVideoPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[Manifest.permission.CAMERA] == true) {
            takePictureLauncher.launch(CameraArg(uiState.title.orEmpty()))
        } else {
            onCameraPermissionDenied()
        }
    }

    val onTakePicture: () -> Unit = {
        capturePhotoOrVideoPermissionsLauncher.launch(
            arrayOf(
                PermissionUtils.getCameraPermission(),
                PermissionUtils.getRecordAudioPermission()
            )
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ChatGallery(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            onTakePicture = {
                Analytics.tracker.trackEvent(ChatConversationTakePictureMenuItemEvent)
                onTakePicture()
            },
            onFileGalleryItemClicked = {
                Analytics.tracker.trackEvent(
                    ChatImageAttachmentItemSelectedEvent(
                        ChatImageAttachmentItemSelected.SelectionType.SingleMode,
                        1
                    )
                )
                it.fileUri?.toUri()?.let { uri ->
                    onAttachFiles(listOf(uri))
                    hideSheet()
                }
            },
            onCameraPermissionDenied = onCameraPermissionDenied,
            hideSheet = hideSheet,
            isVisible = isVisible,
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
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationGalleryMenuItemEvent)
                    galleryPicker.launch(PickVisualMediaRequest())
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_GALLERY)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_file,
                itemName = pluralStringResource(id = R.plurals.general_num_files, count = 1),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationFileMenuItemEvent)
                    navigateToFileModal()
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_FILE)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_gif,
                itemName = stringResource(id = R.string.chat_room_toolbar_gif_option),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationGIFMenuItemEvent)
                    openGifPicker(context, gifPickerLauncher)
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_GIF)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_scan,
                itemName = stringResource(id = R.string.chat_room_toolbar_scan_option),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationScanMenuItemEvent)
                    openDocumentScanner(context, scanDocumentLauncher)
                },
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
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationLocationMenuItemEvent)
                    onPickLocation()
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_LOCATION)
            )
            AttachItem(
                iconId = R.drawable.ic_attach_from_contact,
                itemName = stringResource(id = R.string.attachment_upload_panel_contact),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationContactMenuItemEvent)
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChatToolbarBottomSheet(
            onAttachContacts = {},
            uiState = ChatUiState(),
            scaffoldState = rememberScaffoldState(),
            onPickLocation = {},
            onSendGiphyMessage = {},
            closeModal = {},
            hideSheet = {},
            navigateToFileModal = {},
            isVisible = true,
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
internal const val TEST_TAG_ATTACH_GALLERY_ITEM = "chat_view:attach_panel:gallery_item"


