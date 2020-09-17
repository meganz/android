package mega.privacy.android.app.fragments.homepage.documents

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentDocumentsBinding
import mega.privacy.android.app.fragments.homepage.ActionModeCallback
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.NodeGridAdapter
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.NodeListAdapter
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.getLocationAndDimen
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.MODE1
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.MODE5
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.DOCUMENTS_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.DOCUMENTS_SEARCH_ADAPTER
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.DraggingThumbnailCallback
import mega.privacy.android.app.utils.FileUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.app.utils.displayMetrics
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DocumentsFragment : Fragment(), HomepageSearchable {

    private val viewModel by viewModels<DocumentsViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    private lateinit var binding: FragmentDocumentsBinding
    private lateinit var listView: NewGridRecyclerView
    private lateinit var listAdapter: NodeListAdapter
    private lateinit var gridAdapter: NodeGridAdapter
    private lateinit var itemDecoration: PositionDividerItemDecoration

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    @Inject lateinit var megaApi: MegaApiAndroid

    private var openingNodeHandle = INVALID_HANDLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDocumentsBinding.inflate(inflater, container, false).apply {
            viewModel = this@DocumentsFragment.viewModel
            sortByHeaderViewModel = this@DocumentsFragment.sortByHeaderViewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        setupEmptyHint()
        setupListView()
        setupListAdapter()
        setupFastScroller()
        setupActionMode()
        setupNavigation()
        setupDraggingThumbnailCallback()

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                callManager { manager ->
                    manager.invalidateOptionsMenu()  // Hide the search icon if no file
                }
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.node != null })
        }
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
            getString(R.string.homepage_empty_hint_documents).toUpperCase(Locale.ROOT)
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            callManager {
                it.hideKeyboardSearch()  // Make the snack bar visible to the user
                it.showSnackbar(
                    SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    -1
                )
            }
        }
    }

    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            openDoc(it.node, it.index)
        })

        itemOperationViewModel.showNodeItemOptionsEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline {
                callManager { manager ->
                    manager.showNodeOptionsPanel(
                        it.node,
                        if (viewModel.searchMode) MODE5 else MODE1
                    )
                }
            }
        })

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel()
            }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.loadDocuments(true, it)
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(
            viewLifecycleOwner,
            EventObserver { isList ->
                switchListGridView(isList)
                viewModel.refreshUi()
            })
    }

    private fun switchListGridView(isList: Boolean) {
        if (isList) {
            listView.switchToLinear()
            listView.adapter = listAdapter
            if (listView.itemDecorationCount == 0) {
                listView.addItemDecoration(itemDecoration)
            }
        } else {
            listView.switchBackToGrid()
            listView.adapter = gridAdapter
            listView.removeItemDecoration(itemDecoration)

            (listView.layoutManager as CustomizedGridLayoutManager).apply {
                spanSizeLookup = gridAdapter.getSpanSizeLookup(spanCount)
            }
        }
    }

    /**
     * Only refresh the list items of uiDirty = true
     */
    private fun updateUi() = viewModel.items.value?.let { it ->
        val newList = ArrayList<NodeItem>(it)
        if (sortByHeaderViewModel.isList) {
            listAdapter.submitList(newList)
        } else {
            gridAdapter.submitList(newList)
        }
    }

    private fun preventListItemBlink() {
        val animator = listView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun elevateToolbarWhenScrolling() = ListenScrollChangesHelper().addViewToListen(
        listView
    ) { v: View?, _, _, _, _ ->
        callManager { manager ->
            manager.changeActionBarElevation(v!!.canScrollVertically(-1))
        }
    }

    private fun setupListView() {
        listView = binding.documentList
        preventListItemBlink()
        elevateToolbarWhenScrolling()
        itemDecoration = PositionDividerItemDecoration(context, displayMetrics())

        listView.clipToPadding = false
        listView.setHasFixedSize(true)
    }

    private fun setupActionMode() {
        actionModeCallback = ActionModeCallback(
            requireActivity() as ManagerActivityLollipop, actionModeViewModel, megaApi
        )

        observeItemLongClick()
        observeSelectedItems()
        observeAnimatedItems()
        observeActionModeDestroy()
    }

    private fun observeItemLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            doIfOnline { actionModeViewModel.enterActionMode(it) }
        })

    private fun observeSelectedItems() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                actionModeCallback.nodeCount = viewModel.getRealNodeCount()

                if (actionMode == null) {
                    callManager { manager ->
                        manager.hideKeyboardSearch()
                    }
                    actionMode = (activity as AppCompatActivity).startSupportActionMode(
                        actionModeCallback
                    )
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        })

    private fun observeAnimatedItems() {
        var animatorSet: AnimatorSet? = null

        actionModeViewModel.animNodeIndices.observe(viewLifecycleOwner, Observer {
            animatorSet?.run {
                // End the started animation if any, or the view may show messy as its property
                // would be wrongly changed by multiple animations running at the same time
                // via contiguous quick clicks on the item
                if (isStarted) {
                    end()
                }
            }

            // Must create a new AnimatorSet, or it would keep all previous
            // animation and play them together
            animatorSet = AnimatorSet()
            val animatorList = mutableListOf<Animator>()

            animatorSet?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    updateUi()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView

                    val imageView: ImageView? = if (sortByHeaderViewModel.isList) {
                        if (listAdapter.getItemViewType(pos) != NodeListAdapter.TYPE_HEADER) {
                            itemView.setBackgroundColor(resources.getColor(R.color.new_multiselect_color))
                        }
                        itemView.findViewById(R.id.thumbnail)
                    } else {
                        if (gridAdapter.getItemViewType(pos) != NodeGridAdapter.TYPE_HEADER) {
                            itemView.setBackgroundResource(R.drawable.background_item_grid_selected)
                        }
                        itemView.findViewById(R.id.ic_selected)
                    }

                    imageView?.run {
                        setImageResource(R.drawable.ic_select_folder)
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(context, R.animator.icon_select)
                        animator.setTarget(this)
                        animatorList.add(animator)
                    }
                }
            }

            animatorSet?.playTogether(animatorList)
            animatorSet?.start()
        })
    }

    private fun observeActionModeDestroy() =
        actionModeViewModel.actionModeDestroy.observe(viewLifecycleOwner, EventObserver {
            actionMode = null
            callManager { manager ->
                manager.showKeyboardForSearch()
            }
        })

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    private fun setupListAdapter() {
        listAdapter =
            NodeListAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel)
        listAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                listView.linearLayoutManager?.scrollToPosition(0)
            }
        })

        gridAdapter =
            NodeGridAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel)
        gridAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                listView.layoutManager?.scrollToPosition(0)
            }
        })

        switchListGridView(sortByHeaderViewModel.isList)
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            Handler().post { callManager { it.hideKeyboardSearch() } }
        }
        if (viewModel.searchMode) return

        viewModel.searchMode = true
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    override fun exitSearch() {
        if (!viewModel.searchMode) return

        viewModel.searchMode = false
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    override fun searchQuery(query: String) {
        if (viewModel.searchQuery == query) return
        viewModel.searchQuery = query
        viewModel.loadDocuments()
    }

    /** All below methods are for supporting functions of PdfViewerActivityLollipop */

    private fun getOpeningThumbnailLocationOnScreen(): IntArray? {
        val position = viewModel.getNodePositionByHandle(openingNodeHandle)
        val viewHolder = listView.findViewHolderForLayoutPosition(position) ?: return null
        val thumbnailView = viewHolder.itemView.findViewById<ImageView>(R.id.thumbnail)
        return thumbnailView.getLocationAndDimen()
    }

    private fun setupDraggingThumbnailCallback() =
        PdfViewerActivityLollipop.addDraggingThumbnailCallback(
            DocumentsFragment::class.java, DocumentsDraggingThumbnailCallback(WeakReference(this))
        )

    private fun openDoc(node: MegaNode?, index: Int) {
        if (node == null) {
            return
        }
        var screenPosition: IntArray? = null

        val localPath = FileUtils.getLocalFile(context, node.name, node.size)

        listView.findViewHolderForLayoutPosition(index)?.itemView?.findViewById<ImageView>(
            R.id.thumbnail
        )?.let {
            screenPosition = it.getLocationAndDimen()
        }

        if (MimeTypeList.typeForName(node.name).isPdf) {
            val intent = Intent(context, PdfViewerActivityLollipop::class.java)
            intent.putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            if (viewModel.searchMode) {
                intent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, DOCUMENTS_SEARCH_ADAPTER)
            } else {
                intent.putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, DOCUMENTS_BROWSE_ADAPTER)
            }
            if (screenPosition != null) {
                intent.putExtra(Constants.INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
            }

            val paramsSetSuccessfully =
                if (FileUtils.isLocalFile(context, node, megaApi, localPath)) {
                    FileUtils.setLocalIntentParams(activity, node, intent, localPath, false)
                } else {
                    FileUtils.setStreamingIntentParams(activity, node, megaApi, intent)
                }

            intent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.handle)

            if (paramsSetSuccessfully) {
                openingNodeHandle = node.handle
                setupDraggingThumbnailCallback()
                startActivity(intent)
                requireActivity().overridePendingTransition(0, 0)
                return
            }
        }

        NodeController(context).prepareForDownload(arrayListOf(node.handle), true)
    }

    companion object {
        private class DocumentsDraggingThumbnailCallback(
            private val fragmentRef: WeakReference<DocumentsFragment>
        ) : DraggingThumbnailCallback {

            override fun setVisibility(visibility: Int) {
            }

            override fun getLocationOnScreen(location: IntArray) {
                val fragment = fragmentRef.get() ?: return
                val result = fragment.getOpeningThumbnailLocationOnScreen() ?: return
                result.copyInto(location, 0, 0, 2)
            }
        }
    }
}
