package mega.privacy.android.app.presentation.favourites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.databinding.FragmentFavouritesBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.mediaplayer.miniplayer.MiniAudioPlayerController
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment.Companion.DOCUMENTS_UPLOAD
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesAdapter
import mega.privacy.android.app.presentation.favourites.adapter.FavouritesGridAdapter
import mega.privacy.android.app.presentation.favourites.adapter.SelectAnimator
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.presentation.favourites.model.FavouritesEventState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject


/**
 * The Fragment for favourites
 */
@AndroidEntryPoint
class FavouritesFragment : Fragment(), HomepageSearchable {
    private val viewModel by viewModels<FavouritesViewModel>()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()
    private val thumbnailViewMode by viewModels<ThumbnailViewModel>()
    private lateinit var binding: FragmentFavouritesBinding
    private lateinit var listAdapter: FavouritesAdapter
    private lateinit var gridAdapter: FavouritesGridAdapter

    @Inject
    lateinit var megaUtilWrapper: MegaUtilWrapper

    @Inject
    lateinit var openFileWrapper: OpenFileWrapper

    private var actionMode: ActionMode? = null
    private var actionModeCallback: FavouriteActionModeCallback? = null
    private var isActionMode = false
    private lateinit var gridLayoutManager: RecyclerView.LayoutManager
    private lateinit var listLayoutManager: RecyclerView.LayoutManager

    private var isList: Boolean = true

