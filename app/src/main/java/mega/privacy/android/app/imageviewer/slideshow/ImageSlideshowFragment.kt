package mega.privacy.android.app.imageviewer.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import mega.privacy.android.app.databinding.FragmentImageSlideshowBinding
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.imageviewer.ImageViewerViewModel
import mega.privacy.android.app.imageviewer.adapter.ImageViewerAdapter
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.STARTED
import mega.privacy.android.app.imageviewer.slideshow.ImageSlideshowState.STOPPED
import mega.privacy.android.app.utils.ContextUtils.isLowMemory
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import timber.log.Timber

@AndroidEntryPoint
class ImageSlideshowFragment : Fragment() {

    private lateinit var binding: FragmentImageSlideshowBinding

    private var shouldReportPosition = false
    private val viewModel by activityViewModels<ImageViewerViewModel>()
    private val pagerAdapter by lazy {
        ImageViewerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
    }

    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (shouldReportPosition) viewModel.updateCurrentPosition(position, false)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentImageSlideshowBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_image_slideshow, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_options -> {
                // do something
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onLowMemory() {
        if (binding.viewPager.offscreenPageLimit != ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT) {
            binding.viewPager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        }
        super.onLowMemory()
    }

    override fun onStop() {
        viewModel.stopSlideshow()
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

        binding.btnPlay.setOnClickListener { viewModel.startSlideshow() }
        binding.btnPause.setOnClickListener { viewModel.stopSlideshow() }
        binding.motion.post { binding.motion.transitionToEnd() }
    }

    private fun setupObservers() {
        viewModel.onImagesIds().observe(viewLifecycleOwner) { items ->
            if (items.isNullOrEmpty()) {
                Timber.e("Null or empty image items")
                activity?.finish()
            } else {
                pagerAdapter.submitList(items) {
                    binding.progress.hide()

                    if (!shouldReportPosition) {
                        binding.viewPager.setCurrentItem(viewModel.getCurrentPosition(), false)
                        binding.viewPager.waitForLayout {
                            viewModel.onCurrentPosition().observe(viewLifecycleOwner, ::updateCurrentPosition)
                            shouldReportPosition = true
                            true
                        }
                    }
                }
            }
        }

        viewModel.onSlideshowState().observe(viewLifecycleOwner, ::updateSlideshowButtons)
        viewModel.onShowToolbar().observe(viewLifecycleOwner, ::changeBottomBarVisibility)
    }

    private fun updateCurrentPosition(newPosition: Int) {
        binding.viewPager.setCurrentItem(newPosition, true)
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

    private fun updateSlideshowButtons(state: ImageSlideshowState) {
        when (state) {
            STARTED -> {
                binding.btnPlay.isVisible = false
                binding.btnPause.isVisible = true
            }
            STOPPED -> {
                binding.btnPause.isVisible = false
                binding.btnPlay.isVisible = true
            }
        }
    }
}
