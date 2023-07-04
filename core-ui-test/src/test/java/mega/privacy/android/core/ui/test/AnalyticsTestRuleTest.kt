package mega.privacy.android.core.ui.test

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.Analytics
import mega.privacy.mobile.analytics.core.event.identifier.DialogDisplayedEventIdentifier
import mega.privacy.mobile.analytics.core.event.identifier.ScreenViewEventIdentifier
import org.junit.Rule
import org.junit.Test

internal class AnalyticsTestRuleTest {
    @get:Rule
    val underTest = AnalyticsTestRule()

    @Test
    internal fun `test that events are captured`() {
        val expected = object : ScreenViewEventIdentifier {
            override val eventName: String
                get() = ""
            override val uniqueIdentifier: Int
                get() = 1
        }

        Analytics.tracker.trackEvent(expected)

        assertThat(underTest.events).contains(expected)
    }


    @Test
    internal fun `test that events are listed in order`() {
        val intRange = Array(5) { it }
        intRange.map {
            val event = object : DialogDisplayedEventIdentifier {
                override val eventName = it.toString()
                override val uniqueIdentifier = it
                override val dialogName: String
                    get() = ""
                override val screenName: String?
                    get() = null
            }
            Analytics.tracker.trackEvent(event)
        }

        assertThat(underTest.events.map { it.uniqueIdentifier }).containsExactly(*intRange)
    }
}