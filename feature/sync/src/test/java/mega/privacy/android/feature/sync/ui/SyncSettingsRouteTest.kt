package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.feature.sync.R as syncR
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.settings.SettingSyncScreen
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncUiState
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1980dp-xhdpi")
class SyncSettingsRouteTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that sync connection type view displays WiFi only option`() {
        composeTestRule.setContent {
            SettingSyncScreen(
                uiState = SettingsSyncUiState(
                    syncConnectionType = SyncConnectionType.WiFiOnly,
                    syncDebrisSizeInBytes = 1024L,
                    showSyncFrequency = true,
                    syncFrequency = SyncFrequency.EVERY_15_MINUTES,
                    snackbarMessage = null
                ),
                syncDebrisCleared = {},
                syncConnectionTypeSelected = {},
                syncFrequencySelected = {},
                snackbarShown = {}
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(syncR.string.sync_dialog_message_wifi_only))
            .assertIsDisplayed()
    }

    @Test
    fun `test that sync connection type view displays WiFi or Mobile Data option`() {
        composeTestRule.setContent {
            SettingSyncScreen(
                uiState = SettingsSyncUiState(
                    syncConnectionType = SyncConnectionType.WiFiOrMobileData,
                    syncDebrisSizeInBytes = 1024L,
                    showSyncFrequency = true,
                    syncFrequency = SyncFrequency.EVERY_15_MINUTES,
                    snackbarMessage = null
                ),
                syncDebrisCleared = {},
                syncConnectionTypeSelected = {},
                syncFrequencySelected = {},
                snackbarShown = {}
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(syncR.string.sync_dialog_message_wifi_or_mobile_data))
            .assertIsDisplayed()
    }

    @Test
    fun `test that debris row is shown`() {
        composeTestRule.setContent {
            SettingSyncScreen(
                uiState = SettingsSyncUiState(
                    syncConnectionType = SyncConnectionType.WiFiOnly,
                    syncDebrisSizeInBytes = 0L,
                    showSyncFrequency = true,
                    syncFrequency = SyncFrequency.EVERY_15_MINUTES,
                    snackbarMessage = null
                ),
                syncDebrisCleared = {},
                syncConnectionTypeSelected = {},
                syncFrequencySelected = {},
                snackbarShown = {}
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.settings_sync_clear_debris_item_title))
            .assertIsDisplayed()
    }
}
