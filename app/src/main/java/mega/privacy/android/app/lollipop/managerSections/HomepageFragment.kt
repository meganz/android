package mega.privacy.android.app.lollipop.managerSections

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.HomepageBottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.BottomSheetPagerAdapter
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.databinding.FragmentHomepageBinding
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

@AndroidEntryPoint
class HomepageFragment : Fragment() {

    private lateinit var viewDataBinding : FragmentHomepageBinding
    private lateinit var rootView : View
    private lateinit var bottomSheetBehavior: HomepageBottomSheetBehavior<View>
    private lateinit var searchInputView: FloatingSearchView
    private lateinit var fabMain: FloatingActionButton

    private var isFabExpanded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewDataBinding = FragmentHomepageBinding.inflate(inflater, container, false)
        rootView = viewDataBinding.root

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchView()
        setupBottomSheetUI()
        setupBottomSheetBehavior()
        setupFabs()
    }

    private fun setupSearchView() {
        searchInputView = viewDataBinding.searchView
        searchInputView.attachNavigationDrawerToMenuButton(
            (activity as ManagerActivityLollipop).drawerLayout!!)
    }

    private fun setupBottomSheetUI() {
        val viewPager = rootView.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = BottomSheetPagerAdapter(this)

        // Attach the view pager to the tab layout
        val tabs = rootView.findViewById<TabLayout>(R.id.tabs)
        val mediator = TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }
        mediator.attach()

        // Pass selected page view to HomepageBottomSheetBehavior which would seek for
        // the nested scrolling child views and deal with the logic of nested scrolling
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomSheetBehavior.invalidateScrollingChild(
                    (viewPager.adapter as BottomSheetPagerAdapter).getViewAt(position)
                )
            }
        })
    }

    private fun getTabTitle(position: Int): String? {
        val resources = activity?.resources

        when (position) {
            BottomSheetPagerAdapter.RECENT_INDEX -> return resources?.getString(R.string.tab_recents)
            BottomSheetPagerAdapter.FAVOURITES_INDEX -> return resources?.getString(R.string.tab_favourites)
            BottomSheetPagerAdapter.OFFLINE_INDEX -> return resources?.getString(R.string.tab_offline)
        }

        return ""
    }

    private fun setupBottomSheetBehavior() {
        bottomSheetBehavior = HomepageBottomSheetBehavior.from(viewDataBinding.homepageBottomSheet)
        setBottomSheetPeekHeight()
        setBottomSheetExpandedTop()
    }

    private fun setBottomSheetPeekHeight() {
        rootView.viewTreeObserver?.addOnPreDrawListener {
            bottomSheetBehavior.peekHeight = rootView.height - viewDataBinding.banner.bottom
            true
        }
    }

    private fun setBottomSheetExpandedTop() {
        bottomSheetBehavior.addBottomSheetCallback(object :
            HomepageBottomSheetBehavior.BottomSheetCallback() {

            val backgroundMask = rootView.findViewById<View>(R.id.background_mask)
            val dividend = 1.0f - SLIDE_OFFSET_CHANGE_BACKGROUND
            val bottomSheet = viewDataBinding.homepageBottomSheet
            val maxElevation = bottomSheet.elevation

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
                    return
                }

                val res = diff / dividend
                backgroundMask.alpha = res
                bottomSheet.elevation = maxElevation - res * maxElevation
            }
        })
    }

    private fun setupFabs() {
        fabMain = rootView.findViewById(R.id.fab_main)
        val fabChat = rootView.findViewById<View>(R.id.fab_chat)
        val fabUpload = rootView.findViewById<View>(R.id.fab_upload)
        val textChat = rootView.findViewById<View>(R.id.text_chat)
        val textUpload = rootView.findViewById<View>(R.id.text_upload)

        fabMain.setOnClickListener {
            val mask = viewDataBinding.viewMask
            rotateFab()

            if (isFabExpanded) {
                showOut(fabChat)
                showOut(fabUpload)
                showOut(textChat)
                showOut(textUpload)
            } else {
                showIn(fabChat)
                showIn(fabUpload)
                showIn(textChat)
                showIn(textUpload)
            }

            mask.visibility = if (isFabExpanded) View.GONE else View.VISIBLE
            isFabExpanded = !isFabExpanded
        }

        fabChat.setOnClickListener {
        }

        fabUpload.setOnClickListener {
        }
    }

    private fun rotateFab() {
        val rotateAnim = ObjectAnimator.ofFloat(
            fabMain, "rotation",
            if (isFabExpanded) FAB_DEFAULT_ANGEL else FAB_ROTATE_ANGEL
        )

        // The tint of the icon in the middle of the FAB
        val tintAnim = ObjectAnimator.ofArgb(
            fabMain.drawable.mutate(), "tint",
            if (isFabExpanded) Color.WHITE else Color.BLACK
        )

        // The background tint of the FAB
        val backgroundTintAnim = ObjectAnimator.ofArgb(
            fabMain.background.mutate(), "tint",
            if (isFabExpanded) resources.getColor(R.color.accentColor) else Color.WHITE
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
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                }
            })
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
        private const val ALPHA_TRANSPARENT = 0f
        private const val ALPHA_OPAQUE = 1f
        private const val FAB_DEFAULT_ANGEL = 0f
        private const val FAB_ROTATE_ANGEL = 135f
        private const val SLIDE_OFFSET_CHANGE_BACKGROUND = 0.8f
    }
}