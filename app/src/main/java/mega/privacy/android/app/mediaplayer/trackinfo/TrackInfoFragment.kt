package mega.privacy.android.app.mediaplayer.trackinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import javax.inject.Inject

/**
 * The fragment for showing audio track info
 */
@AndroidEntryPoint
class TrackInfoFragment : Fragment() {
    private val args: TrackInfoFragmentArgs by navArgs()
    private val viewModel by viewModels<TrackInfoViewModel>()
    private val audioPlayerViewModel by activityViewModels<MediaPlayerViewModel>()

    /**
     * [GetThemeMode] injection
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                val metadata =
                    audioPlayerViewModel.metadataState.collectAsStateWithLifecycle().value
                val uiState = viewModel.state.collectAsStateWithLifecycle().value
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val snackbarHostState = remember { SnackbarHostState() }
                OriginalTempTheme(isDark = mode.isDarkMode()) {
                    AudioTrackInfoView(
                        uiState = uiState,
                        metadata = metadata,
                        onLocationClicked = { location ->
                            location?.let {
                                handleLocationClick(requireActivity(), args.adapterType, it)
                            }
                        },
                        onCheckedChange = {
                            if (viewModel.getStorageState() == StorageState.PayWall) {
                                showOverDiskQuotaPaywallWarning()
                                return@AudioTrackInfoView
                            }

                            viewModel.makeAvailableOffline(args.handle)
                        })
                    LegacySnackBarWrapper(snackbarHostState = snackbarHostState, activity)
                    StartTransferComponent(
                        event = uiState.transferTriggerEvent,
                        onConsumeEvent = viewModel::consumeTransferEvent,
                        snackBarHostState = snackbarHostState,
                    )
                    EventEffect(
                        event = uiState.offlineRemovedEvent,
                        onConsumed = viewModel::consumeOfflineRemovedEvent
                    ) {
                        snackbarHostState.showAutoDurationSnackbar(getString(R.string.file_removed_offline))
                    }
                }
            }
        }
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MediaPlayerActivity).showToolbar(false)
        viewModel.loadTrackInfo(args.handle)
    }
}
