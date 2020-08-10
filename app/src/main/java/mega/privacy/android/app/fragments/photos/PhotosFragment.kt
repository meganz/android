package mega.privacy.android.app.fragments.photos

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.managerFragments.cu.CuItemSizeConfig
import mega.privacy.android.app.utils.Util
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : BaseFragment() {
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
    lateinit var actionModeCallback: HomepageActionModeCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewModel = this@PhotosFragment.viewModel
            actionModeViewModel = this@PhotosFragment.actionModeViewModel
        }

        listView = binding.photoList

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        setupListAdapter()
        setupFastScroller()
        setupNavigation()
        setupActionMode()
        Log.i("Alex", "viewmodel:$viewModel")

        viewModel.loadPhotos(PhotoQuery(searchDate = LongArray(0)))
    }

    private fun setupActionMode() {
        actionModeViewModel.selectedNodes.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                actionMode?.apply {
                    finish()
                    actionMode = null
                }
            } else {
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

        actionModeViewModel.selectAllEvent.observe(viewLifecycleOwner, Observer {
            viewModel.items.value?.run {
                actionModeViewModel.selectAll(filter {it.node != null})
            }
        })
    }

    private fun setupFastScroller() {
        binding.scroller.setRecyclerView(listView)
    }

    private fun setupNavigation() {
    }

    private fun setupListAdapter() {
        listView.layoutManager?.apply {
            spanSizeLookup = listAdapter.getSpanSizeLookup(spanCount)
            val itemSizeConfig = getItemSizeConfig(spanCount)
            listAdapter.setItemSizeConfig(itemSizeConfig)
        }

        listView.adapter = listAdapter
    }

    private fun getItemSizeConfig(spanCount: Int): CuItemSizeConfig {
        val gridMargin = resources.getDimension(R.dimen.photo_grid_margin).toInt()
        val gridWidth = outMetrics.widthPixels / spanCount - gridMargin * 2
        val selectedIconWidth = Util.dp2px(
            resources.getDimension(R.dimen.photo_selected_icon_width),
            outMetrics
        )
        val selectedIconMargin =
            Util.dp2px(resources.getDimension(R.dimen.photo_selected_icon_margin), outMetrics)
        val roundCornerRadius = Util.dp2px(
            resources.getDimension(R.dimen.photo_selected_icon_round_corner_radius),
            outMetrics
        )
        val selectedPadding =
            Util.dp2px(resources.getDimension(R.dimen.photo_selected_icon_padding), outMetrics)

        return CuItemSizeConfig(
            false, gridWidth, gridMargin, selectedIconWidth, selectedIconMargin,
            roundCornerRadius,
            selectedPadding
        )
    }
}