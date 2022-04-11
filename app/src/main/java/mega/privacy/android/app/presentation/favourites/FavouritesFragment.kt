package mega.privacy.android.app.presentation.favourites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import mega.privacy.android.app.databinding.FragmentFavouritesBinding
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.model.ClickEventState
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import timber.log.Timber
import javax.inject.Inject

/**
 * The Fragment for favourites
 */
@AndroidEntryPoint
class FavouritesFragment : Fragment() {
    private val viewModel by viewModels<FavouritesViewModel>()
    private lateinit var binding: FragmentFavouritesBinding
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
        binding = FragmentFavouritesBinding.inflate(layoutInflater, container, false)
        binding.emptyHintText.text = TextUtil.formatEmptyScreenText(
            context,
            getString(R.string.empty_hint_favourite_album)
        ).toSpannedHtmlText()
        binding.fastscroll.setRecyclerView(binding.fileListViewBrowser)
        setupAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlow()
    }

    /**
     * Setup adapter
     */
    private fun setupAdapter() {
        adapter = FavouritesAdapter(
            onItemClicked = { item ->
                viewModel.openFile(item)
            },
            onThreeDotsClicked = { item ->
                viewModel.threeDotsClicked(item)
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
                viewModel.favouritesState.collect { favouritesState ->
                    setViewVisible(favouritesState)
                    if (favouritesState is FavouriteLoadState.Success) {
                        adapter.submitList(favouritesState.favourites)
                    } else if (favouritesState is FavouriteLoadState.Error) {
                        Timber.e(favouritesState.exception)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.clickEventState.collect { clickEventState ->
                    when (clickEventState) {
                        is ClickEventState.Offline -> {
                            Snackbar.make(
                                requireView(),
                                getString(R.string.error_server_connection_problem),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        is ClickEventState.OpenBottomSheetFragment -> {
                            (activity as ManagerActivity).showNodeOptionsPanel(
                                clickEventState.favourite.node,
                                NodeOptionsBottomSheetDialogFragment.FAVOURITES_IN_TAB_MODE
                            )
                        }
                        is ClickEventState.OpenFile -> {
                            openNode(clickEventState.favouriteFile)
                        }
                        is ClickEventState.OpenFolder -> {
                            findNavController().navigate(
                                HomepageFragmentDirections.actionHomepageFragmentToFavouritesFolderFragment(
                                    clickEventState.parentHandle
                                )
                            )
                        }
                    }
                }
            }
        }
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
                            availablePlaylist = false,
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
     * @param uiState FavouriteLoadState
     */
    private fun setViewVisible(uiState: FavouriteLoadState) {
        with(binding) {
            emptyHint.isVisible = uiState is FavouriteLoadState.Empty
            favouriteProgressbar.isVisible = uiState is FavouriteLoadState.Loading
            fileListViewBrowser.isVisible = uiState is FavouriteLoadState.Success
        }
    }
}