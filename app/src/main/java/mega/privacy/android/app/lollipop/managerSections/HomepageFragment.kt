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
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.BottomSheetPagerAdapter
import mega.privacy.android.app.HomepageBottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

@AndroidEntryPoint
class HomepageFragment : Fragment() {
    private lateinit var bottomSheetBehavior: HomepageBottomSheetBehavior<View>
    private var heightPixels = 0
    private var searchBottom = 0
    private lateinit var searchInputView: FloatingSearchView
    private var isRotate = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_homepage, container, false)

        bottomSheetBehavior = HomepageBottomSheetBehavior.from(view.findViewById<View>(R.id.homepage_bottom_sheet))

        searchInputView = view.findViewById<FloatingSearchView>(R.id.searchView)
        searchInputView.attachNavigationDrawerToMenuButton((activity as ManagerActivityLollipop).drawerLayout!!)

        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter =
            BottomSheetPagerAdapter(this)
        val tabs = view.findViewById<TabLayout>(R.id.tabs)
        val mediator = TabLayoutMediator(tabs, viewPager) { tab, _ ->
            tab.text = "Recent"
        }
        mediator.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomSheetBehavior.invalidateScrollingChild((viewPager.adapter as BottomSheetPagerAdapter).getViewAt(position))
            }
        })

        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                Log.i("Alex", "onGlobalLayout")
                view.viewTreeObserver.removeOnPreDrawListener(this)
//                heightPixels = resources.displayMetrics.heightPixels
                searchBottom = searchInputView.bottom
                val banner = view?.findViewById<View>(R.id.banner)
                bottomSheetBehavior.peekHeight = view!!.height - banner!!.bottom - 20
//                view.findViewById<View>(R.id.design_bottom_sheet1)?.visibility = View.VISIBLE
                return true
            }
        })

        bottomSheetBehavior.addBottomSheetCallback(object : HomepageBottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val layoutParams = bottomSheet.layoutParams
                if (bottomSheet.height > view.height - searchBottom - 20) {
                    layoutParams.height = view.height - searchBottom - 20
                    bottomSheet.layoutParams = layoutParams
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        val fabChat = view.findViewById<View>(R.id.fab_chat)
        val fabUpload = view.findViewById<View>(R.id.fab_upload)
        val textChat = view.findViewById<View>(R.id.text_chat)
        val textUpload = view.findViewById<View>(R.id.text_upload)
        initFabs(fabChat)
        initFabs(fabUpload)
        initFabs(textChat)
        initFabs(textUpload)
        fabChat.setOnClickListener {

        }
        fabUpload.setOnClickListener {

        }

        view.findViewById<FloatingActionButton>(R.id.fab_main).setOnClickListener {
            val mask = view.findViewById<View>(R.id.view_mask)
            isRotate = rotateFab(it, !isRotate)
            if (isRotate) {
                mask.visibility = View.VISIBLE
                showIn(fabChat)
                showIn(fabUpload)
                showIn(textChat)
                showIn(textUpload)
            } else {
                mask.visibility = View.GONE
                showOut(fabChat)
                showOut(fabUpload)
                showOut(textChat)
                showOut(textUpload)
            }
        }

        return view
    }

    private fun initFabs(v: View) {
        v.visibility = View.GONE;
        v.translationY = v.height.toFloat();
        v.alpha = 0f;
    }

    private fun rotateFab(v: View, rotate: Boolean): Boolean {
        val rotateAnim = ObjectAnimator.ofFloat(v, "rotation", if (rotate) 135f else 0f)
        val tintAnim = ObjectAnimator.ofArgb((v as FloatingActionButton).drawable.mutate(), "tint", if (rotate) Color.BLACK else Color.WHITE)
        val backgroundTintAnim = ObjectAnimator.ofArgb(v.background.mutate(), "tint", if (rotate) Color.WHITE else 0xFF00BFA5.toInt())

        AnimatorSet().apply { duration = 200
            playTogether(rotateAnim, backgroundTintAnim, tintAnim)
            start()
        }
        return rotate
    }

    private fun showIn(v: View) {
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.translationY = v.height.toFloat()

        v.animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                    }
                })
                .alpha(1f)
                .start()
    }

    private fun showOut(v: View) {
        v.visibility = View.VISIBLE
        v.alpha = 1f
        v.translationY = 0f
        v.animate()
                .setDuration(200)
                .translationY(v.height.toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        v.visibility = View.GONE
                        super.onAnimationEnd(animation)
                    }
                }).alpha(0f)
                .start()
    }
}