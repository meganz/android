package mega.privacy.android.app.presentation.documentscanner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
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
    lateinit var getThemeMode: GetThemeMode

    /**
     * Handles the Passcode
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    private val viewModel by viewModels<SaveScannedDocumentsViewModel>()

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            SessionContainer {
                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = {
                            SaveScannedDocumentsScreen(
                                viewModel = viewModel,
                                onUploadScansStarted = { uriToUpload ->
                                    if (viewModel.uiState.value.originatedFromChat) {
                                        redirectBackToChat(uriToUpload)
                                    } else {
                                        proceedToFileExplorer(uriToUpload)
                                    }
                                }
                            )
                        },
                    )
                }
            }
        }
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
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    /**
     * When the Activity is accessed from anywhere other than Cloud Drive and the Document Scanning
     * finishes, this creates an [Intent] to [FileExplorerActivity] with the [Uri] containing the
     * scans to be uploaded. This Activity gets finished afterwards
     *
     * @param uriToUpload The [Uri] containing the scans to be uploaded
     */
    private fun proceedToFileExplorer(uriToUpload: Uri) {
        val uiState = viewModel.uiState.value
        val scanDestination = viewModel.uiState.value.scanDestination

        val intent = Intent(this, FileExplorerActivity::class.java).apply {
            putExtra(Intent.EXTRA_STREAM, uriToUpload)
            when (scanDestination) {
                ScanDestination.CloudDrive -> {
                    action = FileExplorerActivity.ACTION_SAVE_TO_CLOUD
                    putExtra(
                        FileExplorerActivity.EXTRA_PARENT_HANDLE,
                        uiState.cloudDriveParentHandle,
                    )
                }

                ScanDestination.Chat -> {
                    action = FileExplorerActivity.ACTION_UPLOAD_TO_CHAT
                }
            }
            type = contentResolver.getType(uriToUpload)
        }

        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_ORIGINATED_FROM_CHAT = "EXTRA_ORIGINATED_FROM_CHAT"
        const val EXTRA_CLOUD_DRIVE_PARENT_HANDLE = "EXTRA_CLOUD_DRIVE_PARENT_HANDLE"
        const val EXTRA_SCAN_PDF_URI = "EXTRA_SCAN_PDF_URI"
        const val EXTRA_SCAN_SOLO_IMAGE_URI = "EXTRA_SCAN_SOLO_IMAGE_URI"
    }
}