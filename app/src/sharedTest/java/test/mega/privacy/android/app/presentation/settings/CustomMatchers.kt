package test.mega.privacy.android.app.presentation.settings

import android.view.View
import android.widget.TextView
import androidx.core.graphics.alpha
import androidx.preference.R
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import kotlin.math.ceil


/**
 * A helper function that returns a view matcher for Espresso tests that matches a view
 * containing text with the specified alpha value
 *
 * @param expectedAlpha expressed as a double value from 0.0 to 1.0
 * @return the View matcher
 */
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
            description?.appendText("Expected alpha of $expectedAlpha, but found ${fullAlpha / (item as TextView).currentTextColor.alpha}")
        }
    }
}

/**
 * Helper function to match views on a preference screen.
 *
 */
fun onPreferences(): ViewInteraction = Espresso.onView(ViewMatchers.withId(R.id.recycler_view))