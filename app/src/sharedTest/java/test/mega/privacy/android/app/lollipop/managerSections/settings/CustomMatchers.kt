package test.mega.privacy.android.app.lollipop.managerSections.settings

import android.view.View
import android.widget.TextView
import androidx.core.graphics.alpha
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import kotlin.math.ceil

fun withTextColorAlpha(expectedAlpha: Double): Matcher<View?> {
    val fullAlpha = 255
    return object : BoundedMatcher<View?, TextView>(TextView::class.java) {
        override fun matchesSafely(textView: TextView): Boolean {
            return textView.currentTextColor.alpha == ceil(expectedAlpha * fullAlpha).toInt()
        }

        override fun describeTo(description: Description) {
            description.appendText("with text alpha: ")
            description.appendValue(expectedAlpha)
        }

        override fun describeMismatch(item: Any?, description: Description?) {
            super.describeMismatch(item, description)
            description?.appendText("Expected alpha of $expectedAlpha, but found ${fullAlpha/(item as TextView).currentTextColor.alpha}" )
        }
    }
}