package mega.privacy.android.app.fragments.homepage.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.View.OnClickListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.zhpan.bannerview.BannerViewPager
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.constants.IndicatorGravity
import com.zhpan.bannerview.utils.BannerUtils
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_homepage.*
import kotlinx.android.synthetic.main.fragment_homepage.view.*
import kotlinx.android.synthetic.main.homepage_fabs.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.databinding.FabMaskLayoutBinding
import mega.privacy.android.app.databinding.FragmentHomepageBinding
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.fragments.homepage.banner.BannerViewHolder
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaBanner
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

@AndroidEntryPoint
class HomepageFragment : Fragment() {

    private val viewModel: HomePageViewModel by viewModels()

    private lateinit var viewDataBinding: FragmentHomepageBinding

    /** Shorthand for viewDataBinding.root */
    private lateinit var rootView: View

    private lateinit var bottomSheetBehavior: HomepageBottomSheetBehavior<View>
    private lateinit var searchInputView: FloatingSearchView
    private lateinit var bannerViewPager: BannerViewPager<MegaBanner, BannerViewHolder>

    /** The fab button in normal state */
    private lateinit var fabMain: FloatingActionButton

    /** The main fab button in expanded state */
    private lateinit var fabMaskMain: FloatingActionButton

    /** The layout for showing the full screen grey mask at FAB expanded state */
    private lateinit var fabMaskLayout: View

    /** The view pager in the bottom sheet, containing 2 pages: Recents and Offline */
    private lateinit var viewPager: ViewPager2

    /** The tab layout in the bottom sheet, associated with the view pager */
    private lateinit var tabLayout: TabLayout

    private var currentSelectedTabFragment: Fragment? = null

    /** The list of all sub views of the TabLayout*/
    private val tabsChildren = ArrayList<View>()

    private var windowContent: ViewGroup? = null

    var isFabExpanded = false

