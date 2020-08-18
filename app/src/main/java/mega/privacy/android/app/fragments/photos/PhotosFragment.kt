package mega.privacy.android.app.fragments.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridLayoutManager
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : BaseFragment(), HomepageSearchable, HomepageRefreshable {

    @Inject
    lateinit var viewModel: PhotosViewModel

    @Inject
    lateinit var actionModeViewModel: ActionModeViewModel

    private lateinit var binding: FragmentPhotosBinding
    private lateinit var listView: NewGridRecyclerView

    @Inject
    lateinit var browseAdapter: PhotosBrowseAdapter

    @Inject
    lateinit var searchAdapter: PhotosSearchAdapter

    private var actionMode: ActionMode? = null

    @Inject
    lateinit var actionModeCallback: ActionModeCallback

    private lateinit var activity: ManagerActivityLollipop

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewModel = this@PhotosFragment.viewModel
            Log.i("Alex", "viewModel:$viewModel")
            actionModeViewModel = this@PhotosFragment.actionModeViewModel
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
        Log.i("Alex", "viewmodel:$viewModel")

        viewModel.items.observe(viewLifecycleOwner) {
            activity.invalidateOptionsMenu()
            actionModeViewModel.setNodesData(it.filter { node -> node.type == PhotoNode.TYPE_PHOTO })
        }

        refresh()
    }

    private fun setupNavigation() {
        viewModel.openPhotoEvent.observe(viewLifecycleOwner, EventObserver {
            openPhoto(it)
        })
    }

    private fun openPhoto(node: PhotoNode) {
        listView.findViewHolderForLayoutPosition(node.index)?.itemView?.findViewById<ImageView>(
            R.id.thumbnail
        )?.also {
            val topLeft = IntArray(2)
            it.getLocationOnScreen(topLeft)
            val intent = Intent(context, FullScreenImageViewerLollipop::class.java)
            val thumbnailLocation = arrayOf(topLeft[0], topLeft[1], it.width, it.height)

            intent.putExtra(INTENT_EXTRA_KEY_POSITION, node.photoIndex)
            intent.putExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                MegaApiJava.ORDER_MODIFICATION_DESC
            )

            val parentNode = megaApi.getParentNode(node.node)
            if (parentNode == null || parentNode.type == MegaNode.TYPE_ROOT) {
                intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, INVALID_HANDLE);
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, parentNode.handle);
            }

            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, PHOTOS_BROWSE_ADAPTER)
            intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, thumbnailLocation);

            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
        }
    }

    override fun refresh() {
        Log.i("Alex", "refresh")
//        viewModel.loadPhotos(PhotoQuery(searchDate = LongArray(0)))
        viewModel.loadPhotos()
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
        listView = binding.photoList
        preventListItemBlink()
        elevateToolbarWhenScrolling()
    }

    private fun setupActionMode() {
        observeSelectedNodes()
        observeAnimatedNodes()
        observeActionModeDestroy()
    }

    private fun observeSelectedNodes() =
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                }
            } else {
                actionModeCallback.nodeCount = getRealNodeCount()

                if (actionMode == null) {
                    actionMode = (activity as AppCompatActivity).startSupportActionMode(
                        actionModeCallback
                    )
                } else {
                    actionMode?.invalidate()  // Update the action items based on the selected nodes
                }

                actionMode?.title = it.size.toString()
            }
        })

    private fun observeAnimatedNodes() {
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
                    // Refresh the Ui here is necessary. The reason is certain cache mechanism of
                    // RecyclerView would cause a couple of selected icons failed to be updated even
                    // though listView.setItemViewCacheSize(0) (some ItemViews just out of
                    // the screen are already generated but get ViewHolders return null,
                    // and their bind() wouldn't be invoked via scrolling). Plus adding round corner
                    // for thumbnails
                    updateUi()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val imageView = if (viewModel.searchMode) {
                            viewHolder.itemView.findViewById(R.id.thumbnail)
                        } else {
                            viewHolder.itemView.findViewById<ImageView>(
                                R.id.icon_selected
                            )
                        }

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
        })

    private fun updateUi() {
        Log.i("Alex", "updateUi")
        val e = Exception()
        e.printStackTrace()
        viewModel.items.value?.let { it ->
            val newList = ArrayList<PhotoNode>(it)
            if (viewModel.searchMode) {
//                searchAdapter.submitList(newList.filter { it.type == PhotoNode.TYPE_PHOTO })
                searchAdapter.submitList(newList)
            } else {
                browseAdapter.submitList(newList)
            }
        }
    }

    private fun getRealNodeCount(): Int {
        viewModel.items.value?.filter { it.type == PhotoNode.TYPE_PHOTO }?.let {
            return it.size
        }

        return 0
    }

    private fun setupFastScroller() {
        binding.scroller.setRecyclerView(listView)
    }

    private fun setupListAdapter() {
        if (viewModel.searchMode) {
            listView.switchToLinear()
            listView.adapter = searchAdapter
        } else {
            configureGridLayoutManager()
            listView.adapter = browseAdapter
        }
    }

    override fun shouldShowSearch(): Boolean {
        val size = viewModel.items.value?.size
        if (size != null) {
            return size > 0
        }

        return false
    }

    override fun searchReady() {
        if (viewModel.searchMode) return

        viewModel.searchMode = true
        listView.switchToLinear()
        listView.adapter = searchAdapter
        updateUi()
    }

    override fun exitSearch() {
        if (!viewModel.searchMode) return

        viewModel.searchMode = false
        listView.switchBackToGrid()
        configureGridLayoutManager()
        listView.adapter = browseAdapter
        updateUi()
    }

    private fun configureGridLayoutManager() {
        if (listView.layoutManager !is CustomizedGridLayoutManager) return

        listView.layoutManager?.apply {
            spanSizeLookup = browseAdapter.getSpanSizeLookup(spanCount)
            val itemDimen =
                outMetrics.widthPixels / spanCount - resources.getDimension(R.dimen.photo_grid_margin)
                    .toInt() * 2
            browseAdapter.setItemDimen(itemDimen)
        }
    }
}