package mega.privacy.android.app.presentation.recentactions

import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.HeaderItemDecoration
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.components.scrollBar.FastScrollerScrollListener
import mega.privacy.android.app.databinding.FragmentRecentActionsBinding
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.mapper.GetIntentToOpenFileMapper
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.Node
import timber.log.Timber
import javax.inject.Inject

/**
 * Recent actions page
 */
@AndroidEntryPoint
class RecentActionsFragment : Fragment() {

    private lateinit var binding: FragmentRecentActionsBinding

    /**
     * Mapper to open file
     */
    @Inject
    lateinit var getIntentToOpenFileMapper: GetIntentToOpenFileMapper

    /**
     * Adapter holding the list of recent action
     */
    @Inject
    lateinit var adapter: RecentActionsAdapter

    private lateinit var emptyLayout: ScrollView
    private lateinit var emptyText: TextView
    private lateinit var showActivityButton: Button
    private lateinit var emptySpanned: Spanned
    private lateinit var activityHiddenSpanned: Spanned
    private lateinit var listView: RecyclerView
    private lateinit var fastScroller: FastScroller
    private lateinit var nodeController: NodeController

    private val viewModel: RecentActionsViewModel by activityViewModels()

    private var homepageFragment: HomepageFragment? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRecentActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nodeController = NodeController(requireActivity())
        setupView()
        observeDragSupportEvents(viewLifecycleOwner, listView, Constants.VIEWER_FROM_RECENT_ACTIONS)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                    Timber.d("Collect ui state")
                    setRecentActions(it.recentActionItems)
                    displayRecentActionsActivity(
                        it.hideRecentActivity,
                        it.recentActionItems.size
                    )
                }
            }
        }
    }

    private fun setupView() {
        homepageFragment = parentFragment as? HomepageFragment
        emptyLayout = binding.emptyStateRecents
        emptyText = binding.emptyTextRecents
        showActivityButton = binding.showActivityButton
        showActivityButton.setOnClickListener {
            viewModel.disableHideRecentActivitySetting()
        }
        emptySpanned = TextUtil.formatEmptyScreenText(
            requireContext(),
            getString(R.string.context_empty_recents)
        )
        activityHiddenSpanned = TextUtil.formatEmptyScreenText(
            requireContext(),
            getString(R.string.recents_activity_hidden)
        )
        listView = binding.listViewRecents
        fastScroller = binding.fastscroll

        fastScroller.setUpScrollListener(object : FastScrollerScrollListener{
            override fun onScrolled() {
                homepageFragment?.hideFabButton()
            }

            override fun onScrolledToTop() {
                homepageFragment?.showFabButton()
            }
        })
        initAdapter()
    }

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        adapter.setOnItemClickListener { item, position ->
            if (!item.isKeyVerified) {
                Intent(requireActivity(), AuthenticityCredentialsActivity::class.java)
                    .apply {
                        putExtra(Constants.IS_NODE_INCOMING, item.bucket.nodes[0].isIncomingShare)
                        putExtra(Constants.EMAIL, item.bucket.userEmail)
                    }.let {
                        requireActivity().startActivity(it)
                    }
            } else {
                // If only one element in the bucket
                if (item.bucket.nodes.size == 1) {
                    openFile(item.bucket.nodes[0], position)
                }
                // If more element in the bucket
                else {
                    viewModel.select(item)
                    val currentDestination =
                        Navigation.findNavController(requireView()).currentDestination
                    if (currentDestination != null && currentDestination.id == R.id.homepageFragment) {
                        Navigation.findNavController(requireView())
                            .navigate(
                                HomepageFragmentDirections.actionHomepageToRecentBucket(),
                                NavOptions.Builder().build()
                            )
                    }
                }
            }
        }

        adapter.setOnThreeDotsClickListener { node ->
            if (!Util.isOnline(context)) {
                (requireActivity() as ManagerActivity).showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    requireContext().getString(R.string.error_server_connection_problem), -1
                )
            } else {
                showOptionsMenuForItem(node)
            }
        }

        listView.adapter = adapter
        listView.addItemDecoration(HeaderItemDecoration(requireContext()))
        listView.clipToPadding = false
        listView.itemAnimator = DefaultItemAnimator()
    }

    /**
     * Set the recent actions list to the adapter
     *
     * @param recentActionItems
     */
    private fun setRecentActions(recentActionItems: List<RecentActionItemType>) {
        adapter.setItems(recentActionItems)
        listView.layoutManager =
            TopSnappedStickyLayoutManager(requireContext()) { recentActionItems }
    }

    /**
     * Display the recent actions activity.
     * Hide the activity if the setting to hide is enabled, and shows it if the
     * setting is disabled.
     *
     * @param hideRecentActivity True if the setting to hide the recent activity is enabled,
     * false otherwise.
     * @param listSize
     */
    private fun displayRecentActionsActivity(hideRecentActivity: Boolean, listSize: Int) {
        when {
            hideRecentActivity -> hideActivity()
            listSize == 0 -> showEmptyActivity()
            else -> showActivity(listSize)
        }
    }

    /**
     * Show an empty activity
     */
    private fun showEmptyActivity() {
        emptyLayout.visibility = View.VISIBLE
        listView.visibility = View.GONE
        fastScroller.visibility = View.GONE
        showActivityButton.visibility = View.GONE
        emptyText.text = emptySpanned
    }

    /**
     * Show the recent activity
     *
     * @param listSize
     */
    private fun showActivity(listSize: Int) {
        emptyLayout.visibility = View.GONE
        listView.visibility = View.VISIBLE
        fastScroller.setRecyclerView(listView)
        fastScroller.visibility =
            if (listSize < Constants.MIN_ITEMS_SCROLLBAR)
                View.GONE
            else
                View.VISIBLE
    }

    /**
     * Hide the recent activity
     */
    private fun hideActivity() {
        emptyLayout.visibility = View.VISIBLE
        listView.visibility = View.GONE
        fastScroller.visibility = View.GONE
        showActivityButton.visibility = View.VISIBLE
        emptyText.text = activityHiddenSpanned
    }

    /**
     * Open File
     * @param fileNode
     * @param position
     */
    private fun openFile(fileNode: FileNode, position: Int) {
        lifecycleScope.launch {
            runCatching {
                val intent = getIntentToOpenFileMapper(
                    activity = requireActivity(),
                    fileNode = fileNode,
                    viewType = Constants.FILE_BROWSER_ADAPTER
                )
                intent?.let {
                    if (MegaApiUtils.isIntentAvailable(context, it)) {
                        putThumbnailLocation(
                            intent,
                            listView,
                            position,
                            Constants.VIEWER_FROM_RECENT_ACTIONS,
                            adapter,
                        )
                        startActivity(it)
                    } else {
                        (requireActivity() as? BaseActivity)?.showSnackbar(
                            content = getString(R.string.intent_not_available),
                        )
                    }
                }
            }.onFailure {
                Timber.e(it)
                (requireActivity() as? BaseActivity)?.showSnackbar(
                    content = getString(R.string.general_text_error)
                )
            }
        }
    }

    /**
     * Shows Options menu for item clicked
     *
     * @param node
     */
    private fun showOptionsMenuForItem(node: Node) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = node.id,
            mode = NodeOptionsBottomSheetDialogFragment.RECENTS_MODE,
        )
    }
}
