package mega.privacy.android.app

import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.LinkedList

/**
 * Analytics Test Extension for JUnit 5
 *
 * @constructor
 * @param tracker
 *
 * @property trackedEvents
 */
class AnalyticsTestExtension(
    private val tracker: AnalyticsTracker? = null,
) : AfterEachCallback, AfterAllCallback, BeforeEachCallback, BeforeAllCallback {

    private var initialTracker: AnalyticsTracker? = null
    private val testTracker = tracker ?: initTestTracker()
    private val trackedEvents = LinkedList<EventIdentifier>()

    /**
     * List of tracked events
     */
    val events: Set<EventIdentifier>
        get() = trackedEvents.toSet()


    private fun initTestTracker() = object : AnalyticsTracker {
        override fun trackEvent(eventIdentifier: EventIdentifier) {
            trackedEvents.add(eventIdentifier)
        }
    }

    override fun beforeEach(context: ExtensionContext) {
        trackedEvents.clear()
    }

    override fun afterEach(context: ExtensionContext) {
        trackedEvents.clear()
    }

    override fun beforeAll(context: ExtensionContext) {
        initialTracker = runCatching { Analytics.tracker }.getOrNull()
        Analytics.initialise(testTracker)
    }

    override fun afterAll(context: ExtensionContext) {
        Analytics.initialise(initialTracker)
    }
}