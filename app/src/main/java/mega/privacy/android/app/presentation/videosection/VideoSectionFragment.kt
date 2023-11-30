package mega.privacy.android.app.presentation.videosection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentVideoSectionBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import java.io.File
import javax.inject.Inject

/**
 * The Fragment for video section
 */
@AndroidEntryPoint
class VideoSectionFragment : Fragment(), HomepageSearchable {

    private val videoSectionViewModel by viewModels<VideoSectionViewModel>()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private var _binding: FragmentVideoSectionBinding? = null
    private val binding get() = _binding!!

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVideoSectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initVideoSectionComposeView()
        setupMiniAudioPlayer()

        sortByHeaderViewModel.orderChangeEvent.observe(
            viewLifecycleOwner, EventObserver { videoSectionViewModel.refreshWhenOrderChanged() }
        )

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.isPendingRefresh }.distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(videoSectionViewModel) {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.allVideos }.distinctUntilChanged()
        ) { list ->
            if (!videoSectionViewModel.state.value.searchMode && list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }
    }

    private fun initVideoSectionComposeView() {
        binding.videoSectionComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                MegaAppTheme(isDark = themeMode.isDarkMode()) {
                    VideoSectionComposeView(
                        onSortOrderClick = { showSortByPanel() },
                        videoSectionViewModel = videoSectionViewModel,
                        onClick = { item, index ->
                            openVideoFile(activity = requireActivity(), item = item, index = index)
                        },
                        onLongClick = null,
                        onMenuClick = { item ->
                            showOptionsMenuForItem(item)
                        }
                    )
                }
            }
        }
    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    private fun openVideoFile(
        activity: Activity,
        item: UIVideo,
        index: Int,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val nodeHandle = item.id.longValue
            val nodeName = item.name
            val intent = getIntent(item = item, index = index)

            activity.startActivity(
                videoSectionViewModel.isLocalFile(nodeHandle)?.let { localPath ->
                    File(localPath).let { mediaFile ->
                        runCatching {
                            FileProvider.getUriForFile(
                                activity,
                                AUTHORITY_STRING_FILE_PROVIDER,
                                mediaFile
                            )
                        }.onFailure {
                            Uri.fromFile(mediaFile)
                        }.map { mediaFileUri ->
                            intent.setDataAndType(
                                mediaFileUri,
                                MimeTypeList.typeForName(nodeName).type
                            )
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    }
                    intent
                } ?: videoSectionViewModel.updateIntent(
                    handle = nodeHandle,
                    name = nodeName,
                    intent = intent
                )
            )
        }
    }

    private fun getIntent(
        item: UIVideo,
        index: Int,
    ) = Util.getMediaIntent(activity, item.name).apply {
        putExtra(INTENT_EXTRA_KEY_POSITION, index)
        putExtra(INTENT_EXTRA_KEY_HANDLE, item.id.longValue)
        putExtra(INTENT_EXTRA_KEY_FILE_NAME, item.name)
        putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, VIDEO_BROWSE_ADAPTER)
        putExtra(
            INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
            sortByHeaderViewModel.cloudSortOrder.value
        )
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    /**
     * Shows Options menu for item clicked
     */
    private fun showOptionsMenuForItem(item: UIVideo) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = item.id,
            mode = NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
        )
    }

    /**
     * Establish the mini audio player
     */
    private fun setupMiniAudioPlayer() {
        val audioPlayerController = MiniAudioPlayerController(binding.miniAudioPlayer).apply {
            shouldVisible = true
        }
        lifecycle.addObserver(audioPlayerController)
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Should show search menu
     *
     * @return true if should show search menu, false otherwise
     */
    override fun shouldShowSearchMenu(): Boolean = videoSectionViewModel.shouldShowSearchMenu()

    /**
     * Search ready
     */
    override fun searchReady() {
        videoSectionViewModel.searchReady()
    }

    /**
     * Search query
     *
     * @param query query string
     */
    override fun searchQuery(query: String) {
        videoSectionViewModel.searchQuery(query)
    }

    /**
     * Exit search
     */
    override fun exitSearch() {
        videoSectionViewModel.exitSearch()
    }
}