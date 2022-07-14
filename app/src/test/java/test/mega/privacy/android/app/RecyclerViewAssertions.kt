package test.mega.privacy.android.app

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert

class RecyclerViewAssertions {
    companion object{

        /**
         * Provides a RecyclerView assertion based on a view matcher. This allows you to
         * validate whether a RecyclerView contains a row in memory without scrolling the list.
         *
         * @param viewMatcher - an Espresso ViewMatcher for a descendant of any row in the recycler.
         * @return an Espresso ViewAssertion to check against a RecyclerView.
         */
        fun withRowContaining(viewMatcher: Matcher<View?>): ViewAssertion {
            return ViewAssertion { view, noViewException ->
                if (noViewException != null) {
                    throw noViewException
                }
                Assert.assertTrue(view is RecyclerView)
                val recyclerView = view as RecyclerView
                val adapter = recyclerView.adapter ?: return@ViewAssertion
                for (position in 0 until adapter.itemCount) {
                    val itemType = adapter.getItemViewType(position)
                    val viewHolder = adapter.createViewHolder(recyclerView, itemType)
                    adapter.bindViewHolder(viewHolder, position)
                    if (ViewMatchers.hasDescendant(viewMatcher).onViewHolder().matches(
                            viewHolder
                        )
                    ) {
                        return@ViewAssertion  // Found a matching row
                    }
                }
                throw NoMatchingViewException.Builder()
                    .withRootView(view)
                    .withViewMatcher(viewMatcher)
                    .build()
            }
        }

        /**
         * Provides a RecyclerView assertion based on a view matcher. This allows you to
         * validate whether a RecyclerView does not contain a row in memory without scrolling the list.
         *
         * @param viewMatcher - an Espresso ViewMatcher for a descendant of any row in the recycler.
         * @return an Espresso ViewAssertion to check against a RecyclerView.
         */
        fun withNoRowContaining(viewMatcher: Matcher<View?>): ViewAssertion {
            return ViewAssertion { view, noViewFoundException ->
                if (noViewFoundException != null) {
                    throw noViewFoundException
                }
                Assert.assertTrue(view is RecyclerView)
                val recyclerView = view as RecyclerView
                val adapter = recyclerView.adapter ?: return@ViewAssertion
                for (position in 0 until adapter.itemCount) {
                    val itemType = adapter.getItemViewType(position)
                    val viewHolder = adapter.createViewHolder(recyclerView, itemType)
                    adapter.bindViewHolder(viewHolder, position)
                    if (ViewMatchers.hasDescendant(viewMatcher).onViewHolder().matches(
                            viewHolder
                        )
                    ) {
                        Assert.fail("Found matching view where none expected")
                    }
                }
                return@ViewAssertion  // Found no matching row
            }
        }


        /**
         * On view holder
         *
         * converts a view matcher to match views on a viewHolder
         *
         * @return  matcher as ViewHolder matcher
         */
        fun Matcher<View>.onViewHolder(): Matcher<RecyclerView.ViewHolder> {
            return object : TypeSafeMatcher<RecyclerView.ViewHolder>() {
                override fun matchesSafely(viewHolder: RecyclerView.ViewHolder): Boolean {
                    return this@onViewHolder.matches(viewHolder.itemView)
                }

                override fun describeTo(description: Description) {
                    description.appendText("ViewHolder with view: ")
                    this@onViewHolder.describeTo(description)
                }
            }
        }
    }
}