package mega.privacy.android.app.main.managerSections

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.camera.CameraArg
import mega.privacy.android.app.camera.InAppCameraLauncher
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.main.CameraPermissionManager
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.NavigationDrawerManager
import mega.privacy.android.app.main.ParentNodeManager
import mega.privacy.android.app.presentation.bottomsheet.ShowNewFolderDialogActionListener
import mega.privacy.android.app.presentation.bottomsheet.ShowNewTextFileDialogActionListener
import mega.privacy.android.app.presentation.bottomsheet.TakePictureAndUploadActionListener
import mega.privacy.android.app.presentation.bottomsheet.UploadFilesActionListener
import mega.privacy.android.app.presentation.bottomsheet.UploadFolderActionListener
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsActivity
import mega.privacy.android.app.presentation.extensions.uploadFolderManually
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_FOLDER_DIALOG_SHOWN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.IS_NEW_TEXT_FILE_SHOWN
import mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_FOLDER_DIALOG_TEXT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.NEW_TEXT_FILE_TEXT
import mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewFolderDialogState
import mega.privacy.android.app.utils.MegaNodeDialogUtil.checkNewTextFileDialogState
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewFolderDialog
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showNewTxtFileDialog
import mega.privacy.android.app.utils.Util.checkTakePicture
import mega.privacy.android.app.utils.permission.PermissionUtilWrapper
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.mobile.analytics.event.DocumentScanInitiatedEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * Upload bottom sheet dialog action handler for [ManagerActivity]
 */
