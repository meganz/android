package mega.privacy.android.app.fragments.homepage.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.image.ImageViewerActivity
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.MODE5
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*

@AndroidEntryPoint
class PhotosFragment : BaseFragment(), HomepageSearchable {

    private val viewModel by viewModels<PhotosViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()
    private val itemOperationViewModel by viewModels<ItemOperationViewModel>()

    private lateinit var binding: FragmentPhotosBinding

    private lateinit var listView: NewGridRecyclerView

    private lateinit var browseAdapter: PhotosBrowseAdapter
    private lateinit var searchAdapter: PhotosSearchAdapter

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var itemDecoration: SimpleDividerItemDecoration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewModel = this@PhotosFragment.viewModel
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

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                activity?.invalidateOptionsMenu()  // Hide the search icon if no photo
            }

            actionModeViewModel.setNodesData(it.filter { nodeItem -> nodeItem.type == PhotoNodeItem.TYPE_PHOTO })
        }

        observeDragSupportEvents(viewLifecycleOwner, listView, VIEWER_FROM_PHOTOS)
    }

    private fun setupEmptyHint() {
        binding.emptyHint.emptyHintImage.isVisible = false
        binding.emptyHint.emptyHintText.isVisible = false
        binding.emptyHint.emptyHintText.text =
            getString(R.string.homepage_empty_hint_photos).toUpperCase(Locale.ROOT)
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            val activity = activity as ManagerActivityLollipop

            activity.hideKeyboardSearch()  // Make the snack bar visible to the user
            activity.showSnackbar(
                SNACKBAR_TYPE,
                context.getString(R.string.error_server_connection_problem),
                MEGACHAT_INVALID_HANDLE
            )
        }
    }

    private fun setupNavigation() {
        itemOperationViewModel.openItemEvent.observe(viewLifecycleOwner, EventObserver {
            openPhoto(it as PhotoNodeItem)
        })

        itemOperationViewModel.showNodeItemOptionsEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline {
                (activity as ManagerActivityLollipop).showNodeOptionsPanel(
                    it.node,
                    MODE5
                )
            }
        })
    }

    /**
     * Only refresh the list items of uiDirty = true
     */
    private fun updateUi() = viewModel.items.value?.let { it ->
        val newList = ArrayList<PhotoNodeItem>(it)

        if (viewModel.searchMode) {
            searchAdapter.submitList(newList)
        } else {
            browseAdapter.submitList(newList)
        }
    }

    private fun elevateToolbarWhenScrolling() = ListenScrollChangesHelper().addViewToListen(
        listView
    ) { v: View?, _, _, _, _ ->
        (activity as ManagerActivityLollipop).changeAppBarElevation(v!!.canScrollVertically(-1))
    }

    private fun setupListView() {
        listView = binding.photoList
        listView.itemAnimator = noChangeRecyclerViewItemAnimator()
        elevateToolbarWhenScrolling()

        itemDecoration = SimpleDividerItemDecoration(context)
        if (viewModel.searchMode) listView.addItemDecoration(itemDecoration)

        listView.clipToPadding = false
        listView.setHasFixedSize(true)
    }

    private fun setupActionMode() {
        actionModeCallback =
            ActionModeCallback(activity as ManagerActivityLollipop, actionModeViewModel, megaApi)

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
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                actionModeCallback.nodeCount = viewModel.getRealPhotoCount()

                if (actionMode == null) {
                    (activity as ManagerActivityLollipop).hideKeyboardSearch()
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

        actionModeViewModel.animNodeIndices.observe(viewLifecycleOwner, {
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

                    val imageView = if (viewModel.searchMode) {
                        itemView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.new_multiselect_color
                            )
                        )
                        itemView.findViewById(R.id.thumbnail)
                    } else {
                        // Draw the green outline for the thumbnail view at once
                        val thumbnailView =
                            itemView.findViewById<SimpleDraweeView>(R.id.thumbnail)
                        thumbnailView.hierarchy.roundingParams = getRoundingParams(context)

                        itemView.findViewById<ImageView>(
                            R.id.icon_selected
                        )
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
            (activity as ManagerActivityLollipop).showKeyboardForSearch()
        })

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    private fun setupListAdapter() {
        browseAdapter = PhotosBrowseAdapter(actionModeViewModel, itemOperationViewModel)
        searchAdapter = PhotosSearchAdapter(actionModeViewModel, itemOperationViewModel)

        searchAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (!viewModel.skipNextAutoScroll) {
                    listView.layoutManager?.scrollToPosition(0)
                }
                viewModel.skipNextAutoScroll = false
            }
        })

        if (viewModel.searchMode) {
            listView.switchToLinear()
            listView.adapter = searchAdapter
        } else {
            configureGridLayoutManager()
            listView.adapter = browseAdapter
        }
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            RunOnUIThreadUtils.post { (activity as ManagerActivityLollipop).hideKeyboardSearch() }
        }

        if (viewModel.searchMode) return

        listView.switchToLinear()
        listView.adapter = searchAdapter
        listView.addItemDecoration(itemDecoration)

        viewModel.searchMode = true
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    override fun exitSearch() {
        if (!viewModel.searchMode) return

        listView.switchBackToGrid()
        configureGridLayoutManager()
        listView.adapter = browseAdapter
        listView.removeItemDecoration(itemDecoration)

        viewModel.searchMode = false
        viewModel.searchQuery = ""
        viewModel.refreshUi()
    }

    private fun configureGridLayoutManager() {
        if (listView.layoutManager !is CustomizedGridLayoutManager) return

        (listView.layoutManager as CustomizedGridLayoutManager).apply {
            spanSizeLookup = browseAdapter.getSpanSizeLookup(spanCount)
            val itemDimen =
                outMetrics.widthPixels / spanCount - resources.getDimension(R.dimen.photo_grid_margin)
                    .toInt() * 2
            browseAdapter.setItemDimen(itemDimen)
        }
    }

    override fun searchQuery(query: String) {
        if (viewModel.searchQuery == query) return

        viewModel.searchQuery = query
        viewModel.loadPhotos()
    }

    private fun openPhoto(nodeItem: PhotoNodeItem) {
        listView.findViewHolderForLayoutPosition(nodeItem.index)?.itemView?.findViewById<ImageView>(
            R.id.thumbnail
        )?.also {
//            val intent = Intent(context, FullScreenImageViewerLollipop::class.java)
            val intent = Intent(context, ImageViewerActivity::class.java)
//                .apply {
//                putExtra(INTENT_EXTRA_KEY_HANDLE, nodeItem.node?.handle ?: INVALID_HANDLE)
//                putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, megaApi.getParentNode(nodeItem.node).handle)
//                }

            intent.putExtra(INTENT_EXTRA_KEY_POSITION, nodeItem.photoIndex)
            intent.putExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                MegaApiJava.ORDER_MODIFICATION_DESC
            )

            if (viewModel.searchMode) {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_SEARCH_ADAPTER);
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_BROWSE_ADAPTER)
            }
            intent.putExtra(
                INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH,
                viewModel.getHandlesOfPhotos()
            )

            intent.putExtra(INTENT_EXTRA_KEY_HANDLE, nodeItem.node?.handle ?: INVALID_HANDLE)
            (listView.adapter as? DragThumbnailGetter)?.let {
                putThumbnailLocation(intent, listView, nodeItem.index, VIEWER_FROM_PHOTOS, it)
            }

            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.skipNextAutoScroll = true
    }
}
