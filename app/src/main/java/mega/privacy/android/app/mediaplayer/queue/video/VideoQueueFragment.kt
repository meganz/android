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
import mega.privacy.android.app.mediaplayer.VideoPlayerActivity
import mega.privacy.android.app.mediaplayer.VideoPlayerViewModel
import mega.privacy.android.app.mediaplayer.queue.view.VideoQueueView
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Video queue fragment for displaying the video queue.
 */
@AndroidEntryPoint
class VideoQueueFragment : Fragment() {
    private val videoQueueViewModel by viewModels<VideoQueueViewModel>()
    private val videoPlayerViewModel by activityViewModels<VideoPlayerViewModel>()

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
                        videoPlayerViewModel = videoPlayerViewModel,
                        onDragFinished = { videoPlayerViewModel.updatePlaySource(false) },
                        onMove = { from, to ->
                            videoQueueViewModel.updateMediaQueueAfterReorder(from, to)
                            videoPlayerViewModel.swapItems(from, to)
                        },
                        onToolbarColorUpdated = {
                            (activity as? VideoPlayerActivity)?.setupToolbarColors(it)
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
        videoQueueViewModel.initMediaQueueItemList(videoPlayerViewModel.getPlaylistItems())
        (activity as? VideoPlayerActivity)?.hideToolbar()
        tryObservePlaylist()
    }

    /**
     * Observe playlist LiveData when view is created and service is connected.
     */
    private fun tryObservePlaylist() {
        with(videoPlayerViewModel) {
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