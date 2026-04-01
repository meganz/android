package mega.privacy.android.analytics.decorator

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.navigation.contract.metadata.NavEntryMetadataScope
import mega.privacy.mobile.analytics.core.event.identifier.DialogDisplayedEventIdentifier
import mega.privacy.mobile.analytics.core.event.identifier.ScreenViewEventIdentifier
import timber.log.Timber
import java.lang.reflect.Field

private const val SCREEN_EVENT_KEY = "screen_event"

private const val DIALOG_EVENT_KEY = "dialog_event"

/**
 * Registers a [ScreenViewEventIdentifier] so that the analytics decorator
 * will track a screen view event when this entry is displayed.
 */
fun NavEntryMetadataScope.withScreenViewEvent(event: ScreenViewEventIdentifier) {
    set(SCREEN_EVENT_KEY, event)
}

/**
 * Registers a [DialogDisplayedEventIdentifier] so that the analytics decorator
 * will track a dialog displayed event when this entry is shown.
 */
fun NavEntryMetadataScope.withDialogEvent(event: DialogDisplayedEventIdentifier) {
    set(DIALOG_EVENT_KEY, event)
}

/**
 * Returns an [AnalyticNavEntryDecorator] that is remembered across recompositions.
 *
 * This decorator logs route open and close events for navigation analytics.
 */
@Composable
fun <T : Any> rememberAnalyticNavEntryDecorator(): AnalyticNavEntryDecorator<T> {
    return remember {
        AnalyticNavEntryDecorator()
    }
}

/**
 * Decorator that logs route open and close events for navigation analytics.
 *
 * This decorator tracks when routes are opened (when [decorate] is called) and
 * when routes are closed (when [onPop] is called).
 *
 * @param T The type of the navigation key
 */
class AnalyticNavEntryDecorator<T : Any> : NavEntryDecorator<T>(
    onPop = { key ->
        // No operation on pop for now
    },
    decorate = { entry ->
        entry.Content()
        logRouteOpen(entry)
        logFirebaseScreenView(entry, Firebase.analytics)
    },
)

/**
 * Logs a route open event for the given [NavEntry].
 *
 * If the entry's metadata contains a [ScreenViewEventIdentifier] under the
 * [SCREEN_EVENT_KEY], it is tracked using the Analytics tracker.
 *
 * Ìf the entry's metadata contains a [DialogDisplayedEventIdentifier] under the
 * [DIALOG_EVENT_KEY], it is also tracked using the Analytics tracker.
 *
 * @param entry The navigation entry for which to log the route open event
 */
private fun <T : Any> logRouteOpen(entry: NavEntry<T>) {
    (entry.metadata[SCREEN_EVENT_KEY] as? ScreenViewEventIdentifier)?.let { screenViewEvent ->
        Analytics.tracker.trackEvent(screenViewEvent)
    }

    (entry.metadata[DIALOG_EVENT_KEY] as? DialogDisplayedEventIdentifier)?.let { dialogDisplayedEvent ->
        Analytics.tracker.trackEvent(dialogDisplayedEvent)
    }
}

/**
 * Cached reflection field for accessing [NavEntry.key], which is private.
 * This is needed to get the nav key's class name for Firebase screen tracking.
 */
private val navEntryKeyField: Field? by lazy {
    runCatching {
        NavEntry::class.java.getDeclaredField("key").apply { isAccessible = true }
    }.getOrNull()
}

/**
 * Logs a screen view event to Firebase Analytics for the given [NavEntry].
 *
 * This enables Firebase Crashlytics to associate crashes with the current screen,
 * similar to how Activities and Fragments automatically report screen views.
 * The nav key class name (e.g., `AchievementMain`) is used as the screen name.
 *
 * Note: All NavKey classes are annotated with `@Serializable`, and ProGuard is
 * configured to keep names of serializable classes, so `simpleName` will return
 * the real class name in release builds.
 *
 * @param entry The navigation entry for which to log the screen view
 * @param firebaseAnalytics The Firebase Analytics instance
 */
private fun <T : Any> logFirebaseScreenView(
    entry: NavEntry<T>,
    firebaseAnalytics: FirebaseAnalytics,
) {
    val screenName = navEntryKeyField?.get(entry)?.let { it::class.simpleName } ?: return

    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
    })
}
