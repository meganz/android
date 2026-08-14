package mega.privacy.android.navigation.payment

/**
 * How the subscription offer landing screen was opened. Reported to analytics, which distinguishes
 * the automatic post-login launch from a launch the user triggered themselves.
 */
enum class SubscriptionOfferSource {
    /**
     * Opened automatically after login, when an offer is active and the reshow interval has elapsed.
     */
    AutoOpen,

    /**
     * Opened from the offer banner in the Home banner carousel.
     */
    HomeBanner,

    /**
     * Opened from the offer banner on the Menu screen.
     */
    MenuBanner,

    /**
     * Opened from a promo notification, either in the notifications list or as a push, through the
     * `mega://upgrade?offer=1` deep link.
     */
    Notification,
}
