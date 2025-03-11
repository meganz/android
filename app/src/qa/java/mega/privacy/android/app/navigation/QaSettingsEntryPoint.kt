package mega.privacy.android.app.navigation

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.QASettingsHome
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.mobile.analytics.event.QASettingsItemSelectedEvent

val qaSettingsEntryPoint = FeatureSettingEntryPoint(
    key = "qa",
    title = R.string.settings_qa,
    icon = R.drawable.ic_scheduled_meeting_edit,
    preferredOrdinal = -1,
    destination = QASettingsHome,
    analyticsEvent = QASettingsItemSelectedEvent,
)