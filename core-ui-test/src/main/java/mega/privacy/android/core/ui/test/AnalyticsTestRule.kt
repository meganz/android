package mega.privacy.android.core.ui.test

import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.event.AnalyticsInfo
import mega.privacy.android.analytics.event.ButtonInfo
import mega.privacy.android.analytics.event.DialogInfo
import mega.privacy.android.analytics.event.ScreenInfo
import mega.privacy.android.analytics.event.TabInfo
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.LinkedList

/**
 * Analytics test rule
 *
 * @constructor
 * @param tracker
 *
 * @property events
 */
class AnalyticsTestRule(tracker: AnalyticsTracker? = null) : TestRule {
    private val testTracker: AnalyticsTracker
    val events = LinkedList<AnalyticsCall<AnalyticsInfo>>()

    init {
        testTracker = tracker ?: initTestTracker()
    }

    private fun initTestTracker() = object : AnalyticsTracker {
        override fun trackScreenView(screen: ScreenInfo) {
            events.add(AnalyticsCall(screen))
        }

        override fun trackTabSelected(tab: TabInfo) {
            events.add(AnalyticsCall(tab))
        }

        override fun trackDialogDisplayed(dialog: DialogInfo, screen: ScreenInfo) {
            events.add(AnalyticsCall(dialog, listOf(screen)))
        }

        override fun trackDialogDisplayed(dialog: DialogInfo) {
            events.add(AnalyticsCall(dialog))
        }

        override fun trackButtonPress(button: ButtonInfo) {
            events.add(AnalyticsCall(button))
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val current = runCatching { Analytics.tracker }.getOrNull()

                Analytics.initialise(testTracker)
                events.clear()

                try {
                    base.evaluate()
                } finally {
                    Analytics.initialise(current)
                }
            }

        }
    }

    /**
     * Analytics call
     *
     * @param T
     * @property info
     * @property additionalInfo
     * @constructor Create empty Analytics call
     */
    data class AnalyticsCall<out T : AnalyticsInfo>(
        val info: T,
        val additionalInfo: List<AnalyticsInfo> = emptyList(),
    )
}



