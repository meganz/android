package mega.privacy.android.app.presentation.documentscanner

import android.content.Intent
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
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import java.io.File
import java.nio.file.Files
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
                                onUploadScansStarted = { uploadScans() }
                            )
                        },
                    )
                }
            }
        }
    }

    /**
     * Starts an [Intent] to [FileExplorerActivity] in order to upload the scanned document/s
     */
    private fun uploadScans() {
        val uiState = viewModel.uiState.value
        val scanDestination = uiState.scanDestination
        val scanFileType = uiState.scanFileType
        val uriToUpload = when (scanFileType) {
            ScanFileType.Pdf -> uiState.pdfUri
            ScanFileType.Jpg -> uiState.soloImageUri
        }

        uriToUpload?.let { nonNullUri ->
            val uriPath = nonNullUri.path
            if (uriPath != null) {
                val uriContentType = Files.probeContentType(File(uriPath).toPath())
                val intent = Intent(this, FileExplorerActivity::class.java).apply {
                    putExtra(
                        FileExplorerActivity.EXTRA_DOCUMENT_SCAN_FILENAME,
                        uiState.actualFilename,
                    )
                    putExtra(Intent.EXTRA_STREAM, nonNullUri)
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
                    type = uriContentType
                }
                startActivity(intent)
                finish()
            } else {
                Timber.e("The $scanFileType URI Path does not exist")
            }
        } ?: Timber.e("The $scanFileType URI does not exist")
    }

    companion object {
        const val EXTRA_CLOUD_DRIVE_PARENT_HANDLE = "EXTRA_CLOUD_DRIVE_PARENT_HANDLE"
        const val EXTRA_SCAN_PDF_URI = "EXTRA_SCAN_PDF_URI"
        const val EXTRA_SCAN_SOLO_IMAGE_URI = "EXTRA_SCAN_SOLO_IMAGE_URI"
    }
}