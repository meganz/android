package mega.privacy.android.app.presentation.documentscanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_CLOUD_DRIVE_PARENT_HANDLE
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_ORIGINATED_FROM_CHAT
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_SCAN_PDF_URI
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_SCAN_SOLO_IMAGE_URI
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.INITIAL_FILENAME_FORMAT
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.resources.R as SharedR
import javax.inject.Inject

/**
 * An Activity that shows a screen where Users can configure some aspects of their scanned documents
 */
@AndroidEntryPoint
internal class SaveScannedDocumentsActivity : AppCompatActivity() {

    /**
     * Retrieves the Device Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Handles the Passcode
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = themeMode.isDarkMode().not()
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = useDarkIcons
            )

            MegaAppContainer(
                themeMode = themeMode,
                passcodeCryptObjectFactory = passcodeCryptObjectFactory
            ) {
                SaveScannedDocumentsScreen(
                    viewModel = hiltViewModel<SaveScannedDocumentsViewModel, SaveScannedDocumentsViewModel.Factory> { factory ->
                        factory.create(
                            SaveScannedDocumentsViewModel.Args(
                                originatedFromChat = intent.getBooleanExtra(
                                    EXTRA_ORIGINATED_FROM_CHAT,
                                    false
                                ),
                                cloudDriveParentHandle = intent.getLongExtra(
                                    EXTRA_CLOUD_DRIVE_PARENT_HANDLE,
                                    -1L
                                )
                                    .takeIf { it != -1L },
                                pdfUri = intent.getUriExtra(EXTRA_SCAN_PDF_URI),
                                soloImageUri = intent.getUriExtra(EXTRA_SCAN_SOLO_IMAGE_URI),
                                fileFormat = intent.getStringExtra(INITIAL_FILENAME_FORMAT)
                                    .orEmpty(),

                                )
                        )
                    },
                    onUploadToChat = { uri, scanFileType, originatedFromChat, canSelectScanFileType ->
                        if (originatedFromChat) {
                            redirectBackToChat(uri)
                        } else {
                            proceedToFileExplorer(
                                uriToUpload = uri,
                                cloudDriveParentHandle = null,
                                scanFileType = scanFileType,
                                canSelectScanFileType = canSelectScanFileType,
                            )
                        }

                    },
                    onUploadToCloudDrive = { uri, scanFileType, cloudDriveParentHandle, canSelectScanFileType ->
                        proceedToFileExplorer(
                            uriToUpload = uri,
                            cloudDriveParentHandle = cloudDriveParentHandle,
                            scanFileType = scanFileType,
                            canSelectScanFileType = canSelectScanFileType,
                        )
                    },
                )
            }
        }
    }

    private fun Intent.getUriExtra(key: String): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key)
        }

    /**
     * When the Activity is accessed from Chat and the Document Scanning finishes, this creates an
     * [Intent] with the [Uri] containing the scans to be uploaded. This Activity finishes and the
     * result gets sent back to the caller
     *
     * @param uriToUpload The [Uri] containing the scans to be uploaded
     */
    private fun redirectBackToChat(uriToUpload: Uri) {
        val intent = Intent().apply {
            setDataAndType(uriToUpload, contentResolver.getType(uriToUpload))
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * When the Activity is accessed from anywhere other than Cloud Drive and the Document Scanning
     * finishes, this creates an [Intent] to [FileExplorerActivity] with the [Uri] containing the
     * scans to be uploaded. This Activity gets finished afterwards
     *
     * @param uriToUpload The [Uri] containing the scans to be uploaded
     */
    private fun proceedToFileExplorer(
        uriToUpload: Uri,
        cloudDriveParentHandle: Long?,
        scanFileType: ScanFileType,
        canSelectScanFileType: Boolean,
    ) {
        val intent = Intent(this, FileExplorerActivity::class.java).apply {
            putExtra(Intent.EXTRA_STREAM, uriToUpload)
            putExtra(FileExplorerActivity.EXTRA_SCAN_FILE_TYPE, scanFileType.ordinal)
            putExtra(FileExplorerActivity.EXTRA_HAS_MULTIPLE_SCANS, !canSelectScanFileType)
            if (cloudDriveParentHandle != null) {
                action = FileExplorerActivity.ACTION_UPLOAD_SCAN_TO_CLOUD
                putExtra(
                    FileExplorerActivity.EXTRA_PARENT_HANDLE,
                    cloudDriveParentHandle,
                )
            } else {
                action = FileExplorerActivity.ACTION_UPLOAD_SCAN_TO_CHAT
            }
            type = contentResolver.getType(uriToUpload)
        }

        startActivity(intent)
        finish()
    }

    companion object {

        fun getIntent(
            context: Context,
            fromChat: Boolean = false,
            parentHandle: Long? = null,
            pdfUri: Uri? = null,
            imageUris: List<Uri> = emptyList(),
        ): Intent {
            return Intent(
                context,
                SaveScannedDocumentsActivity::class.java,
            ).apply {
                putExtra(
                    EXTRA_ORIGINATED_FROM_CHAT,
                    fromChat,
                )
                parentHandle?.let {
                    putExtra(
                        EXTRA_CLOUD_DRIVE_PARENT_HANDLE,
                        it,
                    )
                }
                pdfUri?.let { putExtra(EXTRA_SCAN_PDF_URI, it) }
                putExtra(
                    EXTRA_SCAN_SOLO_IMAGE_URI,
                    if (imageUris.size == 1) imageUris[0] else null,
                )
                putExtra(
                    INITIAL_FILENAME_FORMAT,
                    context.getString(SharedR.string.document_scanning_default_file_name)
                )
            }
        }
    }
}