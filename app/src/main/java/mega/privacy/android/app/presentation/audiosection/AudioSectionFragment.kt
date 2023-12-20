package mega.privacy.android.app.presentation.audiosection

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
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
import mega.privacy.android.app.databinding.FragmentAudioSectionBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import java.io.File
import javax.inject.Inject

/**
 * The fragment for audio section
 */
@AndroidEntryPoint
class AudioSectionFragment : Fragment(), HomepageSearchable {
    private val audioSectionViewModel by viewModels<AudioSectionViewModel>()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private var _binding: FragmentAudioSectionBinding? = null
    private val binding get() = _binding!!

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAudioSectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initAudioSectionComposeView()
        setupMiniAudioPlayer()

        sortByHeaderViewModel.orderChangeEvent.observe(
            viewLifecycleOwner, EventObserver { audioSectionViewModel.refreshWhenOrderChanged() }
        )

        viewLifecycleOwner.collectFlow(
            audioSectionViewModel.state.map { it.isPendingRefresh }.distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(audioSectionViewModel) {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            audioSectionViewModel.state.map { it.allAudios }.distinctUntilChanged()
        ) { list ->
            if (!audioSectionViewModel.state.value.searchMode && list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }
    }

    private fun initAudioSectionComposeView() {
        binding.audioSectionComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by audioSectionViewModel.state.collectAsStateWithLifecycle()
                MegaAppTheme(isDark = themeMode.isDarkMode()) {
                    AudioSectionComposeView(
                        uiState = uiState,
                        onChangeViewTypeClick = audioSectionViewModel::onChangeViewTypeClicked,
                        onSortOrderClick = { showSortByPanel() },
                        onClick = { item, index ->
                            openAudioFile(
                                activity = requireActivity(),
                                item = item,
                                index = index
                            )
                        },
                        onMenuClick = { item ->
                            showOptionsMenuForItem(item)
                        }
                    )
                }
            }
        }
    }

    private fun showSortByPanel() {
        (requireActivity() as? ManagerActivity)?.showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    private fun openAudioFile(
        activity: Activity,
        item: UIAudio,
        index: Int,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val nodeHandle = item.id.longValue
            val nodeName = item.name
            val intent = getIntent(item = item, index = index)

            activity.startActivity(
                audioSectionViewModel.isLocalFile(nodeHandle)?.let { localPath ->
                    File(localPath).let { mediaFile ->
                        runCatching {
                            FileProvider.getUriForFile(
                                activity,
                                Constants.AUTHORITY_STRING_FILE_PROVIDER,
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
                } ?: audioSectionViewModel.updateIntent(
                    handle = nodeHandle,
                    name = nodeName,
                    intent = intent
                )
            )
        }
    }

    private fun getIntent(
        item: UIAudio,
        index: Int,
    ) = getMediaIntent(activity, item.name).apply {
        putExtra(INTENT_EXTRA_KEY_POSITION, index)
        putExtra(INTENT_EXTRA_KEY_HANDLE, item.id.longValue)
        putExtra(INTENT_EXTRA_KEY_FILE_NAME, item.name)
        putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, AUDIO_BROWSE_ADAPTER)
        putExtra(
            INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
            sortByHeaderViewModel.cloudSortOrder.value
        )
        addFlags(FLAG_ACTIVITY_SINGLE_TOP)
    }

    private fun showOptionsMenuForItem(item: UIAudio) {
        (requireActivity() as? ManagerActivity)?.showNodeOptionsPanel(
            nodeId = item.id,
            mode = NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
        )
    }

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
    override fun shouldShowSearchMenu(): Boolean = audioSectionViewModel.shouldShowSearchMenu()

    /**
     * Search ready
     */
    override fun searchReady() {
        audioSectionViewModel.searchReady()
    }

    /**
     * Search query
     *
     * @param query query string
     */
    override fun searchQuery(query: String) {
        audioSectionViewModel.searchQuery(query)
    }

    /**
     * Exit search
     */
    override fun exitSearch() {
        audioSectionViewModel.exitSearch()
    }
}