package mega.privacy.android.navigation.contract.navkey

/**
 * No session nav key
 *
 * Use this interface for all destinations that do not require a user to be logged in.
 */
sealed interface NoSessionNavKey : NoNodeNavKey {

    /**
     * We can still navigate to this screen if the user is logged out
     */
    interface Optional : NoSessionNavKey

    /**
     * If the user is already logged in, navigating to this screen will take you to the
     *  current logged in screen or the default landing screen if none is present
     */
    interface Mandatory : NoSessionNavKey
}