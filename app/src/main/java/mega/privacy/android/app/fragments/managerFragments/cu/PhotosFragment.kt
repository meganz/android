package mega.privacy.android.app.fragments.managerFragments.cu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosPagerAdapter.Companion.ALBUM_INDEX
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosPagerAdapter.Companion.TIMELINE_INDEX
import mega.privacy.android.app.main.ManagerActivity

/**
 * PhotosFragment is a parent fragment for both TimelineFragment and AlbumsFragment
 */
class PhotosFragment : BaseFragment() {

    private lateinit var mManagerActivity: ManagerActivity

    private lateinit var binding: FragmentPhotosBinding

    private lateinit var tabLayout: TabLayout

    private lateinit var viewPager: ViewPager2

    var currentTab: PhotosTabCallback? = null

    var tabIndex = 0

    companion object {
        const val KEY_TAB_INDEX = "tab_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mManagerActivity = requireActivity() as ManagerActivity
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB_INDEX, tabIndex)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotosBinding.inflate(inflater, container, false)
        tabIndex = savedInstanceState?.getInt(KEY_TAB_INDEX) ?: TIMELINE_INDEX
        setupPhotosViewPager()
        return binding.root
    }

    /**
     * Set up ViewPager2
     */
    private fun setupPhotosViewPager() {
        viewPager = binding.viewPager
        val adapter = PhotosPagerAdapter(this)
        // By setting this will make BottomSheetPagerAdapter create all the fragments on initialization.
        viewPager.offscreenPageLimit = adapter.itemCount
        viewPager.adapter = adapter
        // sub fragment may create late.
        if (tabIndex != ALBUM_INDEX) {
            switchToTimeline()
        }

        // Attach the view pager to the tab layout
        tabLayout = binding.tabLayout
        val mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }
        mediator.attach()

        // Pass selected page view to HomepageBottomSheetBehavior which would seek for
        // the nested scrolling child views and deal with the logic of nested scrolling
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                currentTab =
                    childFragmentManager.findFragmentByTag("f$position") as? PhotosTabCallback
                currentTab?.let { currentTab->
                    checkScroll()

                    if (currentTab is TimelineFragment) {
                        tabIndex = TIMELINE_INDEX
                        val timelineFragment = currentTab
                        with(timelineFragment) {
                            setHideBottomViewScrollBehaviour()
                            if (isEnablePhotosFragmentShown() && gridAdapterHasData() && isInActionMode()) {
                                mManagerActivity.updateCUViewTypes(View.VISIBLE)
                            } else {
                                mManagerActivity.updateCUViewTypes(View.GONE)
                            }
                        }
                    } else {
                        tabIndex = ALBUM_INDEX
                        with(mManagerActivity) {
                            updateCUViewTypes(View.GONE)
                            enableHideBottomViewOnScroll(false)
                            showBottomView()
                        }
                    }
                }
            }
        })
    }

    /**
     * Get the title of the tabs in Photos fragment
     *
     * @param position the tab index
     * @return The title text or "" for invalid position param
     */
    private fun getTabTitle(position: Int): String {
        when (position) {
            TIMELINE_INDEX -> return resources.getString(R.string.tab_title_timeline)
            ALBUM_INDEX -> return resources.getString(R.string.tab_title_album)
        }

        return ""
    }

    /**
     * Switch to TimelineFragment
     */
    fun switchToTimeline() {
        viewPager.postDelayed({
            viewPager.currentItem = TIMELINE_INDEX
        }, 50)
    }

    /**
     * Switch to AlbumFragment
     */
    fun switchToAlbum() {
        viewPager.postDelayed({
            viewPager.currentItem = ALBUM_INDEX
            mManagerActivity.updateCUViewTypes(View.GONE)
        }, 50)
    }

    /**
     * Handle Back Press logic
     */
    fun onBackPressed() = currentTab?.onBackPressed() ?: 0

    /**
     * Handle titleBar Elevation. will call in checkScrollElevation
     */
    fun checkScroll() = currentTab?.checkScroll()

    /**
     * Enable CU
     */
    fun enableCu() {
        (currentTab as? TimelineFragment)?.enableCu()
    }

    /**
     * handle enable CU click UI and logic
     */
    fun enableCUClick() {
        if (currentTab !is TimelineFragment) {
            viewPager.postDelayed({
                viewPager.currentItem = TIMELINE_INDEX
                (currentTab as TimelineFragment).enableCUClick()
            }, 50)
        } else {
            (currentTab as TimelineFragment).enableCUClick()
        }
    }

    /**
     * Refresh view and layout after CU enabled or disabled.
     */
    fun refreshViewLayout() {
        (currentTab as? TimelineFragment)?.refreshViewLayout()
    }

    /**
     * handle Storage Permission when got refused
     */
    fun onStoragePermissionRefused() {
        (currentTab as? TimelineFragment)?.onStoragePermissionRefused()
    }

    /**
     * Check is enable PhotosFragment showing
     *
     * @return True, show it if in TimelineFragment, otherwise,falsem hide.
     */
    fun isEnablePhotosFragmentShown() = if (currentTab is TimelineFragment) {
        (currentTab as TimelineFragment).isEnablePhotosFragmentShown()
    } else {
        false
    }

    /**
     * Set default View for TimelineFragment
     */
    fun setDefaultView() {
        (currentTab as? TimelineFragment)?.setDefaultView()
    }

    /**
     * Check should show full info and options
     */
    fun shouldShowFullInfoAndOptions(): Boolean? {
        return if (currentTab is TimelineFragment) {
            (currentTab as? TimelineFragment)?.shouldShowFullInfoAndOptions()
        } else {
            false
        }
    }

    /**
     * update progress UI
     */
    fun updateProgress(visibility: Int, pending: Int) {
        (currentTab as? TimelineFragment)?.updateProgress(visibility, pending)
    }

    /**
     * Load Photos
     */
    fun loadPhotos() {
        (currentTab as? TimelineFragment)?.loadPhotos()
    }

    /**
     * Should show or hide tabLayout in Timeline
     *
     * @param isVisible true, show; false hide
     */
    fun shouldShowTabLayout(isVisible: Boolean) {
        binding.tabLayout.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * Should enable or disable viewPager2 in Timeline
     *
     * @param isEnabled true, enable; false disable
     */
    fun shouldEnableViewPager(isEnabled: Boolean) {
        binding.viewPager.isUserInputEnabled = isEnabled
    }
}