    private val fabChangeObserver = androidx.lifecycle.Observer<Boolean> {
        if (it && !isActionMode) {
            showFabButton()
        } else {
            hideFabButton()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFavouritesBinding.inflate(layoutInflater, container, false)
        binding.emptyHintText.text = formatEmptyScreenText(requireContext(),
            getString(R.string.homepage_empty_hint_favourites))
        binding.fastscroll.setRecyclerView(binding.fileListViewBrowser)
        gridLayoutManager = GridLayoutManager(requireContext(), 2)
        listLayoutManager = LinearLayoutManager(requireContext())
        setupAdapter()

        LiveEventBus.get(Constants.EVENT_FAB_CHANGE, Boolean::class.java)
            .observeForever(fabChangeObserver)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlow()
        setupAddFabButton()
        setupMiniAudioPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.exitSearch()
        LiveEventBus.get(Constants.EVENT_FAB_CHANGE, Boolean::class.java)
            .removeObserver(fabChangeObserver)
    }

    /**
     * Setup adapter
     */
    private fun setupAdapter() {
        listAdapter = FavouritesAdapter(
            sortByHeaderViewModel = sortByHeaderViewModel,
            onItemClicked = ::itemClicked,
            onThreeDotsClicked = viewModel::threeDotsClicked,
            onLongClicked = ::itemLongClicked,
            getThumbnail = thumbnailViewMode::getThumbnail
        )

        gridAdapter = FavouritesGridAdapter(
            sortByHeaderViewModel = sortByHeaderViewModel,
            onItemClicked = ::itemClicked,
            onThreeDotsClicked = viewModel::threeDotsClicked,
            onLongClicked = ::itemLongClicked,
            getThumbnail = thumbnailViewMode::getThumbnail
        )
        switchListGridView(sortByHeaderViewModel.isList)
    }

    /**
     * Setup flow
     */
    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.favouritesState.collect { favouritesState ->
                    setViewVisible(favouritesState)
                    if (!favouritesState.showSearch) {
                        requireActivity().invalidateOptionsMenu()
                    }
                    if (favouritesState is FavouriteLoadState.Success) {
                        if (sortByHeaderViewModel.isList) {
                            listAdapter.submitList(favouritesState.favourites)
                        } else {
                            gridAdapter.submitList(favouritesState.favourites)
                        }
                    }
                }
            }
        }

        viewModel.favouritesEventState.flowWithLifecycle(
            viewLifecycleOwner.lifecycle,
            Lifecycle.State.RESUMED
        ).onEach { eventState ->
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
                        NodeOptionsBottomSheetDialogFragment.FAVOURITES_IN_TAB_MODE
                    )
                }
                is FavouritesEventState.OpenFile -> {
                    openNode(eventState.favouriteFile)
                }
                is FavouritesEventState.OpenFolder -> {
                    findNavController().navigate(
                        HomepageFragmentDirections.actionHomepageFragmentToFavouritesFolderFragment(
                            eventState.parentHandle
                        )
                    )
                }
                is FavouritesEventState.ActionModeState -> {
                    eventState.selectedCount.let { selectedCount ->
                        if (selectedCount > 0) {
                            hideFabButton()
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
                            showFabButton()
                        }
                    }
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel(Constants.ORDER_FAVOURITES)
            }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.onOrderChange(order = it.first)
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(
            viewLifecycleOwner,
            EventObserver { isList ->
                if (isList != this.isList) {
                    switchListGridView(isList)
                }
            })
    }

    /**
     * Open node
     * @param favourite FavouriteFile
     */
    private fun openNode(favourite: FavouriteFile) {
        MimeTypeList.typeForName(favourite.typedNode.name).apply {
            when {
                isImage ||
                        (isVideoReproducible || isAudio) ||
                        isPdf -> {
                    launchIntent(
                        openFileWrapper.getIntentForOpenFile(
                            context = requireContext(),
                            node = favourite.node,
                            isText = false,
                            availablePlaylist = false,
                            snackbarShower = activity as ManagerActivity
                        )
                    )
                }
                isURL -> {
                    megaUtilWrapper.manageURLNode(requireContext(), favourite.node)
                }
                isOpenableTextFile(favourite.typedNode.size) -> {
                    MegaNodeUtil.manageTextFileIntent(
                        requireContext(),
                        favourite.node,
                        Constants.FAVOURITES_ADAPTER
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
                FavouriteActionModeCallback((requireActivity() as ManagerActivity), viewModel)
        }
        actionModeCallback?.run {
            actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(this)
        }
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            RunOnUIThreadUtils.post { callManager { it.hideKeyboardSearch() } }
        }
        viewModel.enableSearch()
        hideFabButton()
    }

    override fun exitSearch() {
        viewModel.exitSearch()
        showFabButton()
    }

    override fun searchQuery(query: String) {
        viewModel.searchQuery(query)
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
     * Setup add fab button
     */
    private fun setupAddFabButton() {
        binding.addFabButton.setOnClickListener {
            (requireActivity() as ManagerActivity).showUploadPanel(DOCUMENTS_UPLOAD)
        }
    }

    /**
     * Hides the fabButton
     */
    fun hideFabButton() {
        binding.addFabButton.hide()
    }

    /**
     * Shows the fabButton
     */
    fun showFabButton() {
        binding.addFabButton.show()
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
                findItem(R.id.cab_menu_share_folder), !items.values.any {
                    it !is FavouriteFolder
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
            viewModel.openFile(item)
        }
    }

    /**
     * item long clicked
     * @param item Favourite item
     * @return true to make the view long clickable, false otherwise
     */
    private fun itemLongClicked(item: Favourite): Boolean {
        if (Util.isOnline(context)) {
            viewModel.itemSelected(item)
        } else {
            callManager {
                it.hideKeyboardSearch()  // Make the snack bar visible to the user
                it.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    StringResourcesUtils.getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            }
        }
        return true
    }

    /**
     * Switch the recycle view between list and grid
     * @param isList true is list, otherwise is grid
     */
    private fun switchListGridView(isList: Boolean) {
        this.isList = isList
        with(binding.fileListViewBrowser) {
            if (isList) {
                switchToLinear()
                adapter = listAdapter
            } else {
                switchBackToGrid()
                adapter = gridAdapter
                (layoutManager as CustomizedGridLayoutManager).apply {
                    spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
                }
            }
            itemAnimator = SelectAnimator()
        }
        viewModel.forceUpdateData(isList)
    }
}