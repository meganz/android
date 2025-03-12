package mega.privacy.android.app.presentation.settings.compose.security.navigation

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.compose.security.home.SecuritySettings
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.mobile.analytics.event.SecuritySettingsItemSelectedEvent

/**
 * Security setting entry point
 */
val securitySettingEntryPoint = FeatureSettingEntryPoint(
    key = "security",
    title = R.string.settings_security_options_title,
    icon = iconPackR.drawable.ic_lock,
    preferredOrdinal = 70,
    destination = SecuritySettings,
    analyticsEvent = SecuritySettingsItemSelectedEvent,
)