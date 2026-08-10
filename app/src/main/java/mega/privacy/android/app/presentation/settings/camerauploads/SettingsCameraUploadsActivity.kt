package mega.privacy.android.app.presentation.settings.camerauploads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.settings.SettingsCameraUploadsFeatureDestination
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import javax.inject.Inject

/**
 * An Activity that shows the Settings Camera Uploads screen
 */
@AndroidEntryPoint
class SettingsCameraUploadsActivity : ComponentActivity() {

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
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        val isShowHowToUploadPrompt =
            intent.getBooleanExtra(INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT, false)
        val isShowDisableCameraUploads =
            intent.getBooleanExtra(INTENT_EXTRA_KEY_SHOW_DISABLE_CU, false)

        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            LegacyActivityScaffold(
                container = { content ->
                    MegaAppContainer(
                        themeMode = themeMode,
                        finishOnSessionRefresh = false,
                        content = content
                    )
                },
                initialKey = SettingsCameraUploadsNavKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                excludeOwnDestination = SettingsCameraUploadsFeatureDestination::class,
                onEmptyBackStack = { if (!isFinishing) finish() },
            ) { navigationHandler, _ ->
                entry<SettingsCameraUploadsNavKey> {
                    val viewModel = hiltViewModel<SettingsCameraUploadsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    SettingsCameraUploadsView(
                        uiState = uiState,
                        isShowHowToUploadPrompt = isShowHowToUploadPrompt,
                        isShowDisableCameraUploads = isShowDisableCameraUploads,
                        onBusinessAccountPromptDismissed = viewModel::onBusinessAccountPromptDismissed,
                        onCameraUploadsProcessStarted = viewModel::onCameraUploadsProcessStarted,
                        onCameraUploadsStateChanged = viewModel::onCameraUploadsStateChanged,
                        onChargingDuringVideoCompressionStateChanged = viewModel::onChargingDuringVideoCompressionStateChanged,
                        onChargingWhenUploadingContentStateChanged = viewModel::onChargingWhenUploadingContentStateChanged,
                        onHowToUploadPromptOptionSelected = viewModel::onHowToUploadPromptOptionSelected,
                        onIncludeLocationTagsStateChanged = viewModel::onIncludeLocationTagsStateChanged,
                        onKeepFileNamesStateChanged = viewModel::onKeepFileNamesStateChanged,
                        onLocalPrimaryFolderSelected = viewModel::onLocalPrimaryFolderSelected,
                        onLocalSecondaryFolderSelected = viewModel::onLocalSecondaryFolderSelected,
                        onLocationPermissionGranted = viewModel::onLocationPermissionGranted,
                        onMediaPermissionsGranted = viewModel::onMediaPermissionsGranted,
                        onMediaUploadsStateChanged = viewModel::onMediaUploadsStateChanged,
                        onNewVideoCompressionSizeLimitProvided = viewModel::onNewVideoCompressionSizeLimitProvided,
                        onPrimaryFolderNodeSelected = viewModel::onPrimaryFolderNodeSelected,
                        onRegularBusinessAccountSubUserPromptAcknowledged = viewModel::onRegularBusinessAccountSubUserPromptAcknowledged,
                        onRelatedNewLocalFolderWarningDismissed = viewModel::onRelatedNewLocalFolderWarningDismissed,
                        onRequestLocationPermissionStateChanged = viewModel::onRequestLocationPermissionStateChanged,
                        onRequestMediaPermissionsStateChanged = viewModel::onRequestMediaPermissionsStateChanged,
                        onSecondaryFolderNodeSelected = viewModel::onSecondaryFolderNodeSelected,
                        onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
                        onUploadOptionUiItemSelected = viewModel::onUploadOptionUiItemSelected,
                        onVideoQualityUiItemSelected = viewModel::onVideoQualityUiItemSelected,
                        onDisableCameraUploads = viewModel::disableCameraUploads,
                        onNavigate = { destination ->
                            navigationHandler.navigate(destination)
                        },
                        monitorResult = navigationHandler::monitorResult,
                        clearResult = navigationHandler::clearResult,
                    )
                }
            }
        }
    }
}

const val INTENT_EXTRA_KEY_SHOW_DISABLE_CU = "SHOW_DISABLE_CU"
