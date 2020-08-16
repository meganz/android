package mega.privacy.android.app.fragments.photos

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import android.os.Handler
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
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Util
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
    lateinit var listAdapter: PhotosGridAdapter

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
        }
        refresh()
    }

    override fun refresh() {
        viewModel.loadPhotos(PhotoQuery(searchDate = LongArray(0)))
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
        observeSelectAll()
        observeAnimatedNodes()
        observeActionModeDestroy()
    }

    private fun setupNavigation() {
//        activity = getActivity() as ManagerActivityLollipop
//        activity.isFirstNavigationLevel = false

//        activity.supportActionBar?.apply {
//            title = resources.getString(R.string.category_photos)
//            setHomeAsUpIndicator(
//                Util.mutateIcon(
//                    context,
//                    R.drawable.ic_arrow_back_white,
//                    R.color.black
//                ))
//        }
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

    private fun observeSelectAll() =
        actionModeViewModel.selectAllEvent.observe(viewLifecycleOwner, Observer {
            viewModel.items.value?.run {
                actionModeViewModel.selectAll(filter { it.node != null })
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
                    viewHolder.itemView.findViewById<ImageView>(
                        R.id.icon_selected
                    )?.run {
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
        actionModeViewModel.actionModeDestroy.observe(viewLifecycleOwner, Observer {
            actionMode = null
        })

    private fun updateUi() {
        viewModel.items.value?.let { it ->
            listAdapter.submitList(ArrayList<PhotoNode>(it))
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
        listView.layoutManager?.apply {
            spanSizeLookup = listAdapter.getSpanSizeLookup(spanCount)
            val itemDimen =
                outMetrics.widthPixels / spanCount - resources.getDimension(R.dimen.photo_grid_margin)
                    .toInt() * 2
            listAdapter.setItemDimen(itemDimen)
        }

        listView.adapter = listAdapter
    }

    override fun shouldShowSearch(): Boolean {
        val size = viewModel.items.value?.size
        if (size != null) {
            return size > 0
        }

        return false
    }

    override fun searchReady() {
    }

    override fun exitSearch() {
    }
}