@ActivityScoped
internal class ManagerUploadBottomSheetDialogActionHandler @Inject constructor(
    activity: Activity,
    private val permissionUtilWrapper: PermissionUtilWrapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : DefaultLifecycleObserver,
    UploadFilesActionListener,
    UploadFolderActionListener,
    TakePictureAndUploadActionListener,
    ShowNewFolderDialogActionListener,
    ShowNewTextFileDialogActionListener {

    private val managerActivity = activity as ManagerActivity
    private val parentNodeManager = activity as ParentNodeManager
    private val cameraPermissionManager = activity as CameraPermissionManager
    private val navigationDrawerManager = activity as NavigationDrawerManager
    private val actionNodeCallback = activity as ActionNodeCallback

    private var newTextFileDialog: AlertDialog? = null
    private var newFolderDialog: AlertDialog? = null

    init {
        managerActivity.lifecycle.addObserver(this)
    }

    fun onSaveInstanceState(outState: Bundle) {
        newFolderDialog.checkNewFolderDialogState(outState)
        checkNewTextFileDialogState(newTextFileDialog, outState)
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.run {
            if (getBoolean(IS_NEW_FOLDER_DIALOG_SHOWN, false)) {
                showNewFolderDialog(getString(NEW_FOLDER_DIALOG_TEXT))
            }
            if (getBoolean(IS_NEW_TEXT_FILE_SHOWN, false)) {
                showNewTextFileDialog(getString(NEW_TEXT_FILE_TEXT))
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        newFolderDialog?.dismiss()
        newTextFileDialog?.dismiss()
    }

    private val openMultipleDocumentLauncher =
        managerActivity.registerForActivityResult(OpenMultipleDocumentsPersistable()) {
            if (it.isNotEmpty()) {
                it.forEach { uri ->
                    runCatching {
                        managerActivity.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }.onFailure {
                        Timber.e(it, "Failed to take persistable URI permission")
                    }
                }
                managerActivity.handleFileUris(it)
            }
        }

    private class OpenMultipleDocumentsPersistable :
        ActivityResultContracts.OpenMultipleDocuments() {
        override fun createIntent(context: Context, input: Array<String>): Intent {
            return super.createIntent(context, input).also {
                it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                it.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
        }
    }

    /**
     * When manually uploading Files and the device is running Android 13 and above, this Launcher
     * is called to request the Notification Permission (if possible) and upload Files regardless
     * if the Notification Permission is granted or not
     */
    private val manualUploadFilesLauncher: ActivityResultLauncher<String> =
        managerActivity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            runCatching {
                openMultipleDocumentLauncher.launch(arrayOf("*/*"))
            }.onFailure {
                Timber.e(it)
            }
        }

    /**
     * When manually uploading a Folder and the device is running Android 13 and above, this Launcher
     * is called to request the Notification Permission whenever possible, and upload the Folder
     * regardless if the Notification Permission is granted or not
     */
    private val manualUploadFolderLauncher: ActivityResultLauncher<String> =
        managerActivity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { activity.uploadFolderManually() }

    /**
     * Launcher to scan documents using the ML Kit Document Scanner. After scanning, a different
     * screen is opened to configure where to save the scanned documents.
     */
    private val scanDocumentLauncher = managerActivity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.let { data ->
                with(data) {
                    val imageUris = pages?.mapNotNull { page ->
                        page.imageUri
                    } ?: emptyList()

                    // The PDF URI must exist before moving to the Scan Confirmation page
                    pdf?.uri?.let { pdfUri ->
                        val intent = Intent(
                            managerActivity, SaveScannedDocumentsActivity::class.java
                        ).apply {
                            putExtra(SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT, false)
                            putExtra(
                                SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE,
                                parentNodeManager.currentParentHandle,
                            )
                            putExtra(SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI, pdfUri)
                            putExtra(
                                SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI,
                                if (imageUris.size == 1) imageUris[0] else null,
                            )
                        }
                        managerActivity.startActivity(intent)
                    } ?: run {
                        Timber.e("The PDF file could not be retrieved from Cloud Drive after scanning")
                    }
                }
            }
        } else {
            Timber.e("The ML Kit Document Scan result could not be retrieved from Cloud Drive")
        }
    }

    override fun uploadFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manualUploadFilesLauncher.launch(POST_NOTIFICATIONS)
        } else {
            runCatching {
                openMultipleDocumentLauncher.launch(arrayOf("*/*"))
            }.onFailure {
                Timber.e(it, "Activity not found")
            }
        }
    }

    override fun uploadFolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manualUploadFolderLauncher.launch(POST_NOTIFICATIONS)
        } else {
            managerActivity.uploadFolderManually()
        }
    }

    private val inAppCameraLauncher = managerActivity.registerForActivityResult(
        InAppCameraLauncher()
    ) {
        it?.let { uri ->
            managerActivity.handleFileUris(listOf(uri))
        }
    }

    private val cameraPermissionLauncher: ActivityResultLauncher<Array<String>> =
        managerActivity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                inAppCameraLauncher.launch(
                    CameraArg("", activity.getString(R.string.context_upload))
                )
            } else {
                managerActivity.showSnackbar(
                    type = Constants.NOT_CALL_PERMISSIONS_SNACKBAR_TYPE,
                    content = managerActivity.getString(R.string.chat_attach_pick_from_camera_deny_permission),
                    chatId = -1L
                )
            }
        }

    override fun takePictureAndUpload() {
        managerActivity.lifecycleScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.CameraActivityInCloudDrive)
            }.onSuccess { enabled ->
                if (enabled) {
                    cameraPermissionLauncher.launch(
                        arrayOf(
                            PermissionUtils.getCameraPermission(),
                            PermissionUtils.getRecordAudioPermission()
                        )
                    )
                } else {
                    startLegacyCameraIntent()
                }
            }
        }
    }

    private fun startLegacyCameraIntent() {
        if (!permissionUtilWrapper.hasPermissions(Manifest.permission.CAMERA)) {
            cameraPermissionManager.setTypesCameraPermission(Constants.TAKE_PICTURE_OPTION)
            ActivityCompat.requestPermissions(
                managerActivity,
                arrayOf(Manifest.permission.CAMERA),
                Constants.REQUEST_CAMERA
            )
            return
        }
        if (!permissionUtilWrapper.hasPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(
                managerActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                Constants.REQUEST_WRITE_STORAGE
            )
            return
        }
        checkTakePicture(managerActivity, Constants.TAKE_PHOTO_CODE)
    }

    /**
     * Begin scanning Documents using the ML Kit Document Scanner
     *
     * @param documentScanner the ML Kit Document Scanner
     */
    fun scanDocument(documentScanner: GmsDocumentScanner) {
        documentScanner.apply {
            getStartScanIntent(managerActivity)
                .addOnSuccessListener {
                    Analytics.tracker.trackEvent(DocumentScanInitiatedEvent)
                    scanDocumentLauncher.launch(IntentSenderRequest.Builder(it).build())
                }
                .addOnFailureListener { exception ->
                    Timber.e(
                        exception,
                        "An error occurred when attempting to run the ML Kit Document Scanner from Cloud Drive",
                    )
                    managerActivity.onDocumentScannerFailedToOpen()
                }
        }
    }

    override fun showNewFolderDialog(typedText: String?) {
        parentNodeManager.getCurrentParentNode(
            parentNodeManager.currentParentHandle,
            Constants.INVALID_VALUE
        )?.let { parent ->
            newFolderDialog =
                showNewFolderDialog(managerActivity, actionNodeCallback, parent, typedText)
        }

    }

    override fun showNewTextFileDialog(typedName: String?) {
        parentNodeManager.getCurrentParentNode(
            parentNodeManager.currentParentHandle,
            Constants.INVALID_VALUE
        )?.let { parent ->
            newTextFileDialog = showNewTxtFileDialog(
                managerActivity, parent, typedName,
                navigationDrawerManager.drawerItem === DrawerItem.HOMEPAGE
            )
        }
    }
}