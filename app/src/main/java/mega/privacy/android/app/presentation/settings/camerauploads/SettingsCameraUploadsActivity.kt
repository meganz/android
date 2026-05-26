package mega.privacy.android.app.presentation.settings.camerauploads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.snackbar.SnackbarLifetimeController
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.appstate.content.navigation.rememberPendingBackStack
import mega.privacy.android.app.appstate.content.transfer.AppTransferViewModel
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.presentation.settings.SettingsCameraUploadsFeatureDestination
import mega.privacy.android.app.presentation.settings.camerauploads.navigation.SettingsCameraUploadsNavigationHandler
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.bottomsheet.BottomSheetSceneStrategy
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.shared.rememberSharedViewModelStoreNavEntryDecorator
import mega.privacy.android.navigation.contract.transition.fadeTransition
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
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            MegaAppContainer(themeMode = themeMode) {
                val backStack = rememberPendingBackStack(SettingsCameraUploadsNavKey)
                val navigationHandler = remember {
                    SettingsCameraUploadsNavigationHandler(
                        backStack = backStack,
                        navigationResultManager = navigationResultManager,
                        onEmptyBackStack = { if (!isFinishing) finish() },
                    )
                }
                val appTransferViewModel = hiltViewModel<AppTransferViewModel>()
                val transferState by appTransferViewModel.state.collectAsStateWithLifecycle()
                val transferHandler = remember(appTransferViewModel) {
                    object : TransferHandler {
                        override fun setTransferEvent(event: TransferTriggerEvent) {
                            appTransferViewModel.setTransferEvent(event)
                        }
                    }
                }
                val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
                val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
                val snackbarHostState = remember { SnackbarHostState() }

                CompositionLocalProvider(
                    LocalSnackBarHostState provides snackbarHostState
                ) {
                    SnackbarLifetimeController()
                    NavDisplay(
                        backStack = backStack,
                        onBack = { navigationHandler.back() },
                        sceneStrategies = listOf(dialogStrategy, bottomSheetStrategy),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberSharedViewModelStoreNavEntryDecorator(),
                        ),
                        transitionSpec = { fadeTransition },
                        popTransitionSpec = { fadeTransition },
                        predictivePopTransitionSpec = { fadeTransition },
                        entryProvider = entryProvider {
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

                            featureDestinations
                                .filterNot { it is SettingsCameraUploadsFeatureDestination }
                                .forEach { destination ->
                                    destination.navigationGraph(
                                        this,
                                        navigationHandler,
                                        transferHandler,
                                    )
                                }

                            appDialogDestinations.forEach { destination ->
                                destination.navigationGraph(
                                    this,
                                    navigationHandler,
                                    {},
                                )
                            }
                        },
                    )

                    StartTransferComponent(
                        event = transferState.transferEvent,
                        onConsumeEvent = appTransferViewModel::consumedTransferEvent,
                    )
                }
            }
        }
    }
}

const val INTENT_EXTRA_KEY_SHOW_DISABLE_CU = "SHOW_DISABLE_CU"
