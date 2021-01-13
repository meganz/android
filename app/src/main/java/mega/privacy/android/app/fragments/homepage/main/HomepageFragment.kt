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
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.Window
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_homepage.view.*
import kotlinx.android.synthetic.main.homepage_bottom_sheet.view.*
import kotlinx.android.synthetic.main.homepage_fabs.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.databinding.FabMaskLayoutBinding
import mega.privacy.android.app.databinding.FragmentHomepageBinding
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.fragments.homepage.homepageVisibilityChange
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

@AndroidEntryPoint
class HomepageFragment : Fragment() {

    private val viewModel: HomePageViewModel by viewModels()

    private lateinit var viewDataBinding: FragmentHomepageBinding
    private lateinit var rootView: View
    private lateinit var bottomSheetBehavior: HomepageBottomSheetBehavior<View>
    private lateinit var searchInputView: FloatingSearchView
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMaskMain: FloatingActionButton
    private lateinit var fabMaskLayout: View
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var currentSelectedTabFragment: Fragment? = null
    private val tabsChildren = ArrayList<View>()
    private var windowContent: ViewGroup? = null

    private var pendingExpandBottomSheet = false
    private val homepageVisibilityChangeObserver = androidx.lifecycle.Observer<Boolean> {
        if (it && pendingExpandBottomSheet) {
            pendingExpandBottomSheet = false
            post {
                fullyExpandBottomSheet(true)
            }
        }
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }
            when (intent.getIntExtra(INTENT_EXTRA_KEY_ACTION_TYPE, -1)) {
                GO_OFFLINE -> showOfflineMode()
                GO_ONLINE -> showOnlineMode()
            }
        }
    }

    var isFabExpanded = false

    private val categoryClickListener = OnClickListener {
        with(viewDataBinding.category) {
            val direction = when (it) {
                categoryPhoto -> HomepageFragmentDirections.actionHomepageFragmentToPhotosFragment()
                categoryDocument -> HomepageFragmentDirections.actionHomepageFragmentToDocumentsFragment()
                categoryAudio -> HomepageFragmentDirections.actionHomepageFragmentToAudioFragment()
                categoryVideo -> HomepageFragmentDirections.actionHomepageFragmentToVideoFragment();
                else -> return@with
            }

            findNavController().navigate(direction)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = FragmentHomepageBinding.inflate(inflater, container, false)
        rootView = viewDataBinding.root

        homepageVisibilityChange.observeForever(homepageVisibilityChangeObserver)

        isFabExpanded = savedInstanceState?.getBoolean(KEY_IS_FAB_EXPANDED) ?: false
        if (savedInstanceState != null) {
            val isBottomSheetExpanded = savedInstanceState.getBoolean(KEY_IS_BOTTOM_SHEET_EXPANDED)
            post {
                if (isBottomSheetExpanded) {
                    if (rootView.height == 0) {
                        pendingExpandBottomSheet = true
                    } else {
                        fullyExpandBottomSheet(true)
                    }
                }
            }
        }

        (activity as? ManagerActivityLollipop)?.adjustTransferWidgetPositionInHomepage()

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMask()
        setupSearchView()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()

        tabsChildren.clear()
        requireContext().unregisterReceiver(networkReceiver)
        homepageVisibilityChange.removeObserver(homepageVisibilityChangeObserver)
    }

    private fun showOnlineMode() {
        if (viewModel.isRootNodeNull()) return

        viewPager.isUserInputEnabled = true
        rootView.category.isVisible = true
        fullyCollapseBottomSheet()

        tabsChildren.forEach { tab ->
            tab.isEnabled = true
        }
    }

    private fun showOfflineMode() {
        viewPager.isUserInputEnabled = false
        post {
            // other code is doing too much work when enter offline mode,
            // to prevent janky frame when change several UI elements together,
            // we have to post to end of UI thread.
            viewPager.setCurrentItem(BottomSheetPagerAdapter.OFFLINE_INDEX, false)
            rootView.category.isVisible = false
            fullyExpandBottomSheet(false)
        }

        if (tabsChildren.isEmpty()) {
            tabLayout.touchables.forEach { tab ->
                tab.isEnabled = false
                tabsChildren.add(tab)
            }
        } else {
            tabsChildren.forEach { tab ->
                tab.isEnabled = false
            }
        }
    }

    private fun fullyExpandBottomSheet(draggable: Boolean) {
        val bottomSheetRoot = viewDataBinding.homepageBottomSheet.root
        bottomSheetBehavior.state = HomepageBottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.isDraggable = draggable
        viewDataBinding.backgroundMask.alpha = 1F
        bottomSheetRoot.elevation = 0F

        val layoutParams = bottomSheetRoot.layoutParams
        layoutParams.height = rootView.height - searchInputView.bottom
        bottomSheetRoot.layoutParams = layoutParams
    }

    private fun fullyCollapseBottomSheet() {
        bottomSheetBehavior.setState(HomepageBottomSheetBehavior.STATE_COLLAPSED)
        bottomSheetBehavior.isDraggable = true
    }

    private fun setupSearchView() {
        val activity = activity as ManagerActivityLollipop

        searchInputView = viewDataBinding.searchView
        searchInputView.attachNavigationDrawerToMenuButton(
            activity.drawerLayout!!
        )

        viewModel.notification.observe(viewLifecycleOwner) {
            searchInputView.setLeftNotificationCount(it)
        }
        viewModel.avatar.observe(viewLifecycleOwner) {
            searchInputView.setAvatar(it)
        }
        viewModel.chatStatus.observe(viewLifecycleOwner) {
            val iconRes = if (Util.isDarkMode(requireContext())) {
                when (it) {
                    MegaChatApi.STATUS_ONLINE -> R.drawable.ic_online_dark_drawer
                    MegaChatApi.STATUS_AWAY -> R.drawable.ic_away_dark_drawer
                    MegaChatApi.STATUS_BUSY -> R.drawable.ic_busy_dark_drawer
                    MegaChatApi.STATUS_OFFLINE -> R.drawable.ic_offline_dark_drawer
                    else -> 0
                }
            } else {
                when (it) {
                    MegaChatApi.STATUS_ONLINE -> R.drawable.ic_online_light
                    MegaChatApi.STATUS_AWAY -> R.drawable.ic_away_light
                    MegaChatApi.STATUS_BUSY -> R.drawable.ic_busy_light
                    MegaChatApi.STATUS_OFFLINE -> R.drawable.ic_offline_light
                    else -> 0
                }
            }

            searchInputView.setChatStatus(iconRes != 0, iconRes)
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

    private fun setupBottomSheetUI() {
        viewPager = rootView.view_pager
        val adapter = BottomSheetPagerAdapter(this)
        // By setting this will make BottomSheetPagerAdapter create all the fragments on initialization.
        viewPager.offscreenPageLimit = adapter.itemCount
        viewPager.adapter = adapter
        // Attach the view pager to the tab layout
        tabLayout = rootView.tab_layout
        val mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }
        mediator.attach()

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

        setupBottomSheetBackground()
    }

    /**
     * Set bottom sheet background for the elevation effect according to Invision
     */
    private fun setupBottomSheetBackground() {
        val elevationPx = Util.dp2px(BOTTOM_SHEET_ELEVATION, resources.displayMetrics).toFloat()
        val cornerSizePx = Util.dp2px(BOTTOM_SHEET_CORNER_SIZE, resources.displayMetrics).toFloat()

        viewDataBinding.root.homepage_bottom_sheet.background =
            ColorUtils.getShapeDrawableForElevation(
                requireContext(),
                elevationPx,
                cornerSizePx
            )
        viewPager.setBackgroundColor(ColorUtils.getColorForElevation(requireContext(), elevationPx))
    }

    private fun setupMask() {
        windowContent = activity?.window?.findViewById(Window.ID_ANDROID_CONTENT)
        fabMaskLayout = FabMaskLayoutBinding.inflate(layoutInflater, windowContent, false).root
    }

    private fun setupCategories() {
        viewDataBinding.category.categoryPhoto.setOnClickListener(categoryClickListener)
        viewDataBinding.category.categoryDocument.setOnClickListener(categoryClickListener)
        viewDataBinding.category.categoryAudio.setOnClickListener(categoryClickListener)
        viewDataBinding.category.categoryVideo.setOnClickListener(categoryClickListener)
    }

    private fun getTabTitle(position: Int): String? {
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

    private fun setBottomSheetPeekHeight() {
        rootView.viewTreeObserver?.addOnPreDrawListener {
            bottomSheetBehavior.peekHeight = rootView.height - viewDataBinding.category.root.bottom
            true
        }
    }

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

    private fun changeTabElevation(withElevation: Boolean) {
        if (withElevation) {
            tabLayout.elevation = Util.dp2px(4f, resources.displayMetrics).toFloat()
        } else {
            tabLayout.elevation = 0f
        }
    }

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

    private fun openNewChatActivity() {
        doIfOnline(true) {
            val intent = Intent(activity, AddContactActivityLollipop::class.java).apply {
                putExtra(KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA)
            }

            activity?.startActivityForResult(intent, REQUEST_CREATE_CHAT)
        }
    }

    private fun showUploadPanel() {
        doIfOnline(true) {
            (activity as ManagerActivityLollipop).showUploadPanel()
        }
    }

    private fun fabMainClickCallback() {
        if (isFabExpanded) {
            collapseFab()
        } else {
            expandFab()
        }
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

    private fun showIn(vararg fabs: View) {
        for (fab in fabs) {
            showIn(fab)
        }
    }

    private fun showOut(vararg fabs: View) {
        for (fab in fabs) {
            showOut(fab)
        }
    }

    private fun addMask() {
        windowContent?.addView(fabMaskLayout)
    }

    private fun removeMask() {
        windowContent?.removeView(fabMaskLayout)
    }

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

    private fun showIn(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = ALPHA_TRANSPARENT
        view.translationY = view.height.toFloat()

        view.animate()
            .setDuration(FAB_ANIM_DURATION)
            .translationY(0f)
            .setListener(object :
                AnimatorListenerAdapter() {/* No need to override any methods here. */ })
            .alpha(ALPHA_OPAQUE)
            .start()
    }

    private fun showOut(view: View) {
        view.animate()
            .setDuration(FAB_ANIM_DURATION)
            .translationY(view.height.toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    super.onAnimationEnd(animation)
                }
            }).alpha(ALPHA_TRANSPARENT)
            .start()
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
        const val BOTTOM_SHEET_ELEVATION = 2f    // 2dp, for the overlay opacity is 7%
        private const val BOTTOM_SHEET_CORNER_SIZE = 8f  // 8dp
        private const val KEY_IS_BOTTOM_SHEET_EXPANDED = "isBottomSheetExpanded"
    }
}
