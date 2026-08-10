package mega.privacy.android.domain.entity.analytics

/**
 * Catalog of events sent to Firebase Analytics
 *
 * These events are consumed by Firebase A/B Testing experiments; regular product analytics
 * should keep using the MEGA analytics tracker.
 *
 * @property eventName Name of the event as reported to Firebase Analytics
 */
enum class FirebaseAnalyticsEvent(val eventName: String) {

    /**
     * A new account was created and its email verified
     */
    CreateNewAccount("create_new_account"),
}
