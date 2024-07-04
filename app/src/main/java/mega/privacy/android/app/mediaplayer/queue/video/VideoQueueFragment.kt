package mega.privacy.android.app.mediaplayer.queue.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerActivity
import mega.privacy.android.app.mediaplayer.LegacyVideoPlayerViewModel
import mega.privacy.android.app.mediaplayer.queue.view.VideoQueueView
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Video queue fragment for displaying the video queue.
 */
@AndroidEntryPoint
class VideoQueueFragment : Fragment() {
    private val videoQueueViewModel by viewModels<VideoQueueViewModel>()
    private val legacyVideoPlayerViewModel by activityViewModels<LegacyVideoPlayerViewModel>()

    private var playlistObserved = false

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OriginalTempTheme(isDark = true) {
                    VideoQueueView(
                        viewModel = videoQueueViewModel,
                        legacyVideoPlayerViewModel = legacyVideoPlayerViewModel,
                        onDragFinished = { legacyVideoPlayerViewModel.updatePlaySource(false) },
                        onMove = { from, to ->
                            videoQueueViewModel.updateMediaQueueAfterReorder(from, to)
                            legacyVideoPlayerViewModel.swapItems(from, to)
                        },
                        onToolbarColorUpdated = {
                            (activity as? LegacyVideoPlayerActivity)?.setupToolbarColors(it)
                        }
                    ) {
                        activity?.supportFragmentManager?.popBackStack()
                    }
                }
            }
        }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoQueueViewModel.initMediaQueueItemList(legacyVideoPlayerViewModel.getPlaylistItems())
        (activity as? LegacyVideoPlayerActivity)?.hideToolbar()
        tryObservePlaylist()
    }

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        with(legacyVideoPlayerViewModel) {
            if (!playlistObserved && view != null) {
                playlistObserved = true

                viewLifecycleOwner.collectFlow(playlistTitleState) { title ->
                    title?.let {
                        (requireActivity() as MediaPlayerActivity).setToolbarTitle(it)
                    }
                }
            }
        }
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()
        playlistObserved = false
    }
}