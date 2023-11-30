package mega.privacy.android.app.mediaplayer.trackinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.MegaNodeUtil.handleLocationClick
import mega.privacy.android.app.utils.Util
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * The fragment for showing audio track info
 */
@AndroidEntryPoint
class TrackInfoFragment : Fragment() {
    private val args: TrackInfoFragmentArgs by navArgs()
    private val viewModel by viewModels<TrackInfoViewModel>()

    /**
     * [GetThemeMode] injection
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                val mode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                MegaAppTheme(isDark = mode.isDarkMode()) {
                    AudioTrackInfoView(
                        audioNodeInfo = viewModel.audioNodeInfo.asFlow()
                            .collectAsStateWithLifecycle(
                                initialValue = null
                            ).value,
                        metadata = viewModel.metadata.asFlow().collectAsStateWithLifecycle(
                            initialValue = null
                        ).value,
                        onLocationClicked = { location ->
                            location?.let {
                                handleLocationClick(requireActivity(), args.adapterType, it)
                            }
                        },
                        onCheckedChange = { isChecked ->
                            if (viewModel.getStorageState() == StorageState.PayWall) {
                                showOverDiskQuotaPaywallWarning()
                                return@AudioTrackInfoView
                            }

                            viewModel.makeAvailableOffline(isChecked, requireActivity())
                        })
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MediaPlayerActivity).showToolbar(false)
        viewModel.offlineRemoveSnackBarShow.observe(viewLifecycleOwner) {
            if (it) {
                Util.showSnackbar(activity, getString(R.string.file_removed_offline))
            }
        }
        viewModel.loadTrackInfo(args)
    }

    /**
     * Update node name
     *
     * @param handle node handle
     * @param newName the new name
     */
    fun updateNodeNameIfNeeded(handle: Long, newName: String) {
        if (isResumed) {
            viewModel.updateNodeNameIfNeeded(handle, newName)
        }
    }
}
