package mega.privacy.android.app.fragments.photos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.managerFragments.cu.CuItemSizeConfig
import mega.privacy.android.app.utils.Util
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : BaseFragment() {

    private val viewModel by viewModels<PhotosViewModel>()
    private lateinit var binding: FragmentPhotosBinding

    @Inject
    lateinit var listAdapter: PhotosGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        setupListAdapter()
        setupNavigation()

        viewModel.loadPhotos(PhotoQuery(searchDate = LongArray(0)))
    }

    private fun setupNavigation() {
    }

    private fun setupListAdapter() {
        binding.photoList.layoutManager?.apply {
            spanSizeLookup = listAdapter.getSpanSizeLookup(spanCount)
            val itemSizeConfig = getItemSizeConfig(spanCount)
            listAdapter.setItemSizeConfig(itemSizeConfig)
        }

        binding.photoList.adapter = listAdapter
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