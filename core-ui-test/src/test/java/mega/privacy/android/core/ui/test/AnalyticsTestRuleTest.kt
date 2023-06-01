package mega.privacy.android.core.ui.test

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.event.DialogInfo
import mega.privacy.android.analytics.event.ScreenInfo
import org.junit.Rule
import org.junit.Test

internal class AnalyticsTestRuleTest {
    @get:Rule
    val underTest = AnalyticsTestRule()

    @Test
    internal fun `test that events are captured`() {
        val expected = object : ScreenInfo {
            override val name: String
                get() = ""
            override val uniqueIdentifier: Int
                get() = 1
        }

        Analytics.tracker.trackScreenView(expected)

        assertThat(underTest.events.map { it.info }).contains(expected)
    }


    @Test
    internal fun `test that events are listed in order`() {
        val intRange = Array(5) { it }
        intRange.map {
            val event = object : DialogInfo {
                override val name = it.toString()
                override val uniqueIdentifier = it
            }
            Analytics.tracker.trackDialogDisplayed(event)
        }

        assertThat(underTest.events.map { it.info.uniqueIdentifier }).containsExactly(*intRange)
    }
}