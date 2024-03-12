package mega.privacy.android.app.mediaplayer.trackinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.MediaPlayerViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
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
                MegaAppTheme(isDark = mode.isDarkMode()) {
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

                            viewModel.makeAvailableOffline(args.handle, requireActivity())
                        })
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
        collectFlow(
            viewModel.state.map { it.offlineRemoveSnackBarShow }.distinctUntilChanged()
        ) { show ->
            if (show == true) {
                Util.showSnackbar(activity, getString(R.string.file_removed_offline))
            }
        }
        viewModel.loadTrackInfo(args.handle)
    }
}
