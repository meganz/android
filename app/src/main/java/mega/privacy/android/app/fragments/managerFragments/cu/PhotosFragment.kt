package mega.privacy.android.app.fragments.managerFragments.cu

import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import mega.privacy.android.app.featuretoggle.PhotosFilterAndSortToggle
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosPagerAdapter.Companion.ALBUM_INDEX
import mega.privacy.android.app.fragments.managerFragments.cu.PhotosPagerAdapter.Companion.TIMELINE_INDEX
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.Util
import java.util.Locale

/**
 * PhotosFragment is a parent fragment for both TimelineFragment and AlbumsFragment
 */
class PhotosFragment : BaseFragment() {

    private lateinit var mManagerActivity: ManagerActivity

    private lateinit var binding: FragmentPhotosBinding

    private lateinit var tabLayout: TabLayout

    private lateinit var viewPager: ViewPager2

    private lateinit var actionBar: ActionBar

    private lateinit var actionBarSubtitle: TextView

    private lateinit var actionBarSubtitleArrow: ImageView

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
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPhotosBinding.inflate(inflater, container, false)
        actionBar = (context as AppCompatActivity).supportActionBar!!
        tabIndex = if (mManagerActivity.fromAlbumContent) {
            ALBUM_INDEX
        } else {
            savedInstanceState?.getInt(KEY_TAB_INDEX) ?: TIMELINE_INDEX
        }
        if (PhotosFilterAndSortToggle.enabled) {
            setCustomisedActionBar()
        }
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
                currentTab?.let { currentTab ->
                    checkScroll()

                    if (currentTab is TimelineFragment) {
                        tabIndex = TIMELINE_INDEX
                        val timelineFragment = currentTab
                        mManagerActivity.fromAlbumContent = false
                        if (PhotosFilterAndSortToggle.enabled) {
                            showHideABSubtitle(false)
                        }
                        with(timelineFragment) {
                            if (PhotosFilterAndSortToggle.enabled) {
                                setActionBarSubtitleText(Util.adjustForLargeFont(getCurrentFilter()))
                                actionBar.customView.findViewById<View>(R.id.ab_container)
                                    .setOnClickListener {
                                        createFilterDialog(mManagerActivity)
                                    }
                            }
                            setHideBottomViewScrollBehaviour()
                            updateOptionsButtons()
                            if (isEnablePhotosFragmentShown() || !gridAdapterHasData() || isInActionMode()) {
                                mManagerActivity.updateCUViewTypes(View.GONE)
                            } else {
                                mManagerActivity.updateCUViewTypes(View.VISIBLE)
                            }
                        }
                    } else {
                        tabIndex = ALBUM_INDEX
                        if (PhotosFilterAndSortToggle.enabled) {
                            showHideABSubtitle(false)
                        }
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
        viewPager.setCurrentItem(ALBUM_INDEX, false)
        mManagerActivity.updateCUViewTypes(View.GONE)
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
     * Enable Camera Upload
     */
    fun enableCameraUpload() {
        (currentTab as? TimelineFragment)?.enableCameraUpload()
    }

    /**
     * handle enable Camera Upload click UI and logic
     */
    fun enableCameraUploadClick() {
        if (currentTab !is TimelineFragment) {
            viewPager.postDelayed({
                viewPager.currentItem = TIMELINE_INDEX
                (currentTab as TimelineFragment).enableCameraUploadClick()
            }, 50)
        } else {
            (currentTab as TimelineFragment).enableCameraUploadClick()
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

    fun setCustomisedActionBar() {
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayShowTitleEnabled(false)
        actionBar.setCustomView(R.layout.fragment_timeline_action_bar)
        val v: View = actionBar.customView
        val actionBarTitle = v.findViewById<TextView>(R.id.ab_title)
        actionBarTitle?.text = Util.adjustForLargeFont(
            getString(R.string.settings_start_screen_photos_option).uppercase(Locale.getDefault())
        )
        actionBarSubtitle = v.findViewById(R.id.ab_subtitle)
        actionBarSubtitleArrow = v.findViewById(R.id.ab_subtitle_arrow)
    }

    fun setActionBarSubtitleText(text: SpannableString) {
        actionBarSubtitle.text = text
    }

    fun showHideABSubtitle(hide: Boolean) {
        actionBarSubtitle.visibility = if (hide) {
            View.GONE
        } else {
            View.VISIBLE
        }

        actionBarSubtitleArrow.visibility = if (hide) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
