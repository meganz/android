package mega.privacy.android.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

class HomepageFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_homepage, container, false)
//        view.findViewById<TextView>(R.id.textview).setOnClickListener {
//            findNavController().navigate(R.id.action_homepageFragment_to_homepageFragment2)
//        }

        val searchInputView = view.findViewById<FloatingSearchView>(R.id.searchView)
        searchInputView.attachNavigationDrawerToMenuButton((activity as ManagerActivityLollipop).drawerLayout!!)
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = BottomSheetPagerAdapter(this)
        val tabs = view.findViewById<TabLayout>(R.id.tabs)
        val mediator = TabLayoutMediator(tabs, viewPager) {
            tab, _ -> tab.text = "Recent"
        }
        mediator.attach()
        return view
    }
}