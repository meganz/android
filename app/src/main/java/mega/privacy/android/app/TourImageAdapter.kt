package mega.privacy.android.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

/**
 * Tour image adapter
 *
 * @property context
 */
class TourImageAdapter(private val context: Context) : PagerAdapter() {
    private val mImages = intArrayOf(
        R.drawable.tour1,
        R.drawable.tour2,
        R.drawable.tour3,
        R.drawable.tour4,
        R.drawable.tour5
    )

    private val barTitles: Array<String> = context.resources.getStringArray(R.array.tour_titles)
    private val barTexts: Array<String> = context.resources.getStringArray(R.array.tour_text)

    override fun getCount() = mImages.size

    override fun isViewFromObject(view: View, `object`: Any) =
        view === `object` as LinearLayout

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return LayoutInflater.from(container.context)
            .inflate(R.layout.tour_image_layout, container, false).apply {
            findViewById<ImageView>(R.id.imageTour).setImageResource(mImages[position])
            findViewById<TextView>(R.id.tour_text_1).text = barTitles[position]
            findViewById<TextView>(R.id.tour_text_2).text = barTexts[position]
            (container as ViewPager).addView(this)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        (container as ViewPager).removeView(`object` as LinearLayout)
    }
}