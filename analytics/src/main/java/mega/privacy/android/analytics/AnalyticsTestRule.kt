package mega.privacy.android.analytics

import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
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
    val events = LinkedList<EventIdentifier>()

    init {
        testTracker = tracker ?: initTestTracker()
    }

    private fun initTestTracker() = object : AnalyticsTracker {
        override fun trackEvent(eventIdentifier: EventIdentifier) {
            events.add(eventIdentifier)
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

}



