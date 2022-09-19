package mega.privacy.android.app.imageviewer.util

import android.view.View
import androidx.annotation.NonNull
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Fade out ViewPager2 Page Transformer
 */
class FadeOutPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(@NonNull page: View, position: Float) {
        page.translationX = -position * page.width
        page.alpha = 1 - abs(position)
    }
}
