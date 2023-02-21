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
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.HeaderItemDecoration
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.components.scrollBar.FastScroller
import mega.privacy.android.app.databinding.FragmentRecentsBinding
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForSingleNode
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.recentactions.model.RecentActionItemType
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Recent actions page
 */
@AndroidEntryPoint
class RecentActionsFragment : Fragment() {

    private lateinit var binding: FragmentRecentsBinding

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRecentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nodeController = NodeController(requireActivity())
        setupView()
        observeDragSupportEvents(viewLifecycleOwner, listView, Constants.VIEWER_FROM_RECETS)

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
        emptyLayout = binding.emptyStateRecents
        emptyText = binding.emptyTextRecents
        showActivityButton = binding.showActivityButton
        showActivityButton.setOnClickListener {
            viewModel.disableHideRecentActivitySetting()
        }
        emptySpanned = TextUtil.formatEmptyScreenText(requireContext(),
            StringResourcesUtils.getString(R.string.context_empty_recents))
        activityHiddenSpanned = TextUtil.formatEmptyScreenText(requireContext(),
            StringResourcesUtils.getString(R.string.recents_activity_hidden))
        listView = binding.listViewRecents
        fastScroller = binding.fastscroll

        initAdapter()
    }

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        adapter.setOnItemClickListener { item, position ->
            if (!item.isKeyVerified) {
                lifecycleScope.launch {
                    viewModel.getMegaNode(item.bucket.nodes[0].id.longValue)?.let { megaNode ->
                        Intent(requireActivity(),
                            AuthenticityCredentialsActivity::class.java).apply {
                            putExtra(Constants.IS_NODE_INCOMING,
                                nodeController.nodeComesFromIncoming(megaNode))
                            putExtra(Constants.EMAIL, item.bucket.userEmail)
                            requireActivity().startActivity(this)
                        }
                    }
                }
            } else {
                // If only one element in the bucket
                if (item.bucket.nodes.size == 1) {
                    lifecycleScope.launch {
                        val node = item.bucket.nodes[0]
                        viewModel.getMegaNode(node.id.longValue)?.let {
                            openFile(position, it)
                        }
                    }
                }
                // If more element in the bucket
                else {
                    viewModel.select(item)
                    val currentDestination =
                        Navigation.findNavController(requireView()).currentDestination
                    if (currentDestination != null && currentDestination.id == R.id.homepageFragment) {
                        Navigation.findNavController(requireView())
                            .navigate(HomepageFragmentDirections.actionHomepageToRecentBucket(),
                                NavOptions.Builder().build())
                    }
                }
            }
        }

        adapter.setOnThreeDotsClickListener { node ->
            if (!Util.isOnline(context)) {
                (requireActivity() as ManagerActivity).showSnackbar(Constants.SNACKBAR_TYPE,
                    requireContext().getString(R.string.error_server_connection_problem), -1)
            } else {
                lifecycleScope.launch {
                    val megaNode = viewModel.getMegaNode(node.id.longValue)
                    (requireActivity() as ManagerActivity).showNodeOptionsPanel(megaNode,
                        NodeOptionsBottomSheetDialogFragment.RECENTS_MODE)
                }
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


    fun openFile(index: Int, node: MegaNode) {
        val intent: Intent
        if (MimeTypeList.typeForName(node.name).isImage) {
            intent = getIntentForSingleNode(
                requireContext(),
                node.handle,
                false
            )
            putThumbnailLocation(intent, listView, index, Constants.VIEWER_FROM_RECETS, adapter)
            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
            return
        }
        val localPath = FileUtil.getLocalFile(node)
        val paramsSetSuccessfully: Boolean
        if (FileUtil.isAudioOrVideo(node)) {
            intent = if (FileUtil.isInternalIntent(node)) {
                Util.getMediaIntent(requireContext(), node.name)
            } else {
                Intent(Intent.ACTION_VIEW)
            }
            intent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.RECENTS_ADAPTER)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, node.name)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_IS_PLAYLIST, false)
            paramsSetSuccessfully = if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(requireContext(), node, intent, localPath,
                    false, requireActivity() as ManagerActivity)
            } else {
                FileUtil.setStreamingIntentParams(requireContext(), node, megaApi, intent,
                    requireActivity() as ManagerActivity)
            }
            if (paramsSetSuccessfully && FileUtil.isOpusFile(node)) {
                intent.setDataAndType(intent.data, "audio/*")
            }
            launchIntent(intent, paramsSetSuccessfully, node, index)
        } else if (MimeTypeList.typeForName(node.name).isURL) {
            manageURLNode(requireActivity(), megaApi, node)
        } else if (MimeTypeList.typeForName(node.name).isPdf) {
            intent = Intent(requireContext(), PdfViewerActivity::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.RECENTS_ADAPTER)
            paramsSetSuccessfully = if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(requireContext(), node, intent, localPath,
                    false, requireActivity() as ManagerActivity)
            } else {
                FileUtil.setStreamingIntentParams(requireContext(), node, megaApi, intent,
                    requireActivity() as ManagerActivity)
            }
            launchIntent(intent, paramsSetSuccessfully, node, index)
        } else if (MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)) {
            manageTextFileIntent(requireContext(), node, Constants.RECENTS_ADAPTER)
        } else {
            Timber.d("itemClick:isFile:otherOption")
            onNodeTapped(requireActivity(),
                node,
                { n: MegaNode? -> (requireActivity() as ManagerActivity).saveNodeByTap(n) },
                (requireActivity() as ManagerActivity),
                (requireActivity() as ManagerActivity))
        }
    }

    /**
     * Launch corresponding intent to open the file based on its type.
     *
     * @param intent                Intent to launch activity.
     * @param paramsSetSuccessfully true, if the param is set for the intent successfully; false, otherwise.
     * @param node                  The node to open.
     * @param position              Thumbnail's position in the list.
     */
    private fun launchIntent(
        intent: Intent?,
        paramsSetSuccessfully: Boolean,
        node: MegaNode,
        position: Int,
    ) {
        if (intent != null && !MegaApiUtils.isIntentAvailable(requireContext(), intent)) {
            (requireActivity() as ManagerActivity).showSnackbar(Constants.SNACKBAR_TYPE,
                getString(R.string.intent_not_available),
                -1)
            return
        }
        if (intent != null && paramsSetSuccessfully) {
            intent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)
            putThumbnailLocation(intent,
                listView,
                position,
                Constants.VIEWER_FROM_RECETS,
                adapter)
            requireActivity().startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        }
    }
}
