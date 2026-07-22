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
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_CLOUD_DRIVE_PARENT_HANDLE
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_ORIGINATED_FROM_CHAT
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_SCAN_PDF_URI
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.EXTRA_SCAN_SOLO_IMAGE_URI
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsViewModel.Companion.INITIAL_FILENAME_FORMAT
import mega.privacy.android.app.presentation.documentscanner.navigation.SaveScannedDocumentsDestination
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.SaveScannedDocumentsNavKey
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

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = themeMode.isDarkMode().not()
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = useDarkIcons
            )

            val initialNavKey = remember { buildInitialNavKey() }
            LegacyActivityScaffold(
                container = { content ->
                    MegaAppContainer(
                        themeMode = themeMode,
                        finishOnSessionRefresh = false,
                        content = content
                    )
                },
                initialKey = initialNavKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                excludeOwnDestination = SaveScannedDocumentsDestination::class,
                onEmptyBackStack = { if (!isFinishing) finish() },
            ) { navigationHandler, _ ->
                entry<SaveScannedDocumentsNavKey> { key ->
                    val resources = LocalResources.current
                    val viewModel =
                        hiltViewModel<SaveScannedDocumentsViewModel, SaveScannedDocumentsViewModel.Factory> { factory ->
                            factory.create(
                                SaveScannedDocumentsViewModel.Args(
                                    originatedFromChat = key.originatedFromChat,
                                    cloudDriveParentHandle = key.cloudDriveParentHandle
                                        ?.takeIf { it != -1L },
                                    pdfUri = key.scanPdfUri.takeIf { it.isNotEmpty() }
                                        ?.toUri(),
                                    soloImageUri = key.scanSoloImageUri?.toUri(),
                                    fileFormat = resources.getString(
                                        SharedR.string.document_scanning_default_file_name
                                    ),
                                )
                            )
                        }
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    SaveScannedDocumentsView(
                        uiState = uiState,
                        onFilenameChanged = viewModel::onFilenameChanged,
                        onFilenameConfirmed = viewModel::onFilenameConfirmed,
                        onSaveButtonClicked = viewModel::onSaveButtonClicked,
                        onScanDestinationSelected = viewModel::onScanDestinationSelected,
                        onScanFileTypeSelected = viewModel::onScanFileTypeSelected,
                        onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
                        onUploadScansEventConsumed = viewModel::onUploadScansEventConsumed,
                        onBackToChat = ::redirectBackToChat,
                        onNavigate = { navKeys ->
                            navigationHandler.navigate(navKeys)
                            navigationHandler.remove(key)
                        },
                    )
                }
            }
        }
    }

    private fun buildInitialNavKey(): SaveScannedDocumentsNavKey =
        SaveScannedDocumentsNavKey(
            originatedFromChat = intent.getBooleanExtra(EXTRA_ORIGINATED_FROM_CHAT, false),
            cloudDriveParentHandle = intent.getLongExtra(EXTRA_CLOUD_DRIVE_PARENT_HANDLE, -1L)
                .takeIf { it != -1L },
            scanPdfUri = intent.getUriExtra(EXTRA_SCAN_PDF_URI)?.toString().orEmpty(),
            scanSoloImageUri = intent.getUriExtra(EXTRA_SCAN_SOLO_IMAGE_URI)?.toString(),
        )

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
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setResult(RESULT_OK, intent)
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
