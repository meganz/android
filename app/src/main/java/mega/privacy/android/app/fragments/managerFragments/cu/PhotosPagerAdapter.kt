package mega.privacy.android.app.fragments.managerFragments.cu

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import mega.privacy.android.app.presentation.photos.albums.AlbumsFragment

/**
 * PhotosPagerAdapter includes TimelineFragment and AlbumsFragment as tabs, using in PhotosFragment
 */
class PhotosPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    companion object {
        const val TIMELINE_INDEX = 0
        const val ALBUM_INDEX = 1
    }

    /**
     * HashMap for managing sub Fragments for viewpager
     */
    private val tabFragmentMap = hashMapOf(
        TIMELINE_INDEX to NewTimelineFragment::class.java,
        ALBUM_INDEX to AlbumsFragment::class.java
    )

    override fun getItemCount(): Int {
        return tabFragmentMap.size
    }

    override fun createFragment(position: Int): Fragment {
        var fragment: Fragment? = null

        try {
            fragment = tabFragmentMap[position]?.newInstance() as Fragment
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }

        return fragment!!
    }
}