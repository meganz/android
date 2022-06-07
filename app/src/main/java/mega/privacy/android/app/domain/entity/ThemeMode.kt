package mega.privacy.android.app.domain.entity

/**
 * Theme mode
 */
enum class ThemeMode {
    /**
     * Use Light theme regardless of system settings
     */
    Light,

    /**
     * Use Dark theme regardless of system settings
     */
    Dark,

    /**
     * Use Light/Dark theme based on system settings
     */
    System;

    companion object {
        val DEFAULT = System
    }
}
