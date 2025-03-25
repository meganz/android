package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GiphyPickerActivity
import mega.privacy.android.app.activities.GiphyPickerActivity.Companion.GIF_DATA
import mega.privacy.android.app.camera.CameraArg
import mega.privacy.android.app.camera.InAppCameraLauncher
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsActivity
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openAttachContactActivity
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.CellButton
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.CellButtonPlaceHolder
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
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
import mega.privacy.mobile.analytics.event.DocumentScanInitiatedEvent
import timber.log.Timber

/**
 * Chat toolbar bottom sheet
 *
 * @param modifier
 */
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
    onAttachScan: () -> Unit = {},
    onDocumentScannerInitializationFailed: () -> Unit = {},
    onDocumentScannerFailedToOpen: () -> Unit = {},
    onGmsDocumentScannerConsumed: () -> Unit = {},
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

    val saveScannedDocumentsActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.data?.let { uri ->
            onAttachFiles(listOf(uri))
        }
        hideSheet()
    }

    val scanDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.let { data ->
                with(data) {
                    val imageUris = pages?.mapNotNull { page ->
                        page.imageUri
                    } ?: emptyList()

                    // The PDF URI must exist before moving to the Scan Confirmation page
                    pdf?.uri?.let { pdfUri ->
                        val intent = Intent(
                            context, SaveScannedDocumentsActivity::class.java
                        ).apply {
                            putExtra(
                                SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT,
                                true,
                            )
                            putExtra(SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI, pdfUri)
                            putExtra(
                                SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI,
                                if (imageUris.size == 1) imageUris[0] else null,
                            )
                        }
                        saveScannedDocumentsActivityLauncher.launch(intent)
                    } ?: run {
                        Timber.e("The PDF file could not be retrieved from Chat after scanning")
                    }
                }
            }
        } else {
            Timber.e("The ML Kit Document Scan result could not be retrieved from Chat")
        }
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

    uiState.documentScanningError?.let {
        onDocumentScannerInitializationFailed()
    }

    EventEffect(
        event = uiState.gmsDocumentScanner,
        onConsumed = onGmsDocumentScannerConsumed,
        action = { gmsDocumentScanner ->
            openDocumentScanner(
                context = context,
                documentScanner = gmsDocumentScanner,
                documentScannerLauncher = scanDocumentLauncher,
                onDocumentScannerFailedToOpen = onDocumentScannerFailedToOpen,
            )
        },
    )

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
            CellButton(
                iconId = R.drawable.ic_attach_from_gallery,
                itemName = stringResource(id = R.string.chat_attach_panel_gallery),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationGalleryMenuItemEvent)
                    galleryPicker.launch(PickVisualMediaRequest())
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_GALLERY)
            )
            CellButton(
                iconId = R.drawable.ic_attach_from_file,
                itemName = pluralStringResource(id = R.plurals.general_num_files, count = 1),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationFileMenuItemEvent)
                    navigateToFileModal()
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_FILE)
            )
            CellButton(
                iconId = R.drawable.ic_attach_from_gif,
                itemName = stringResource(id = R.string.chat_room_toolbar_gif_option),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationGIFMenuItemEvent)
                    openGifPicker(context, gifPickerLauncher)
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_GIF)
            )
            CellButton(
                iconId = R.drawable.ic_attach_from_scan,
                itemName = stringResource(id = R.string.chat_room_toolbar_scan_option),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationScanMenuItemEvent)
                    onAttachScan()
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
            CellButton(
                iconId = R.drawable.ic_attach_from_location,
                itemName = stringResource(id = R.string.chat_room_toolbar_location_option),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationLocationMenuItemEvent)
                    onPickLocation()
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_LOCATION)
            )
            CellButton(
                iconId = R.drawable.ic_attach_from_contact,
                itemName = stringResource(id = R.string.attachment_upload_panel_contact),
                onItemClick = {
                    Analytics.tracker.trackEvent(ChatConversationContactMenuItemEvent)
                    onAttachContactClicked()
                },
                modifier = Modifier.testTag(TEST_TAG_ATTACH_FROM_CONTACT)
            )
            CellButtonPlaceHolder()
            CellButtonPlaceHolder()
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
    documentScanner: GmsDocumentScanner,
    documentScannerLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    onDocumentScannerFailedToOpen: () -> Unit,
) {
    context.findActivity()?.let { activity ->
        documentScanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                Analytics.tracker.trackEvent(DocumentScanInitiatedEvent)
                documentScannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }.addOnFailureListener { exception ->
                Timber.e(
                    exception,
                    "An error occurred when attempting to run the ML Kit Document Scanner from Chat",
                )
                onDocumentScannerFailedToOpen()
            }
    } ?: run {
        Timber.e("Unable to run the ML Kit Document Scanner in Chat as no Activity can be found")
    }
}

@CombinedThemePreviews
@Composable
private fun ChatToolbarBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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


