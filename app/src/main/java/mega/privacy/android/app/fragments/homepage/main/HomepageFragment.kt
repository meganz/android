package mega.privacy.android.app.fragments.homepage.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.zhpan.bannerview.BannerViewPager
import com.zhpan.bannerview.constants.IndicatorGravity
import com.zhpan.bannerview.utils.BannerUtils
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.databinding.FragmentHomepageBinding
import mega.privacy.android.app.fragments.homepage.banner.BannerAdapter
import mega.privacy.android.app.fragments.homepage.banner.BannerClickHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.mapper.UserChatStatusIconMapper
import mega.privacy.android.app.main.view.OngoingCallViewModel
import mega.privacy.android.app.presentation.manager.UserInfoViewModel
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.notAlertAnymoreAboutStartScreen
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.shouldShowStartScreenDialog
import mega.privacy.android.app.presentation.startconversation.StartConversationActivity
import mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.mobile.analytics.event.HomeFABPressedEvent
import mega.privacy.mobile.analytics.event.HomeScreenAudioTilePressedEvent
import mega.privacy.mobile.analytics.event.HomeScreenDocsTilePressedEvent
import mega.privacy.mobile.analytics.event.HomeScreenEvent
import mega.privacy.mobile.analytics.event.HomeScreenSearchMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeScreenVideosTilePressedEvent
import mega.privacy.mobile.analytics.event.OfflineTabEvent
import mega.privacy.mobile.analytics.event.RecentsTabEvent
import mega.privacy.mobile.analytics.event.SmartBannerSwipeEvent
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class HomepageFragment : Fragment() {

    companion object {
        private const val SLIDE_OFFSET_CHANGE_BACKGROUND = 0.8f
        const val BOTTOM_SHEET_ELEVATION = 2f    // 2dp, for the overlay opacity is 7%
        private const val BOTTOM_SHEET_CORNER_SIZE = 8f  // 8dp
        private const val KEY_IS_BOTTOM_SHEET_EXPANDED = "isBottomSheetExpanded"
        private const val START_SCREEN_DIALOG_SHOWN = "START_SCREEN_DIALOG_SHOWN"
    }

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var userChatStatusIconMapper: UserChatStatusIconMapper

    @Inject
    lateinit var navigator: MegaNavigator

    private val viewModel: HomePageViewModel by viewModels()
    private val userInfoViewModel: UserInfoViewModel by activityViewModels()
    private val callInProgressViewModel: OngoingCallViewModel by activityViewModels()

    private lateinit var viewDataBinding: FragmentHomepageBinding

    /** Shorthand for viewDataBinding.root */
    private lateinit var rootView: View

    private lateinit var bottomSheetBehavior: HomepageBottomSheetBehavior<View>
    private lateinit var searchInputView: FloatingSearchView
    private lateinit var bannerViewPager: BannerViewPager<Banner>

    /** The fab button in normal state */
    private lateinit var fabMain: FloatingActionButton

    /** The view pager in the bottom sheet, containing 2 pages: Recents and Offline */
    private lateinit var viewPager: ViewPager2

    /** The tab layout in the bottom sheet, associated with the view pager */
    private lateinit var tabLayout: TabLayout

    private var currentSelectedTabFragment: Fragment? = null

    /** The list of all sub views of the TabLayout*/
    private val tabsChildren = ArrayList<View>()

    private var startScreenDialog: AlertDialog? = null

    private var isPageScrolled: Boolean = false

    private val openNewChatLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val isNewMeeting =
                    data.getBooleanExtra(StartConversationActivity.EXTRA_NEW_MEETING, false)
                val isJoinMeeting =
                    data.getBooleanExtra(StartConversationActivity.EXTRA_JOIN_MEETING, false)
                if (isNewMeeting) {
                    (activity as? ManagerActivity)?.onCreateMeeting()
                } else if (isJoinMeeting) {
                    (activity as? ManagerActivity)?.onJoinMeeting()
                } else {
                    val chatId = data.getLongExtra(
                        StartConversationActivity.EXTRA_NEW_CHAT_ID,
                        MEGACHAT_INVALID_HANDLE
                    )
                    if (chatId != MEGACHAT_INVALID_HANDLE) {
                        navigator.openChat(
                            context = requireActivity(),
                            chatId = chatId,
                            action = Constants.ACTION_CHAT_SHOW_MESSAGES
                        )
                    }
                }
            }
        }

    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    BottomSheetPagerAdapter.RECENT_INDEX -> {
                        Analytics.tracker.trackEvent(RecentsTabEvent)
                    }

                    BottomSheetPagerAdapter.OFFLINE_INDEX -> {
                        Analytics.tracker.trackEvent(OfflineTabEvent)
                    }
                }
                currentSelectedTabFragment = childFragmentManager.findFragmentByTag("f$position")
                bottomSheetBehavior.invalidateScrollingChild(
                    // ViewPager2 has fragments tagged as fX (e.g. f0,f1) that X is the page
                    currentSelectedTabFragment?.view
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewDataBinding = FragmentHomepageBinding.inflate(inflater, container, false)
        rootView = viewDataBinding.root

        // Fully expand the BottomSheet if it had been, e.g. rotate screen
        if (savedInstanceState?.getBoolean(KEY_IS_BOTTOM_SHEET_EXPANDED) == true) {
            rootView.waitForLayout {
                if (rootView.height > 0) {
                    fullyExpandBottomSheet()
                }
                true
            }
        }

        (activity as? ManagerActivity)?.adjustTransferWidgetPositionInHomepage()
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchView()
        setupBannerView()
        setupCategories()
        setupBottomSheetUI()
        setupBottomSheetBehavior()
        setupFab()

        (activity as? ManagerActivity)?.apply {
            adjustTransferWidgetPositionInHomepage()
        }

        if (savedInstanceState?.getBoolean(START_SCREEN_DIALOG_SHOWN, false) == true) {
            showChooseStartScreenDialog()
        }
        viewLifecycleOwner.collectFlow(viewModel.monitorConnectivity) { isConnected ->
            if (isConnected) {
                showOnlineMode()
            } else {
                showOfflineMode()
            }
        }

        viewLifecycleOwner.collectFlow(viewModel.monitorFetchNodesFinish) {
            showOnlineMode()
        }
    }

    override fun onResume() {
        super.onResume()

        Analytics.tracker.trackEvent(HomeScreenEvent)
        Firebase.crashlytics.log("Screen: ${HomeScreenEvent.eventName}")

        // Retrieve the banners from the server again, for the banners are possibly varied
        // while the app is on the background
        viewModel.getBanners()

        callManager { manager ->
            if (manager.isInMainHomePage && shouldShowStartScreenDialog(requireContext())) {
                showChooseStartScreenDialog()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        tabsChildren.clear()
        searchInputView.stopCallAnimation()
        startScreenDialog?.dismiss()
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    }

    /**
     * Show the UI appearance for network connected status (normal UI)
     */
    private fun showOnlineMode() {
        viewPager.isUserInputEnabled = true
        viewDataBinding.category.root.isVisible = true
        viewDataBinding.bannerView.isVisible = true

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
            viewDataBinding.category.root.isVisible = false
            viewDataBinding.bannerView.isVisible = false
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

        setBottomSheetMaxHeight()
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
        val activity = activity as ManagerActivity

        searchInputView = viewDataBinding.searchView
        searchInputView.attachNavigationDrawerToMenuButton(
            activity.drawerLayout
        )

        viewModel.notificationCount.observe(viewLifecycleOwner) {
            searchInputView.setLeftNotificationCount(it)
        }
        viewLifecycleOwner.collectFlow(userInfoViewModel.state) {
            searchInputView.setAvatar(it.avatarContent)
        }

        viewLifecycleOwner.collectFlow(callInProgressViewModel.state) {
            searchInputView.setOngoingCallVisibility(it.currentCall != null)
        }

        viewLifecycleOwner.collectFlow(viewModel.uiState) {
            val iconRes =
                userChatStatusIconMapper(it.userChatStatus, Util.isDarkMode(requireContext()))
            searchInputView.setChatStatus(iconRes != 0, iconRes)
        }

        searchInputView.setAvatarClickListener {
            doIfOnline(false) { activity.showMyAccount() }
        }

        searchInputView.setOnSearchInputClickListener {
            doIfOnline(false) {
                Analytics.tracker.trackEvent(HomeScreenSearchMenuToolbarEvent)
                activity.homepageToSearch()
            }
        }

        searchInputView.setOngoingCallClickListener {
            doIfOnline(false) { activity.returnCall() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(START_SCREEN_DIALOG_SHOWN, isAlertDialogShown(startScreenDialog))
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
        viewPager = viewDataBinding.homepageBottomSheet.viewPager
        val adapter =
            BottomSheetPagerAdapter(
                fragment = this@HomepageFragment,
            )
        // By setting this will make BottomSheetPagerAdapter create all the fragments on initialization.
        viewPager.offscreenPageLimit = adapter.itemCount
        viewPager.adapter = adapter
        // Attach the view pager to the tab layout
        tabLayout = viewDataBinding.homepageBottomSheet.tabLayout
        val mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }
        mediator.attach()

        // Pass selected page view to HomepageBottomSheetBehavior which would seek for
        // the nested scrolling child views and deal with the logic of nested scrolling
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
        setupBottomSheetBackground()
    }

    /**
     * Set bottom sheet background for the elevation effect according to Invision
     */
    private fun setupBottomSheetBackground() {
        val elevationPx = Util.dp2px(BOTTOM_SHEET_ELEVATION, resources.displayMetrics).toFloat()
        val cornerSizePx = Util.dp2px(BOTTOM_SHEET_CORNER_SIZE, resources.displayMetrics).toFloat()

        viewDataBinding.homepageBottomSheet.root.background =
            ColorUtils.getShapeDrawableForElevation(
                requireContext(),
                elevationPx,
                cornerSizePx
            )
        viewPager.setBackgroundColor(ColorUtils.getColorForElevation(requireContext(), elevationPx))
    }

    /**
     * Set up the banner view pager layout
     */
    @Suppress("UNCHECKED_CAST")
    private fun setupBannerView() {
        val bannerAdapter = BannerAdapter(viewModel)
        bannerAdapter.setClickBannerCallback(BannerClickHandler(this))

        bannerViewPager =
            viewDataBinding.bannerView as BannerViewPager<Banner>
        bannerViewPager.setIndicatorSliderGap(BannerUtils.dp2px(6f))
            .setScrollDuration(800)
            .setAutoPlay(false)
            .setLifecycleRegistry(lifecycle)
            .setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setIndicatorSliderGap(Util.dp2px(6f))
            .setIndicatorSliderRadius(
                Util.dp2px(3f),
                Util.dp2px(3f)
            )
            .setIndicatorGravity(IndicatorGravity.CENTER)
            .setIndicatorSliderColor(
                ContextCompat.getColor(requireContext(), R.color.grey_300_grey_600),
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            .setOnPageClickListener(null)
            .registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        if (isPageScrolled) {
                            Timber.d("onPageSelected: $position")
                            Analytics.tracker.trackEvent(SmartBannerSwipeEvent)
                            isPageScrolled = false
                        }
                    }

                    override fun onPageScrollStateChanged(state: Int) {
                        super.onPageScrollStateChanged(state)
                        Timber.d("onPageScrollStateChanged: $state")
                        isPageScrolled = state == ViewPager2.SCROLL_STATE_SETTLING
                    }
                }
            )
            .setAdapter(bannerAdapter)
            .create()

        viewLifecycleOwner.collectFlow(viewModel.banners) {
            bannerViewPager.refreshData(it)
        }
    }

    /**
     * Set the click listeners for file categories buttons
     */
    private fun setupCategories() {
        viewDataBinding.category.categoryFavourites.setOnClickListener {
            clickFavouritesSectionTile()
        }
        viewDataBinding.category.categoryDocument.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                clickDocumentSectionTileAndAnalysis()
            }
        }
        viewDataBinding.category.categoryAudio.setOnClickListener {
            clickAudioSectionTileAndAnalysis()
        }
        viewDataBinding.category.categoryVideo.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                clickVideoSectionTileAndAnalysis()
            }
        }
    }

    private fun clickFavouritesSectionTile() {
        findNavController().run {
            if (currentDestination?.id == R.id.homepageFragment) {
                navigate(HomepageFragmentDirections.actionHomepageFragmentToFavourites())
            }
        }
    }

    private fun clickDocumentSectionTileAndAnalysis() {
        Analytics.tracker.trackEvent(HomeScreenDocsTilePressedEvent)
        findNavController().run {
            if (currentDestination?.id == R.id.homepageFragment) {
                val destination =
                    HomepageFragmentDirections.actionHomepageFragmentToDocumentSectionFragment()

                currentDestination?.getAction(destination.actionId)?.let {
                    navigate(destination.actionId)
                }
            }
        }
    }

    private fun clickVideoSectionTileAndAnalysis() {
        Analytics.tracker.trackEvent(HomeScreenVideosTilePressedEvent)
        findNavController().run {
            if (currentDestination?.id == R.id.homepageFragment) {
                val destination =
                    HomepageFragmentDirections.actionHomepageFragmentToVideoSectionFragment()
                currentDestination?.getAction(destination.actionId)?.let {
                    navigate(destination.actionId)
                }
            }
        }
    }

    private fun clickAudioSectionTileAndAnalysis() {
        Analytics.tracker.trackEvent(HomeScreenAudioTilePressedEvent)
        findNavController().run {
            if (currentDestination?.id == R.id.homepageFragment) {
                navigate(
                    HomepageFragmentDirections.actionHomepageFragmentToAudioSectionFragment()
                )
            }
        }
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
        rootView.waitForLayout {
            if (bannerViewPager.data.isNotEmpty()) {
                bottomSheetBehavior.peekHeight = rootView.height - bannerViewPager.bottom
            } else {
                bottomSheetBehavior.peekHeight =
                    rootView.height - viewDataBinding.category.root.bottom
            }

            setBottomSheetMaxHeight()
            false
        }
    }

    /**
     * Set the topmost height of the bottom sheet(when expanded).
     * The top of the bottom sheet would always below the search view.
     * In addition, set the transition effect while dragging the bottom sheet to/away from the top
     */
    private fun setBottomSheetExpandedTop() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            HomepageBottomSheetBehavior.BottomSheetCallback() {

            @SuppressLint("StaticFieldLeak")
            val backgroundMask = viewDataBinding.backgroundMask
            val dividend = 1.0f - SLIDE_OFFSET_CHANGE_BACKGROUND
            val bottomSheet = viewDataBinding.homepageBottomSheet
            val maxElevation = bottomSheet.root.elevation

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                setBottomSheetMaxHeight()
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

    private fun setBottomSheetMaxHeight() {
        val bottomSheet = viewDataBinding.homepageBottomSheet.root
        val maxHeight = rootView.measuredHeight - searchInputView.bottom
        val layoutParams = bottomSheet.layoutParams

        if (layoutParams.height != maxHeight) {
            layoutParams.height = maxHeight
            bottomSheet.layoutParams = layoutParams
        }
    }

    /**
     * Set up the FAB
     */
    private fun setupFab() {
        fabMain = viewDataBinding.fabHomeMain

        fabMain.setOnClickListener {
            Analytics.tracker.trackEvent(HomeFABPressedEvent)
            showUploadPanel()
        }
    }

    /**
     * Do some operation if the network is connected, or show a snack bar for alerting the disconnection
     *
     * @param showSnackBar true for showing a snack bar for alerting the disconnection
     * @param operation the operation to be executed if online
     */
    private fun doIfOnline(showSnackBar: Boolean, operation: () -> Unit) {
        if (viewModel.isConnected.value) {
            operation()
        } else if (showSnackBar) {
            (activity as ManagerActivity).showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem),
                MEGACHAT_INVALID_HANDLE
            )
        }
    }

    @Suppress("deprecation")
    fun openNewChatActivity() = doIfOnline(true) {
        openNewChatLauncher.launch(Intent(activity, StartConversationActivity::class.java))
    }

    private fun showUploadPanel() = doIfOnline(true) {
        (activity as ManagerActivity).showUploadPanel()
    }

    /**
     * Update FAB position, considering the visibility of PSA layout and mini audio player.
     *
     * @param extendsHeight the height of the PSA layout or mini audio player
     */
    fun updateFabPosition(extendsHeight: Int) {
        if (!this::fabMain.isInitialized) {
            return
        }

        val fabMainParams = fabMain.layoutParams as ConstraintLayout.LayoutParams
        fabMainParams.bottomMargin =
            resources.getDimensionPixelSize(R.dimen.fab_margin_span) + extendsHeight
        fabMain.layoutParams = fabMainParams
    }

    /**
     * Hides the fabButton
     */
    fun hideFabButton() {
        fabMain.hide()
    }

    /**
     * Shows the fabButton
     */
    fun showFabButton() {
        fabMain.show()
    }

    /**
     * Shows the dialog which informs the start screen can be changed.
     */
    private fun showChooseStartScreenDialog() {
        startScreenDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_choose_start_screen)
            .setPositiveButton(getString(R.string.change_setting_action)) { _, _ ->
                callManager { manager -> manager.moveToSettingsSectionStartScreen() }
                notAlertAnymoreAboutStartScreen(requireContext())
            }
            .setNegativeButton(getString(R.string.general_dismiss)) { _, _ ->
                notAlertAnymoreAboutStartScreen(requireContext())
            }
            .show().apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }
    }
}
