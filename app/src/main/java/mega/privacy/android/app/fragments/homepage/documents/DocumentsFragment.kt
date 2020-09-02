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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.facebook.drawee.view.SimpleDraweeView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.databinding.FragmentDocumentsBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.photos.ActionModeCallback
import mega.privacy.android.app.fragments.homepage.photos.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.photos.DividerDecoration
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference

@AndroidEntryPoint
class DocumentsFragment : BaseFragment(), HomepageSearchable {

    private val viewModel by viewModels<DocumentsViewModel>()
    private val actionModeViewModel by viewModels<ActionModeViewModel>()

    private lateinit var binding: FragmentDocumentsBinding

    private lateinit var listView: NewGridRecyclerView

    private lateinit var adapter: DocumentsAdapter

    private var actionMode: ActionMode? = null
    private lateinit var actionModeCallback: ActionModeCallback

    private lateinit var activity: ManagerActivityLollipop

    private var draggingPhotoHandle = INVALID_HANDLE

    private lateinit var itemDecoration: DividerDecoration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDocumentsBinding.inflate(inflater, container, false).apply {
            viewModel = this@DocumentsFragment.viewModel
            actionModeViewModel = this@DocumentsFragment.actionModeViewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        activity = getActivity() as ManagerActivityLollipop

        setupListView()
        setupListAdapter()
        setupFastScroller()
        setupActionMode()
        setupNavigation()

        viewModel.items.observe(viewLifecycleOwner) {
            if (!viewModel.searchMode) {
                activity.invalidateOptionsMenu()  // Hide the search icon if no photo
            }

            actionModeViewModel.setNodesData(it)
        }
    }

    private fun doIfOnline(operation: () -> Unit) {
        if (Util.isOnline(context)) {
            operation()
        } else {
            activity.hideKeyboardSearch()  // Make the snack bar visible to the user
            activity.showSnackbar(
                SNACKBAR_TYPE,
                context.getString(R.string.error_server_connection_problem),
                -1
            )
        }
    }

    private fun setupNavigation() {
        viewModel.openPhotoEvent.observe(viewLifecycleOwner, EventObserver {
//            openPhoto(it)
        })

        viewModel.showNodeItemOptionsEvent.observe(viewLifecycleOwner, EventObserver {
            doIfOnline { activity.showNodeOptionsPanel(it.node) }
        })
    }

    /**
     * Only refresh the list items of uiDirty = true
     */
    private fun updateUi() = viewModel.items.value?.let { it ->
        val newList = ArrayList<NodeItem>(it)
        adapter.submitList(newList)
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
        activity.changeActionBarElevation(v!!.canScrollVertically(-1))
    }

    private fun setupListView() {
        listView = binding.documentList
        listView.switchToLinear()
        preventListItemBlink()
        elevateToolbarWhenScrolling()
        itemDecoration = DividerDecoration(context)
        if (viewModel.searchMode) listView.addItemDecoration(itemDecoration)
    }

    private fun setupActionMode() {
        actionModeCallback = ActionModeCallback(context, actionModeViewModel, megaApi)

        observePhotoLongClick()
        observeSelectedPhotos()
        observeAnimatedPhotos()
        observeActionModeDestroy()
    }

    private fun observePhotoLongClick() =
        actionModeViewModel.longClick.observe(viewLifecycleOwner, EventObserver {
            doIfOnline { actionModeViewModel.enterActionMode(it) }
        })

    private fun observeSelectedPhotos() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                viewModel.items.value?.let { items ->
                    actionModeCallback.nodeCount = items.size
                }

                if (actionMode == null) {
                    activity.hideKeyboardSearch()
                    actionMode = (activity as AppCompatActivity).startSupportActionMode(
                        actionModeCallback
                    )
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        })

    private fun observeAnimatedPhotos() {
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

//                    val imageView = if (viewModel.listMode) {
                        itemView.setBackgroundColor(resources.getColor(R.color.new_multiselect_color))
                        val imageView = itemView.findViewById<ImageView>(R.id.thumbnail)
//                    } else {
//                        // Draw the green outline for the thumbnail view at once
//                        val thumbnailView =
//                            itemView.findViewById<SimpleDraweeView>(R.id.thumbnail)
//                        thumbnailView.hierarchy.roundingParams = getRoundingParams(context)
//
//                        itemView.findViewById<ImageView>(
//                            R.id.icon_selected
//                        )
//                    }

                    imageView?.run {
                        setImageResource(R.drawable.ic_select_folder)
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(context, R.animator.photo_select)
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
            activity.showKeyboardForSearch()
        })

    private fun setupFastScroller() = binding.scroller.setRecyclerView(listView)

    private fun setupListAdapter() {
        adapter = DocumentsAdapter(viewModel, actionModeViewModel)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                listView.linearLayoutManager?.scrollToPosition(0)
            }
        })

        listView.adapter = adapter
    }

    override fun shouldShowSearchMenu(): Boolean = viewModel.shouldShowSearchMenu()

    override fun searchReady() {
        // Rotate screen in action mode, the keyboard would pop up again, hide it
        if (actionMode != null) {
            Handler().post { activity.hideKeyboardSearch() }
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

//    private fun configureGridLayoutManager() {
//        if (listView.layoutManager !is CustomizedGridLayoutManager) return
//
//        (listView.layoutManager as CustomizedGridLayoutManager).apply {
//            spanSizeLookup = adapter.getSpanSizeLookup(spanCount)
//            val itemDimen =
//                outMetrics.widthPixels / spanCount - resources.getDimension(R.dimen.photo_grid_margin)
//                    .toInt() * 2
//            adapter.setItemDimen(itemDimen)
//        }
//    }

    override fun searchQuery(query: String) {
        if (viewModel.searchQuery == query) return
        viewModel.searchQuery = query
        viewModel.loadPhotos()
    }
}