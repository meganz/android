package mega.privacy.android.app.presentation.settings.compose.navigation

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.settings.FeatureSettingEntryPoint
import mega.privacy.android.navigation.settings.FeatureSettings
import mega.privacy.android.navigation.settings.MoreSettingEntryPoint
import mega.privacy.mobile.analytics.event.AboutSettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.AdvancedSettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.CameraUploadsSettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.ChatAndMeetingsSettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.FileManagementSettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.HelpAndFeedbackSettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.MediaSettingsItemSelectedEvent
import mega.privacy.mobile.analytics.event.SyncSettingsItemSelectedEvent

internal val appFeatureSettingsEntryPoints = setOf<FeatureSettingEntryPoint>(

    FeatureSettingEntryPoint(
        key = "file_management",
        title = R.string.settings_file_management_category,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 20,
        destination = FakeSetting1,
        analyticsEvent = FileManagementSettingsItemSelectedEvent,
    ),
    FeatureSettingEntryPoint(
        key = "camera_uploads",
        title = R.string.settings_camera_upload_on,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 30,
        destination = FakeSetting1,
        analyticsEvent = CameraUploadsSettingsItemSelectedEvent,
    ),
    FeatureSettingEntryPoint(
        key = "sync",
        title = mega.privacy.android.shared.resources.R.string.settings_section_sync,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 40,
        destination = FakeSetting1,
        analyticsEvent = SyncSettingsItemSelectedEvent,
    ),
    FeatureSettingEntryPoint(
        key = "media",
        title = R.string.settings_media,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 50,
        destination = FakeSetting1,
        analyticsEvent = MediaSettingsItemSelectedEvent,
    ),
    FeatureSettingEntryPoint(
        key = "chat_and_meetings",
        title = R.string.settings_chat_vibration,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 60,
        destination = FakeSetting1,
        analyticsEvent = ChatAndMeetingsSettingsItemSelectedEvent,
    ),
    FeatureSettingEntryPoint(
        key = "advanced",
        title = R.string.settings_advanced_features,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 80,
        destination = FakeSetting1,
        analyticsEvent = AdvancedSettingsItemSelectedEvent,
    ),
)


internal val appMoreSettingsEntryPoints = setOf<MoreSettingEntryPoint>(
    MoreSettingEntryPoint(
        key = "help_and_feedback",
        title = R.string.settings_help,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 10,
        destination = FakeSetting2,
        analyticsEvent = HelpAndFeedbackSettingsItemSelectedEvent,
    ),
    MoreSettingEntryPoint(
        key = "about",
        title = R.string.settings_about,
        icon = iconPackR.drawable.ic_gear_six_medium_thin_outline,
        preferredOrdinal = 20,
        destination = FakeSetting2,
        analyticsEvent = AboutSettingsItemSelectedEvent,
    ),
)

@Serializable
@Parcelize
data object FakeSetting1 : Parcelable

fun NavGraphBuilder.fakeSettings(
    navHostController: NavHostController,
) {
    composable<FakeSetting1> { backStackEntry ->
        FakeSettingsView("Fake settings screen 1")
    }
}

@Serializable
@Parcelize
data object FakeSetting2 : Parcelable

fun NavGraphBuilder.fakeSettings2(
    navHostController: NavHostController,
) {
    composable<FakeSetting2> { backStackEntry ->
        FakeSettingsView("Fake settings screen 2")
    }
}

val appFeatureSettings = setOf<FeatureSettings>(
    object : FeatureSettings {
        override val settingsNavGraph = NavGraphBuilder::fakeSettings
    },
    object : FeatureSettings {
        override val settingsNavGraph = NavGraphBuilder::fakeSettings2
    },

    )

@Composable
fun FakeSettingsView(
    settingKey: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        MegaText("Selected setting: $settingKey", textColor = TextColor.Warning)
    }
}