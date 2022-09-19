package mega.privacy.android.app.presentation.permissions.model

/**
 * Enum class defining permissions requested in the onboarding.
 */
enum class PermissionScreen {
    /**
     * Notifications permission.
     */
    Notifications,

    /**
     * Media permission.
     */
    Media,

    /**
     * Camera permission.
     */
    Camera,

    /**
     * Calls permission.
     */
    Calls,

    /**
     * Contacts permission.
     */
    Contacts;

    companion object {
        /**
         * Default permission.
         */
        val DEFAULT = Media
    }
}