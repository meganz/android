package mega.privacy.android.app.presentation.settings.compose.appearance.navigation

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.compose.appearance.AppearanceSettings
import mega.privacy.android.navigation.contract.settings.FeatureSettingEntryPoint
import mega.privacy.mobile.analytics.event.AppearanceSettingsItemSelectedEvent

/**
 * Appearance setting entry point
 */
val appearanceSettingEntryPoint = FeatureSettingEntryPoint(
    key = "appearance",
    title = R.string.settings_appearance,
    icon = iconPackR.drawable.ic_palette,
    preferredOrdinal = 10,
    destination = AppearanceSettings,
    analyticsEvent = AppearanceSettingsItemSelectedEvent
)