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
     * Display over other apps permission.
     */
    DisplayOverOtherApps,

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
    Calls;

    companion object {
        /**
         * Default permission.
         */
        val DEFAULT = Media
    }
}