package mega.privacy.android.app.imageviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentImageViewerBinding
import mega.privacy.android.app.imageviewer.adapter.ImageViewerAdapter
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.utils.ContextUtils.isLowMemory
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import timber.log.Timber

@AndroidEntryPoint
class ImageViewerFragment : Fragment() {

    private lateinit var binding: FragmentImageViewerBinding

    private var shouldReportPosition = false
    private val viewModel by activityViewModels<ImageViewerViewModel>()
    private val pagerAdapter by lazy {
        ImageViewerAdapter(true, childFragmentManager, viewLifecycleOwner.lifecycle)
    }

    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (shouldReportPosition) viewModel.updateCurrentImage(position, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentImageViewerBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        if (!shouldReportPosition && pagerAdapter.itemCount > 0 && viewModel.getImagesSize(false) > 0) {
            val currentPosition = viewModel.getCurrentPosition(false)
            binding.viewPager.setCurrentItem(currentPosition, false)
            shouldReportPosition = true
        }
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_image_viewer, menu)
    }

    override fun onLowMemory() {
        if (binding.viewPager.offscreenPageLimit != ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT) {
            binding.viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        }
        super.onLowMemory()
    }

    override fun onStop() {
        shouldReportPosition = false
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onStop()
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        super.onDestroyView()
    }

    private fun setupView() {
        binding.viewPager.apply {
            isSaveEnabled = false
            offscreenPageLimit = if (requireContext().isLowMemory()) {
                ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
            } else {
                ImageViewerActivity.IMAGE_OFFSCREEN_PAGE_LIMIT
            }
            setPageTransformer(MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.image_viewer_pager_margin)))
            adapter = pagerAdapter
        }

        binding.motion.post { binding.motion.transitionToEnd() }
    }

    private fun setupObservers() {
        viewModel.onAdapterImages(false).observe(viewLifecycleOwner) { items ->
            if (items.isNullOrEmpty()) {
                Timber.e("Null or empty image items")
                activity?.finish()
            } else {
                pagerAdapter.submitList(items) {
                    binding.progress.hide()

                    val currentPosition = viewModel.getCurrentPosition(false)
                    if (!shouldReportPosition) {
                        binding.viewPager.setCurrentItem(currentPosition, false)
                        binding.viewPager.waitForLayout {
                            shouldReportPosition = true
                            true
                        }
                    }

                    val imagesSize = items.size
                    binding.txtPageCount.apply {
                        text = StringResourcesUtils.getString(
                            R.string.wizard_steps_indicator,
                            currentPosition + 1,
                            imagesSize
                        )
                        isVisible = imagesSize > 1
                    }
                }
            }
        }

        viewModel.onCurrentImageItem().observe(viewLifecycleOwner, ::showCurrentImageInfo)
        viewModel.onShowToolbar().observe(viewLifecycleOwner, ::changeBottomBarVisibility)
    }

    /**
     * Populate current image information to bottom texts and toolbar options.
     *
     * @param imageItem  Image item to show
     */
    private fun showCurrentImageInfo(imageItem: ImageItem?) {
        val imagesSize = viewModel.getImagesSize(false)
        val position = viewModel.getCurrentPosition(false)
        binding.txtTitle.text = imageItem?.name
        binding.txtPageCount.apply {
            text = StringResourcesUtils.getString(
                R.string.wizard_steps_indicator,
                position + 1,
                imagesSize
            )
            isVisible = imagesSize > 1
        }

        activity?.invalidateOptionsMenu()
    }

    /**
     * Change bottomBar visibility with animation.
     *
     * @param show                  Show or hide toolbar/bottombar
     * @param enableTransparency    Enable transparency change
     */
    private fun changeBottomBarVisibility(show: Boolean, enableTransparency: Boolean = false) {
        binding.motion.post {
            val color: Int
            if (show) {
                color = R.color.white_black
                binding.motion.transitionToEnd()
            } else {
                color = android.R.color.black
                binding.motion.transitionToStart()
            }
            binding.motion.setBackgroundColor(ContextCompat.getColor(requireContext(),
                if (enableTransparency && !show) {
                    android.R.color.transparent
                } else {
                    color
                }))
        }
    }
}
