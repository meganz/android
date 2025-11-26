package mega.privacy.android.core.nodecomponents.upload

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.MegaActivityResultContract
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import timber.log.Timber

/**
 * Data class to hold upload handler functions.
 */
data class UploadHandler(
    val onUploadFilesClicked: () -> Unit,
    val onUploadFolderClicked: () -> Unit,
)

/**
 * Composable function that provides upload functionality for both files and folders.
 *
 * This composable encapsulates all the launcher logic for uploading files and folders,
 * including permission handling, file/folder selection, and internal folder picker integration.
 *
 * @param parentId The parent node ID where files/folders will be uploaded
 * @param onFilesSelected Callback invoked when files are selected, receives list of URIs
 * @param megaNavigator Optional navigator for opening internal folder picker, defaults to rememberMegaNavigator()
 * @param megaResultContract Optional contract provider, defaults to rememberMegaResultContract()
 * @return [UploadHandler] containing handler functions to trigger upload files and folders
 */
@Composable
fun rememberUploadHandler(
    parentId: NodeId,
    onFilesSelected: (List<Uri>) -> Unit,
    megaNavigator: MegaNavigator = rememberMegaNavigator(),
    megaResultContract: MegaActivityResultContract = rememberMegaResultContract(),
): UploadHandler {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current
    var isUploadFolder by rememberSaveable { mutableStateOf(false) }

    val internalFolderPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = it.data
            val resultCode = it.resultCode
            if (intent != null && resultCode == Activity.RESULT_OK) {
                val result = intent.getStringExtra(ExtraConstant.EXTRA_ACTION_RESULT)
                if (!result.isNullOrEmpty()) {
                    coroutineScope.launch {
                        snackbarHostState?.showAutoDurationSnackbar(result)
                    }
                }
            }
        }

    val openMultipleDocumentLauncher =
        rememberLauncherForActivityResult(megaResultContract.openMultipleDocumentsPersistable) {
            if (it.isNotEmpty()) {
                onFilesSelected(it)
            }
        }

    val uploadFolderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = it.data
            val uri = intent?.data
            if (it.resultCode == Activity.RESULT_OK && uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                megaNavigator.openInternalFolderPicker(
                    context = context,
                    isUpload = true,
                    parentId = parentId,
                    initialUri = uri,
                    launcher = internalFolderPickerLauncher
                )
            }
        }

    val manualUploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        runCatching {
            if (isUploadFolder) {
                uploadFolderLauncher.launch(
                    Intent.createChooser(
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
                        null
                    )
                )
            } else {
                openMultipleDocumentLauncher.launch(arrayOf("*/*"))
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    return remember(
        manualUploadLauncher,
        openMultipleDocumentLauncher,
        uploadFolderLauncher
    ) {
        UploadHandler(
            onUploadFilesClicked = {
                isUploadFolder = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    manualUploadLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching {
                        openMultipleDocumentLauncher.launch(arrayOf("*/*"))
                    }.onFailure {
                        Timber.e(it, "Activity not found")
                    }
                }
            },
            onUploadFolderClicked = {
                isUploadFolder = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    manualUploadLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching {
                        uploadFolderLauncher.launch(
                            Intent.createChooser(
                                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
                                null
                            )
                        )
                    }.onFailure {
                        Timber.e(it, "Activity not found")
                    }
                }
            }
        )
    }
}

