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
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentFavouriteFolderBinding
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.model.ChildrenNodesLoadState
import mega.privacy.android.app.presentation.favourites.model.FavouritesEventState
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import javax.inject.Inject

/**
 * The Fragment for open the file from favourites tab of homepage
 */
@AndroidEntryPoint
class FavouriteFolderFragment: Fragment() {
    private val viewModel by viewModels<FavouriteFolderViewModel>()
    private val thumbnailViewModel by viewModels<ThumbnailViewModel>()
    private lateinit var binding: FragmentFavouriteFolderBinding
    private lateinit var adapter: FavouritesAdapter

    @Inject
    lateinit var megaUtilWrapper: MegaUtilWrapper

    @Inject
    lateinit var openFileWrapper: OpenFileWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavouriteFolderBinding.inflate(inflater, container, false)
        binding.emptyHintText.text = TextUtil.formatEmptyScreenText(
            context,
            getString(R.string.file_browser_empty_folder_new)
        ).toSpannedHtmlText()
        setupAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlow()

        with(requireActivity()) {
            onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    // The function is invoked if back button is clicked when current fragment is opened
                    override fun handleOnBackPressed() {
                        if (viewModel.shouldHandleBackPressed()) {
                            isEnabled = false
                            onBackPressed()
                        }
                    }
                })
        }
    }

    /**
     * Setup adapter
     */
    private fun setupAdapter(){
        adapter = FavouritesAdapter(
            onItemClicked = { item, _, _->
                viewModel.openFile(item)
            },
            onThreeDotsClicked = { item ->
                viewModel.threeDotsClicked(item)
            },
            getThumbnail = { handle, onFinished ->
                thumbnailViewModel.getThumbnail(handle, onFinished)
            }
        )
        binding.fileListViewBrowser.adapter = adapter
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
                        is FavouritesEventState.OpenFolder -> {
                            findNavController().navigate(
                                HomepageFragmentDirections.actionHomepageFragmentToFavouritesFolderFragment(
                                    eventState.parentHandle
                                )
                            )
                        }
                        is FavouritesEventState.ActionModeState -> {}
                    }
                }
            }
        }
    }

    /**
     * Set toolbar text
     */
    private fun setToolbarText(nodeName: String) {
        (activity as AppCompatActivity).supportActionBar?.title = nodeName
    }

    /**
     * Open node
     * @param favourite FavouriteFile
     */
    private fun openNode(favourite: FavouriteFile) {
        MimeTypeList.typeForName(favourite.name).apply {
            when {
                isImage ||
                        (isVideoReproducible || isAudio) ||
                        isPdf -> {
                    launchIntent(
                        openFileWrapper.getIntentForOpenFile(
                            context = requireContext(),
                            node = favourite.node,
                            isText = false,
                            availablePlaylist = true,
                            snackbarShower = activity as ManagerActivity
                        )
                    )
                }
                isURL -> {
                    megaUtilWrapper.manageURLNode(requireContext(), favourite.node)
                }
                isOpenableTextFile(favourite.size) -> {
                    MegaNodeUtil.manageTextFileIntent(
                        requireContext(),
                        favourite.node,
                        Constants.FAVOURITES_ADAPTER
                    )
                }
                else -> {
                    MegaNodeUtil.onNodeTapped(
                        context = requireContext(),
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