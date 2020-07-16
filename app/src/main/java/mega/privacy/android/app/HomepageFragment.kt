package mega.privacy.android.app

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

class HomepageFragment : Fragment() {
    private lateinit var behavior: HomepageBottomSheetBehavior<*>
    private var heightPixels = 0
    private var searchBottom = 0
    private lateinit var searchInputView: FloatingSearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_homepage, container, false)
//        view.findViewById<TextView>(R.id.textview).setOnClickListener {
//            findNavController().navigate(R.id.action_homepageFragment_to_homepageFragment2)
//        }

        behavior = HomepageBottomSheetBehavior.from(view.findViewById<View>(R.id.design_bottom_sheet1))
                as HomepageBottomSheetBehavior<*>

        searchInputView = view.findViewById<FloatingSearchView>(R.id.searchView)
        searchInputView.attachNavigationDrawerToMenuButton((activity as ManagerActivityLollipop).drawerLayout!!)

        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = BottomSheetPagerAdapter(this)
        val tabs = view.findViewById<TabLayout>(R.id.tabs)
        val mediator = TabLayoutMediator(tabs, viewPager) {
            tab, _ -> tab.text = "Recent"
        }
        mediator.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                behavior.invalidateScrollingChild((viewPager.adapter as BottomSheetPagerAdapter).getViewAt(position))
            }
        })

        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Log.i("Alex", "onGlobalLayout")
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                heightPixels = resources.displayMetrics.heightPixels
                searchBottom = searchInputView.bottom
                val banner = view?.findViewById<View>(R.id.banner)
                behavior.peekHeight = view!!.height - banner!!.bottom - 20
                view.findViewById<View>(R.id.design_bottom_sheet1)?.visibility = View.VISIBLE
            }
        })

        behavior.addBottomSheetCallback(object : HomepageBottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val layoutParams = bottomSheet.layoutParams
                if (bottomSheet.height > view.height - searchBottom - 20) {
                    layoutParams.height = view.height - searchBottom - 20
                    bottomSheet.layoutParams = layoutParams
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        return view
    }
}