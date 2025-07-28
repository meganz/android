package mega.privacy.android.app.presentation.favourites

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.databinding.FragmentFavouritesBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesAdapter
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesGridAdapter
import mega.privacy.android.app.presentation.favourites.adapter.SelectAnimator
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteItem
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.FavouritePlaceholderItem
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.FavouriteImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt.FAVOURITES_ADAPTER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.mobile.analytics.event.HideNodeMultiSelectMenuItemEvent
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * The Fragment for favourites
 */
@AndroidEntryPoint
class FavouritesFragment : Fragment() {
    private val viewModel by viewModels<FavouritesViewModel>()
    private val sortByHeaderViewModel by activityViewModels<SortByHeaderViewModel>()
    private lateinit var binding: FragmentFavouritesBinding
    private lateinit var listAdapter: FavouritesAdapter
    private lateinit var gridAdapter: FavouritesGridAdapter

    /**
     * Used to access Mega Util functions
     */
    @Inject
    lateinit var megaUtilWrapper: MegaUtilWrapper

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    /**
     * Used to access Open File functions
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

    private var actionMode: ActionMode? = null
    private var actionModeCallback: FavouriteActionModeCallback? = null
    private var isActionMode = false
    private lateinit var gridLayoutManager: RecyclerView.LayoutManager
    private lateinit var listLayoutManager: RecyclerView.LayoutManager

    /**
     * Is online - Temporary variable to hold state until view is migrated to compose
     */
    private var isOnLine: Boolean = false

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private var tempNodeIds: List<NodeId> = listOf()

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFavouritesBinding.inflate(layoutInflater, container, false)
        binding.emptyHintText.text = formatEmptyScreenText(
            requireContext(),
            getString(R.string.homepage_empty_hint_favourites)
        )
        binding.fastscroll.setRecyclerView(binding.fileListViewBrowser)
        gridLayoutManager = GridLayoutManager(requireContext(), 2)
        listLayoutManager = LinearLayoutManager(requireContext())
        setupAdapter()
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlow()
        setupMiniAudioPlayer()
    }

    /**
     * Setup adapter
     */
    private fun setupAdapter() {
        listAdapter = FavouritesAdapter(
            sortByHeaderViewModel = sortByHeaderViewModel,
            onItemClicked = ::itemClicked,
            onThreeDotsClicked = ::threeDotsClicked,
            onLongClicked = ::itemLongClicked,
        )

        gridAdapter = FavouritesGridAdapter(
            sortByHeaderViewModel = sortByHeaderViewModel,
            onItemClicked = ::itemClicked,
            onThreeDotsClicked = ::threeDotsClicked,
            onLongClicked = ::itemLongClicked,
        )
    }

    /**
     * Three dots clicked
     * @param favourite Favourite
     */
    private fun threeDotsClicked(favourite: Favourite) {
        if (isOnLine) {
            openBottomSheet(favourite.node)
        } else {
            showOfflineNotification()
        }
    }

    /**
     * Setup flow
     */
    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.favouritesState
                    .combine(
                        sortByHeaderViewModel.state
                    ) { favouritesState, sortByHeaderState ->
                        Pair(favouritesState, sortByHeaderState)
                    }.collect { (favouritesState, sortByHeaderState) ->
                        val isList = sortByHeaderState.viewType == ViewType.LIST

                        handleConnectivityState(favouritesState.isConnected)
                        switchViewType(sortByHeaderState.viewType)
                        setViewVisible(favouritesState)
                        if (favouritesState is FavouriteLoadState.Success) {
                            if (isList) {
                                listAdapter.updateSelectionMode(favouritesState.selectedItems.isNotEmpty())
                                listAdapter.updateAccountType(
                                    favouritesState.accountType,
                                    favouritesState.isBusinessAccountExpired,
                                    favouritesState.hiddenNodeEnabled
                                )
                                listAdapter.submitList(favouritesState.favourites)
                            } else {
                                gridAdapter.updateSelectionMode(favouritesState.selectedItems.isNotEmpty())
                                gridAdapter.updateAccountType(
                                    favouritesState.accountType,
                                    favouritesState.isBusinessAccountExpired,
                                    favouritesState.hiddenNodeEnabled
                                )
                                gridAdapter.submitList(formatGridList(favouritesState))
                            }
                            handleSelectedItems(favouritesState.selectedItems)
                        }
                    }
            }
        }

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel(Constants.ORDER_FAVOURITES)
            }
        })

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            viewModel.onOrderChange(sortOrder = it.cloudSortOrder)
        }
    }

    /**
     * Returns a list of Favourite Nodes
     *
     * @param favouritesState [FavouriteLoadState]
     * @return a list of [FavouriteItem] objects
     */
    private fun formatGridList(favouritesState: FavouriteLoadState.Success): List<FavouriteItem> {
        val list = favouritesState.favourites.toMutableList()
        if (list.any { it.favourite is FavouriteFolder }) {
            val firstFileIndex = list.indexOfFirst { it.favourite is FavouriteFile }
            if (firstFileIndex % 2 == 0) {
                list.add(firstFileIndex, FavouritePlaceholderItem())
            }
        }
        return list
    }

    /**
     * Handle the selected Favourite Nodes
     *
     * @param selectedItems A Set of Favourite [NodeId] objects
     */
    private fun handleSelectedItems(selectedItems: Set<NodeId>) {
        val selectedCount = selectedItems.size
        if (selectedCount > 0) {
            if (!isActionMode) {
                activateActionMode()
                isActionMode = true
            }
            actionMode?.run {
                menuUpdated(
                    menu = menu,
                    count = selectedCount,
                    items = viewModel.getItemsSelected()
                )
            }
            actionMode?.title = selectedCount.toString()
        } else {
            actionMode?.finish()
            this@FavouritesFragment.isActionMode = false
        }
    }

    /**
     * Opens the [NodeOptionsBottomSheetDialogFragment] to perform Favourites-related functions
     *
     * @param node The Favourite [MegaNode]
     */
    private fun openBottomSheet(node: MegaNode) {
        (activity as ManagerActivity).showNodeOptionsPanel(
            node,
            NodeOptionsBottomSheetDialogFragment.FAVOURITES_IN_TAB_MODE
        )
    }

    /**
     * Handle the app connectivity state
     *
     * @param isConnected True if the user is connected to the Internet
     */
    private fun handleConnectivityState(isConnected: Boolean) {
        isOnLine = isConnected
        if (!isConnected) showOfflineNotification()
    }

    /**
     * Displays a Snackbar to indicate that the user's device is offline
     */
    private fun showOfflineNotification() {
        Snackbar.make(
            requireView(),
            getString(R.string.error_server_connection_problem),
            Snackbar.LENGTH_SHORT
        ).show()
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
                        isMediaQueueAvailable = false
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
                                    imageSource = ImagePreviewFetcherSource.FAVOURITE,
                                    menuOptionsSource = ImagePreviewMenuSource.FAVOURITE,
                                    anchorImageNodeId = NodeId(handle),
                                    params = mapOf(FavouriteImageNodeFetcher.NODE_ID to handle),
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
     * @param uiState FavouriteLoadState
     */
    private fun setViewVisible(uiState: FavouriteLoadState) {
        with(binding) {
            emptyHint.isVisible = uiState is FavouriteLoadState.Empty
            favouriteProgressbar.isVisible = uiState is FavouriteLoadState.Loading
            fileListViewBrowser.isVisible = uiState is FavouriteLoadState.Success
        }
    }

    /**
     * Activated the action mode
     */
    private fun activateActionMode() {
        if (actionModeCallback == null) {
            actionModeCallback =
                FavouriteActionModeCallback(
                    (requireActivity() as ManagerActivity),
                    viewModel,
                    this@FavouritesFragment,
                    requireContext()
                )
        }
        actionModeCallback?.run {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(this)
        }
    }

    /**
     * Setup mini audio player
     */
    private fun setupMiniAudioPlayer() {
        val audioPlayerController = MiniAudioPlayerController(binding.miniAudioPlayer).apply {
            shouldVisible = true
        }
        lifecycle.addObserver(audioPlayerController)
    }


    /**
     * Update the menu UI
     * @param menu Menu
     * @param count the count of selected items
     * @param items the selected items map
     */
    private fun menuUpdated(menu: Menu, count: Int, items: Map<Long, Favourite>) {
        with(menu) {
            showAsActionAndVisibility(findItem(R.id.cab_menu_download), true)
            showAsActionAndVisibility(findItem(R.id.cab_menu_share_link), true)
            showAsActionAndVisibility(
                findItem(R.id.cab_menu_share_folder), items.values.all {
                    it is FavouriteFolder
                }
            )
            showAsActionAndVisibility(findItem(R.id.cab_menu_share_out), true)
            findItem(R.id.cab_menu_select_all).isVisible = true
            findItem(R.id.cab_menu_clear_selection).isVisible = true
            findItem(R.id.cab_menu_rename).isVisible = count == 1
            findItem(R.id.cab_menu_remove_favourites).isVisible = true
            findItem(R.id.cab_menu_copy).isVisible = true
            findItem(R.id.cab_menu_trash).isVisible = true
        }
    }

    /**
     * Set the showAsAction and visibility for menu item
     * @param item MenuItem
     * @param visibility the visibility of menu item
     */
    private fun showAsActionAndVisibility(item: MenuItem, visibility: Boolean) {
        item.run {
            isVisible = visibility
            setShowAsAction(SHOW_AS_ACTION_ALWAYS)
        }
    }

    /**
     * Item clicked
     * @param item Favourite item
     */
    private fun itemClicked(item: Favourite) {
        if (isActionMode) {
            viewModel.itemSelected(item)
        } else {
            openFavourite(item)
        }
    }

    private fun openFavourite(item: Favourite) {
        if (item.typedNode.isTakenDown && item !is FavouriteFolder) {
            megaNodeUtilWrapper.showTakenDownDialog(
                isFolder = false,
                context = requireContext(),
            )
        } else {
            when (item) {
                is FavouriteFile -> openNode(item)
                is FavouriteFolder -> {
                    findNavController().navigate(
                        HomepageFragmentDirections.actionHomepageFragmentToFavouritesFolderFragment(
                            item.typedNode.id.longValue
                        )
                    )
                }
            }
        }
    }

    /**
     * item long clicked
     * @param item Favourite item
     * @return true to make the view long clickable, false otherwise
     */
    @Suppress("DEPRECATION")
    private fun itemLongClicked(item: Favourite): Boolean {
        if (Util.isOnline(context)) {
            viewModel.itemSelected(item)
        } else {
            callManager {
                it.hideKeyboardSearch()  // Make the snack bar visible to the user
                it.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            }
        }
        return true
    }

    /**
     * Switches how Favourite items are being displayed
     *
     * @param viewType The View Type
     */
    private fun switchViewType(viewType: ViewType) {
        with(binding.fileListViewBrowser) {
            when (viewType) {
                ViewType.LIST -> {
                    switchToLinear()
                    adapter = listAdapter
                }

                ViewType.GRID -> {
                    switchBackToGrid()
                    adapter = gridAdapter
                    (layoutManager as CustomizedGridLayoutManager).apply {
                        spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
                    }
                }
            }
            itemAnimator = SelectAnimator()
        }
    }

    /**
     * on hide clicked
     * @param nodeIds List<NodeId>
     */
    fun onHideClicked(
        nodeIds: List<NodeId>,
    ) {
        Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)

        val isHiddenNodesOnboarded = viewModel.isHiddenNodesOnboarded()
        val isPaid = viewModel.getIsPaidAccount()
        val isBusinessAccountExpired = viewModel.getIsBusinessAccountExpired()

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            viewModel.hideOrUnhideNodes(nodeIds, true)
            val message = resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                nodeIds.size,
                nodeIds.size,
            )
            Util.showSnackbar(requireActivity(), message)
        } else {
            this.tempNodeIds = nodeIds
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        viewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = requireContext(),
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        viewModel.hideOrUnhideNodes(tempNodeIds, true)

        val message = resources.getQuantityString(
            R.plurals.hidden_nodes_result_message,
            tempNodeIds.size,
            tempNodeIds.size,
        )
        Util.showSnackbar(requireActivity(), message)
    }
}