    /** The broadcast receiver for network connectivity.
     *  Switch the UI appearance between offline and online status
     */
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            when (intent.getIntExtra(INTENT_EXTRA_KEY_ACTION_TYPE, -1)) {
                GO_OFFLINE -> showOfflineMode()
                GO_ONLINE -> showOnlineMode()
            }
        }
    }

    /** The click listener for clicking on the file category buttons.
     *  Clicking to navigate to corresponding fragments */
    private val categoryClickListener = OnClickListener {
        with(viewDataBinding.category) {
            val direction = when (it) {
                categoryPhoto -> HomepageFragmentDirections.actionHomepageFragmentToPhotosFragment()
                categoryDocument -> HomepageFragmentDirections.actionHomepageFragmentToDocumentsFragment()
                categoryAudio -> HomepageFragmentDirections.actionHomepageFragmentToAudioFragment()
                categoryVideo -> HomepageFragmentDirections.actionHomepageFragmentToVideoFragment()
                else -> return@with
            }

            findNavController().navigate(direction)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewDataBinding = FragmentHomepageBinding.inflate(inflater, container, false)
        rootView = viewDataBinding.root

        isFabExpanded = savedInstanceState?.getBoolean(KEY_IS_FAB_EXPANDED) ?: false

        // Fully expand the BottomSheet if it had been, e.g. rotate screen
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object :
            OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (rootView.height > 0) {
                    if (savedInstanceState?.getBoolean(KEY_IS_BOTTOM_SHEET_EXPANDED) == true) {
                        fullyExpandBottomSheet()
                    }
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMask()
        setupSearchView()
        setupBannerView()
        setupCategories()
        setupBottomSheetUI()
        setupBottomSheetBehavior()
        setupFabs()

        (activity as? ManagerActivityLollipop)?.adjustTransferWidgetPositionInHomepage()

        requireContext().registerReceiver(
            networkReceiver, IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE)
        )
    }

    override fun onResume() {
        super.onResume()

        if (!isOnline(context)) {
            showOfflineMode()
        }

        viewModel.updateBannersIfNeeded()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        tabsChildren.clear()
        requireContext().unregisterReceiver(networkReceiver)
    }

    /**
     * Show the UI appearance for network connected status (normal UI)
     */
    private fun showOnlineMode() {
        if (viewModel.isRootNodeNull()) return

        viewPager.isUserInputEnabled = true
        rootView.category.isVisible = true
        rootView.banner_view.isVisible = true

        fullyCollapseBottomSheet()
        bottomSheetBehavior.isDraggable = true

        enableTabs(true)
    }

    /**
     * Show the UI appearance for network disconnected status
     */
    private fun showOfflineMode() {
        viewPager.isUserInputEnabled = false

        // other code is doing too much work when enter offline mode,
        // to prevent janky frame when change several UI elements together,
        // we have to post to end of UI thread.
        post {
            viewPager.setCurrentItem(BottomSheetPagerAdapter.OFFLINE_INDEX, false)
            rootView.category.isVisible = false
            rootView.banner_view.isVisible = false
            fullyExpandBottomSheet()
            bottomSheetBehavior.isDraggable = false
        }

        enableTabs(false)
    }

    /**
     * Enable/Disable all touchable sub views in the TabLayout
     */
    private fun enableTabs(enable: Boolean) {
        if (tabsChildren.isEmpty()) {
            tabLayout.touchables.forEach { tab ->
                tabsChildren.add(tab)
            }
        }

        tabsChildren.forEach { tab ->
            tab.isEnabled = enable
        }
    }

    /**
     * Expand the bottom sheet to the bottom of the search view
     */
    private fun fullyExpandBottomSheet() {
        val bottomSheetRoot = viewDataBinding.homepageBottomSheet.root
        bottomSheetBehavior.state = HomepageBottomSheetBehavior.STATE_EXPANDED
        viewDataBinding.backgroundMask.alpha = 1F
        bottomSheetRoot.elevation = 0F

        val layoutParams = bottomSheetRoot.layoutParams
        layoutParams.height = rootView.height - searchInputView.bottom
        bottomSheetRoot.layoutParams = layoutParams
    }

    /**
     * Collapse the bottom sheet to its initial position (its top is in the middle of the screen)
     */
    private fun fullyCollapseBottomSheet() =
        bottomSheetBehavior.setState(HomepageBottomSheetBehavior.STATE_COLLAPSED)

    /**
     * Set up the UI elements of the Search view
     * Associate the Search View hamburger icon with the Navigation Drawer
     * Set click listeners, observe the changes of chat status, notification count
     */
    private fun setupSearchView() {
        val activity = activity as ManagerActivityLollipop

        searchInputView = viewDataBinding.searchView
        searchInputView.attachNavigationDrawerToMenuButton(
            activity.drawerLayout!!
        )

        viewModel.notificationCount.observe(viewLifecycleOwner) {
            searchInputView.setLeftNotificationCount(it)
        }
        viewModel.avatar.observe(viewLifecycleOwner) {
            searchInputView.setAvatar(it)
        }
        viewModel.chatStatusDrawableId.observe(viewLifecycleOwner) {
            searchInputView.setChatStatus(it != 0, it)
        }

        searchInputView.setAvatarClickListener {
            doIfOnline(false) { activity.showMyAccount() }
        }

        searchInputView.setOnSearchInputClickListener {
            doIfOnline(false) { activity.homepageToSearch() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(KEY_IS_FAB_EXPANDED, isFabExpanded)
        if (this::bottomSheetBehavior.isInitialized) {
            outState.putBoolean(
                KEY_IS_BOTTOM_SHEET_EXPANDED,
                bottomSheetBehavior.state == HomepageBottomSheetBehavior.STATE_EXPANDED
            )
        }
    }

    /**
     * Set up the view pager, tab layout and fragments contained in the Homepage main bottom sheet
     */
    private fun setupBottomSheetUI() {
        viewPager = rootView.findViewById(R.id.view_pager)
        val adapter = BottomSheetPagerAdapter(this)
        // By setting this will make BottomSheetPagerAdapter create all the fragments on initialization.
        viewPager.offscreenPageLimit = adapter.itemCount
        viewPager.adapter = adapter
        // Attach the view pager to the tab layout
        tabLayout = rootView.findViewById(R.id.tabs)
        val mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }
        mediator.attach()

        if (!isOnline(context)) {
            viewPager.setCurrentItem(BottomSheetPagerAdapter.OFFLINE_INDEX, false)
            rootView.category.isVisible = false
        }

        // Pass selected page view to HomepageBottomSheetBehavior which would seek for
        // the nested scrolling child views and deal with the logic of nested scrolling
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                currentSelectedTabFragment = childFragmentManager.findFragmentByTag("f$position")
                bottomSheetBehavior.invalidateScrollingChild(
                    // ViewPager2 has fragments tagged as fX (e.g. f0,f1) that X is the page
                    currentSelectedTabFragment?.view
                )

                (currentSelectedTabFragment as? Scrollable)?.checkScroll()
            }
        })

        viewModel.isScrolling.observe(viewLifecycleOwner) {
            if (it.first == currentSelectedTabFragment) {
                changeTabElevation(it.second)
            }
        }
    }

    /**
     * Set up the banner view pager layout
     */
    @Suppress("UNCHECKED_CAST")
    private fun setupBannerView() {
        bannerViewPager =
            viewDataBinding.bannerView as BannerViewPager<MegaBanner, BannerViewHolder>
        bannerViewPager.setIndicatorSliderGap(BannerUtils.dp2px(6f))
            .setScrollDuration(800)
            .setAutoPlay(false)
//            .setCanLoop(false)
            .setLifecycleRegistry(lifecycle)
            .setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setIndicatorSliderGap(Util.dp2px(6f))
            .setIndicatorSliderRadius(
                Util.dp2px(3f),
                Util.dp2px(3f)
            )
            .setIndicatorGravity(IndicatorGravity.CENTER)
            .setIndicatorSliderColor(
                ContextCompat.getColor(requireContext(), R.color.grey_info_menu),
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            .setOnPageClickListener(null)
            .setAdapter(object : BaseBannerAdapter<MegaBanner, BannerViewHolder>() {
                override fun createViewHolder(
                    parent: ViewGroup,
                    itemView: View?,
                    viewType: Int
                ): BannerViewHolder {
                    return BannerViewHolder(itemView!!)
                }

                override fun onBind(
                    holder: BannerViewHolder?,
                    data: MegaBanner?,
                    position: Int,
                    pageSize: Int
                ) {
                    holder?.setViewModel(viewModel)
                    holder?.bindData(data, position, pageSize)
                }

                override fun getLayoutId(viewType: Int): Int {
                    return R.layout.item_banner_view
                }
            })
            .create()

        viewModel.bannerList.observe(viewLifecycleOwner) {
            if (it == null || it.isEmpty()) {
                bottomSheetBehavior.peekHeight = rootView.height - category.bottom
            } else {
                bannerViewPager.refreshData(it)
                bottomSheetBehavior.peekHeight = rootView.height - bannerViewPager.bottom
            }
        }
    }

    /**
     * Inflate the layout of the full screen mask
     * The mask will actually be shown after clicking to expand the FAB
     */
    private fun setupMask() {
        windowContent = activity?.window?.findViewById(Window.ID_ANDROID_CONTENT)
        fabMaskLayout = FabMaskLayoutBinding.inflate(layoutInflater, windowContent, false).root
    }

    /**
     * Set the click listeners for file categories buttons
     */
    private fun setupCategories() {
        viewDataBinding.category.categoryPhoto.setOnClickListener(categoryClickListener)
        viewDataBinding.category.categoryDocument.setOnClickListener(categoryClickListener)
        viewDataBinding.category.categoryAudio.setOnClickListener(categoryClickListener)
        viewDataBinding.category.categoryVideo.setOnClickListener(categoryClickListener)
    }

    /**
     * Get the title of the bottom sheet tab
     *
     * @param position the tab index
     * @return The title text or "" for invalid position param
     */
    private fun getTabTitle(position: Int): String {
        when (position) {
            BottomSheetPagerAdapter.RECENT_INDEX -> return resources.getString(R.string.recents_label)
            BottomSheetPagerAdapter.OFFLINE_INDEX -> return resources.getString(R.string.section_saved_for_offline_new)
        }

        return ""
    }

    private fun setupBottomSheetBehavior() {
        bottomSheetBehavior =
            HomepageBottomSheetBehavior.from(viewDataBinding.homepageBottomSheet.root)
        setBottomSheetPeekHeight()
        setBottomSheetExpandedTop()
    }

    /**
     * Set the initial height of the bottom sheet. The top is just below the banner view.
     */
    private fun setBottomSheetPeekHeight() {
        rootView.viewTreeObserver?.addOnPreDrawListener (object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                bottomSheetBehavior.peekHeight = rootView.height - category.bottom
                if (bottomSheetBehavior.peekHeight > 0) {
                    rootView.viewTreeObserver?.removeOnPreDrawListener(this)
                }

                return true
            }
        })
    }

    /**
     * Set the topmost height of the bottom sheet(when expanded).
     * The top of the bottom sheet would always below the search view.
     * In addition, set the transition effect while dragging the bottom sheet to/away from the top
     */
    private fun setBottomSheetExpandedTop() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            HomepageBottomSheetBehavior.BottomSheetCallback() {

            val backgroundMask = viewDataBinding.backgroundMask
            val dividend = 1.0f - SLIDE_OFFSET_CHANGE_BACKGROUND
            val bottomSheet = viewDataBinding.homepageBottomSheet
            val maxElevation = bottomSheet.root.elevation

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val layoutParams = bottomSheet.layoutParams
                val maxHeight = rootView.height - searchInputView.bottom

                if (bottomSheet.height > maxHeight) {
                    layoutParams.height = maxHeight
                    bottomSheet.layoutParams = layoutParams
                }

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    tabLayout.setBackgroundResource(R.drawable.bg_cardview_white)
                } else {
                    tabLayout.setBackgroundResource(R.drawable.bg_cardview_white_top)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // A background color and BottomSheet elevation transition anim effect
                // as dragging the BottomSheet close to/ far away from the top
                val diff = slideOffset - SLIDE_OFFSET_CHANGE_BACKGROUND

                if (diff <= 0) {
                    // The calculation for "alpha" may get a very small Float instead of 0.0f
                    // Reset it to 0f here
                    if (backgroundMask.alpha > 0f) backgroundMask.alpha = 0f
                    // So is the elevation
                    if (bottomSheet.elevation < maxElevation) bottomSheet.elevation = maxElevation
                    return
                }

                val res = diff / dividend
                backgroundMask.alpha = res
                bottomSheet.elevation = maxElevation - res * maxElevation
            }
        })
    }

    /**
     * Elevate the tab or not based on the scrolling in Recents/Offline fragments.
     *
     *
     * @param withElevation elevate the tab if true, false otherwise
     */
    private fun changeTabElevation(withElevation: Boolean) {
        tabLayout.elevation = if (withElevation) {
            Util.dp2px(4f).toFloat()
        } else {
            0f
        }
    }

    /**
     * Set up the Fab and Fabs in the expanded status
     */
    private fun setupFabs() {
        fabMain = rootView.fab_home_main
        fabMaskMain = fabMaskLayout.fab_main

        fabMain.setOnClickListener {
            fabMainClickCallback()
        }

        fabMaskMain.setOnClickListener {
            fabMainClickCallback()
        }

        fabMaskLayout.setOnClickListener {
            fabMainClickCallback()
        }

        fabMaskLayout.fab_chat.setOnClickListener {
            fabMainClickCallback()
            runDelay(FAB_MASK_OUT_DELAY) {
                openNewChatActivity()
            }
        }

        fabMaskLayout.text_chat.setOnClickListener {
            fabMainClickCallback()
            runDelay(FAB_MASK_OUT_DELAY) {
                openNewChatActivity()
            }
        }

        fabMaskLayout.fab_upload.setOnClickListener {
            fabMainClickCallback()
            runDelay(FAB_MASK_OUT_DELAY) {
                showUploadPanel()
            }
        }

        fabMaskLayout.text_upload.setOnClickListener {
            fabMainClickCallback()
            runDelay(FAB_MASK_OUT_DELAY) {
                showUploadPanel()
            }
        }

        if (isFabExpanded) {
            expandFab()
        }
    }

    /**
     * Do some operation if the network is connected, or show a snack bar for alerting the disconnection
     *
     * @param showSnackBar true for showing a snack bar for alerting the disconnection
     * @param operation the operation to be executed if online
     */
    private fun doIfOnline(showSnackBar: Boolean, operation: () -> Unit) {
        if (isOnline(context) && !viewModel.isRootNodeNull()) {
            operation()
        } else if (showSnackBar) {
            (activity as ManagerActivityLollipop).showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem),
                MEGACHAT_INVALID_HANDLE
            )
        }
    }

    private fun openNewChatActivity() = doIfOnline(true) {
        val intent = Intent(activity, AddContactActivityLollipop::class.java).apply {
            putExtra(KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA)
        }

        activity?.startActivityForResult(intent, REQUEST_CREATE_CHAT)
    }

    private fun showUploadPanel() = doIfOnline(true) {
        (activity as ManagerActivityLollipop).showUploadPanel()
    }

    private fun fabMainClickCallback() = if (isFabExpanded) {
        collapseFab()
    } else {
        expandFab()
    }

    fun collapseFab() {
        rotateFab(false)
        showOut(
            fabMaskLayout.fab_chat,
            fabMaskLayout.fab_upload,
            fabMaskLayout.text_chat,
            fabMaskLayout.text_upload
        )
        // After animation completed, then remove mask.
        runDelay(FAB_MASK_OUT_DELAY) {
            removeMask()
            fabMain.visibility = View.VISIBLE
            isFabExpanded = false
        }
    }

    private fun expandFab() {
        fabMain.visibility = View.GONE
        addMask()
        // Need to do so, otherwise, fabMaskMain.background is null.
        post {
            rotateFab(true)
            showIn(
                fabMaskLayout.fab_chat,
                fabMaskLayout.fab_upload,
                fabMaskLayout.text_chat,
                fabMaskLayout.text_upload
            )
            isFabExpanded = true
        }
    }

    /**
     * Present the expanded FABs with animated transition
     */
    private fun showIn(vararg fabs: View) {
        for (fab in fabs) {
            fab.visibility = View.VISIBLE
            fab.alpha = ALPHA_TRANSPARENT
            fab.translationY = fab.height.toFloat()

            fab.animate()
                .setDuration(FAB_ANIM_DURATION)
                .translationY(0f)
                .setListener(object :
                    AnimatorListenerAdapter() {/* No need to override any methods here. */ })
                .alpha(ALPHA_OPAQUE)
                .start()
        }
    }

    /**
     * Hide the expanded FABs with animated transition
     */
    private fun showOut(vararg fabs: View) {
        for (fab in fabs) {
            fab.animate()
                .setDuration(FAB_ANIM_DURATION)
                .translationY(fab.height.toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fab.visibility = View.GONE
                        super.onAnimationEnd(animation)
                    }
                }).alpha(ALPHA_TRANSPARENT)
                .start()
        }
    }

    /**
     * Showing the full screen mask by adding the mask layout to the window content
     */
    private fun addMask() {
        windowContent?.addView(fabMaskLayout)
    }

    /**
     * Removing the full screen mask
     */
    private fun removeMask() {
        windowContent?.removeView(fabMaskLayout)
    }

    /**
     * Animate the appearance of the main FAB when expanding and collapsing
     *
     * @param isExpand true if the FAB is being expanded, false for being collapsed
     */
    private fun rotateFab(isExpand: Boolean) {
        val rotateAnim = ObjectAnimator.ofFloat(
            fabMaskMain, "rotation",
            if (isExpand) FAB_ROTATE_ANGEL else FAB_DEFAULT_ANGEL
        )

        // The tint of the icon in the middle of the FAB
        val tintAnim = ObjectAnimator.ofArgb(
            fabMaskMain.drawable.mutate(), "tint",
            if (isExpand) Color.BLACK else Color.WHITE
        )

        // The background tint of the FAB
        val backgroundTintAnim = ObjectAnimator.ofArgb(
            fabMaskMain.background.mutate(), "tint",
            if (isExpand) Color.WHITE else ContextCompat.getColor(
                requireContext(),
                R.color.accentColor
            )
        )

        AnimatorSet().apply {
            duration = FAB_ANIM_DURATION
            playTogether(rotateAnim, backgroundTintAnim, tintAnim)
            start()
        }
    }

    companion object {
        private const val FAB_ANIM_DURATION = 200L
        private const val FAB_MASK_OUT_DELAY = 200L
        private const val ALPHA_TRANSPARENT = 0f
        private const val ALPHA_OPAQUE = 1f
        private const val FAB_DEFAULT_ANGEL = 0f
        private const val FAB_ROTATE_ANGEL = 135f
        private const val SLIDE_OFFSET_CHANGE_BACKGROUND = 0.8f
        private const val KEY_CONTACT_TYPE = "contactType"
        private const val KEY_IS_FAB_EXPANDED = "isFabExpanded"
        private const val KEY_IS_BOTTOM_SHEET_EXPANDED = "isBottomSheetExpanded"
    }
}
