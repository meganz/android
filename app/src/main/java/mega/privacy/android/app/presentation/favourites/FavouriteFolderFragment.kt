package mega.privacy.android.app.presentation.favourites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentFavouriteFolderBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.favourites.FavouriteFolderViewModel.Companion.KEY_ARGUMENT_PARENT_HANDLE
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesAdapter
import mega.privacy.android.app.presentation.favourites.adapter.SelectAnimator
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.FavouritesEventState
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.FAVOURITES_ADAPTER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import timber.log.Timber
import javax.inject.Inject

/**
 * The Fragment for open the file from favourites tab of homepage
 */
@AndroidEntryPoint
class FavouriteFolderFragment : Fragment() {
    private val viewModel by viewModels<FavouriteFolderViewModel>()
    private lateinit var binding: FragmentFavouriteFolderBinding
    private lateinit var adapter: FavouritesAdapter

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    /**
     * MegaUtilWrapper variable
     */
    @Inject
    lateinit var megaUtilWrapper: MegaUtilWrapper

    /**
     * OpenFileWrapper variable
     */
    @Inject
    lateinit var openFileWrapper: OpenFileWrapper

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * MegaNavigator injection
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.backToPreviousPage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFavouriteFolderBinding.inflate(inflater, container, false)
        binding.emptyHintText.text = formatEmptyScreenText(
            context,
            getString(R.string.file_browser_empty_folder_new)
        )
        initData()
        setupAdapter()
        setupAddFabButton()
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlow()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    /**
     * Returns the parent node handle
     */
    fun getParentNodeHandle() = viewModel.getParentNodeHandle()

    private fun initData() {
        viewModel.init(arguments?.getLong(KEY_ARGUMENT_PARENT_HANDLE) ?: -1)
    }

    /**
     * Setup adapter
     */
    private fun setupAdapter() {
        adapter = FavouritesAdapter(
            onItemClicked = {
                if (it.typedNode.isTakenDown && it !is FavouriteFolder) {
                    megaNodeUtilWrapper.showTakenDownDialog(
                        isFolder = false,
                        context = requireContext(),
                    )
                } else {
                    viewModel.openFile(it)
                }
            },
            onThreeDotsClicked = viewModel::threeDotsClicked,
        )
        binding.fileListViewBrowser.adapter = adapter
        binding.fileListViewBrowser.itemAnimator = SelectAnimator()
    }

    /**
     * Setup add fab button
     */
    private fun setupAddFabButton() {
        binding.addFabButton.setOnClickListener {
            (requireActivity() as? ManagerActivity)?.showUploadPanel()
        }
    }

    /**
     * Setup flow
     */
    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.childrenNodesState.collect { childrenState ->
                    setViewVisible(childrenState)
                    if (childrenState is ChildrenNodesLoadState.Success) {
                        setToolbarText(childrenState.title)
                        // According to the state to enable the onBackPressedCallback
                        onBackPressedCallback.isEnabled = childrenState.isBackPressedEnable
                        adapter.updateAccountType(
                            childrenState.accountType,
                            childrenState.isBusinessAccountExpired,
                            childrenState.hiddenNodeEnabled,
                        )
                        adapter.submitList(childrenState.children)
                    } else if (childrenState is ChildrenNodesLoadState.Empty) {
                        setToolbarText(
                            childrenState.title ?: getString(R.string.favourites_category_title)
                        )
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.favouritesEventState.collect { eventState ->
                    when (eventState) {
                        is FavouritesEventState.Offline -> {
                            Snackbar.make(
                                requireView(),
                                getString(R.string.error_server_connection_problem),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }

                        is FavouritesEventState.OpenBottomSheetFragment -> {
                            (activity as ManagerActivity).showNodeOptionsPanel(
                                eventState.favourite.node,
                                NodeOptionsBottomSheetDialogFragment.FAVOURITES_MODE
                            )
                        }

                        is FavouritesEventState.OpenFile -> {
                            openNode(eventState.favouriteFile)
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * Set toolbar text
     */
    private fun setToolbarText(nodeName: String) {
        (activity as AppCompatActivity).supportActionBar?.apply {
            show()
            title = nodeName
        }
    }

    /**
     * Open node
     * @param favourite FavouriteFile
     */
    private fun openNode(favourite: FavouriteFile) {
        val fileTypeInfo = viewModel.getFileTypeInfo(favourite.typedNode.name) ?: return
        if (fileTypeInfo is VideoFileTypeInfo || fileTypeInfo is AudioFileTypeInfo) {
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    val contentUri = viewModel.getNodeContentUri(favourite.typedNode)
                    megaNavigator.openMediaPlayerActivityByFileNode(
                        context = requireContext(),
                        contentUri = contentUri,
                        fileNode = favourite.typedNode,
                        viewType = FAVOURITES_ADAPTER,
                    )
                }.onFailure {
                    Timber.e(it)
                    Snackbar.make(
                        requireView(),
                        getString(R.string.intent_not_available),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            MimeTypeList.typeForName(favourite.typedNode.name).apply {
                when {
                    isImage || isPdf -> viewLifecycleOwner.lifecycleScope.launch {
                        if (isImage) {
                            val handle = favourite.node.handle
                            launchIntent(
                                ImagePreviewActivity.createIntent(
                                    context = requireContext(),
                                    imageSource = ImagePreviewFetcherSource.DEFAULT,
                                    menuOptionsSource = ImagePreviewMenuSource.FAVOURITE,
                                    anchorImageNodeId = NodeId(handle),
                                    params = mapOf(
                                        DefaultImageNodeFetcher.NODE_IDS to longArrayOf(
                                            handle
                                        )
                                    ),
                                )
                            )
                        } else {
                            launchIntent(
                                openFileWrapper.getIntentForOpenFile(
                                    context = requireContext(),
                                    node = favourite.node,
                                    isText = false,
                                    snackbarShower = activity as ManagerActivity
                                )
                            )
                        }
                    }

                    isURL -> {
                        megaUtilWrapper.manageURLNode(requireContext(), favourite.node)
                    }

                    isOpenableTextFile(favourite.typedNode.size) -> {
                        MegaNodeUtil.manageTextFileIntent(
                            requireContext(),
                            favourite.node,
                            FAVOURITES_ADAPTER
                        )
                    }

                    else -> {
                        MegaNodeUtil.onNodeTapped(
                            context = requireActivity(),
                            node = favourite.node,
                            nodeDownloader = (activity as ManagerActivity)::saveNodeByTap,
                            activityLauncher = activity as ManagerActivity,
                            snackbarShower = activity as ManagerActivity
                        )
                    }
                }
            }
        }
    }

    /**
     * Launch Intent
     * @param intent the intent that will be launch
     */
    private fun launchIntent(intent: Intent?) {
        intent?.run {
            requireActivity().startActivity(this)
        } ?: run {
            Snackbar.make(
                requireView(),
                getString(R.string.intent_not_available),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Set view visible according state
     * @param uiState ChildrenNodesLoadState
     */
    private fun setViewVisible(uiState: ChildrenNodesLoadState) {
        with(binding) {
            emptyHint.isVisible = uiState is ChildrenNodesLoadState.Empty
            favouriteProgressbar.isVisible = uiState is ChildrenNodesLoadState.Loading
            fileListViewBrowser.isVisible = uiState is ChildrenNodesLoadState.Success
        }
    }
}