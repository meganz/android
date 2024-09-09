package mega.privacy.android.app.main.managerSections

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.app.Activity.RESULT_OK
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
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.main.CameraPermissionManager
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.FileExplorerActivity
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
import nz.mega.documentscanner.DocumentScannerActivity
import timber.log.Timber
import javax.inject.Inject

/**
 * Upload bottom sheet dialog action handler for [ManagerActivity]
 */
@ActivityScoped
internal class ManagerUploadBottomSheetDialogActionHandler @Inject constructor(
    activity: Activity,
    private val permissionUtilWrapper: PermissionUtilWrapper,
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
        managerActivity.registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (it.isNotEmpty()) {
                managerActivity.handleFileUris(it)
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
     * The launcher to scan documents using the for the old Document Scanner
     */
    private val legacyScanDocumentLauncher =
        managerActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                it.data?.let { intent ->
                    val savedDestination: String? =
                        intent.getStringExtra(DocumentScannerActivity.EXTRA_PICKED_SAVE_DESTINATION)
                    val fileIntent =
                        Intent(managerActivity, FileExplorerActivity::class.java).apply {
                            if (activity.getString(R.string.section_chat) == savedDestination) {
                                action = FileExplorerActivity.ACTION_UPLOAD_TO_CHAT
                            } else {
                                action = FileExplorerActivity.ACTION_SAVE_TO_CLOUD
                                putExtra(
                                    FileExplorerActivity.EXTRA_PARENT_HANDLE,
                                    parentNodeManager.currentParentHandle
                                )
                            }
                            putExtra(Intent.EXTRA_STREAM, intent.data)
                            type = intent.type
                        }
                    managerActivity.startActivity(fileIntent)
                }
            }
        }

    /**
     * The launcher to scan documents using the new ML Document Kit Scanner. After scanning, a
     * different screen is opened to configure where to save the scanned documents.
     */
    private val newScanDocumentLauncher = managerActivity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.let { data ->
                with(data) {
                    pdf?.uri?.let { pdfUri ->
                        Timber.d("The PDF URI is: $pdfUri")
                        // Do something with the PDF
                    }
                    pages?.forEach { page ->
                        page.imageUri.path?.let { imagePath ->
                            Timber.d("The Image Path is: $imagePath")
                            // Do something with the image
                        }
                    }
                    val intent =
                        Intent(managerActivity, SaveScannedDocumentsActivity::class.java)
                    managerActivity.startActivity(intent)
                }
            }
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

    override fun takePictureAndUpload() {
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
     * Begin scanning Documents using the old Document Scanner
     */
    fun scanDocumentUsingLegacyScanner() {
        val saveDestinations = arrayOf(
            managerActivity.getString(R.string.section_cloud_drive),
            managerActivity.getString(R.string.section_chat)
        )
        val intent = DocumentScannerActivity.getIntent(managerActivity, saveDestinations)
        legacyScanDocumentLauncher.launch(intent)
    }

    /**
     * Begin scanning Documents using the new ML Kit Document Scanner
     *
     * @param documentScanner the new ML Kit Document Scanner
     */
    fun scanDocumentUsingNewScanner(documentScanner: GmsDocumentScanner) {
        documentScanner.apply {
            getStartScanIntent(managerActivity)
                .addOnSuccessListener {
                    newScanDocumentLauncher.launch(
                        IntentSenderRequest.Builder(it).build()
                    )
                }
                .addOnFailureListener { exception ->
                    Timber.e("An error occurred when attempting to initialize the ML Kit Document Scanner: $exception")
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