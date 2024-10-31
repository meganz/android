package mega.privacy.android.feature.sync.ui.views

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.R
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1980dp-xhdpi")
class SyncCardTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that required components are visible for a collapsed Sync card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_TWOWAY,
                    folderPairName = "Sync Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = false,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_two_way))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_device_storage))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_mega_storage))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_added))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_content))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_total_size))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that required components are visible for an expanded Sync card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_TWOWAY,
                    folderPairName = "Sync Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = false,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = true,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_two_way))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_device_storage))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_mega_storage))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_added))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_content))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_total_size))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that device folder path is clickable for an expanded Sync card`() {
        var clicked = false
        val deviceStoragePath = "Device Path"
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_TWOWAY,
                    folderPairName = "Sync Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = deviceStoragePath,
                    hasStalledIssues = false,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = true,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = { clicked = true },
                isLowBatteryLevel = false,
                isFreeAccount = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }
        composeTestRule.onNodeWithText(deviceStoragePath).performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that required components are visible for a collapsed Backup card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_BACKUP,
                    folderPairName = "Backup Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = false,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_add_new_backup_card_sync_type_text))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_device_storage))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_mega_storage))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_added))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_content))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_total_size))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(mega.privacy.android.shared.resources.R.string.sync_stop_backup_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that required components are visible for an expanded Backup card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_BACKUP,
                    folderPairName = "Backup Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = false,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = true,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                isLowBatteryLevel = false,
                isFreeAccount = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_add_new_backup_card_sync_type_text))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_device_storage))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_folders_mega_storage))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_added))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_content))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.info_total_size))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(mega.privacy.android.shared.resources.R.string.sync_stop_backup_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that device folder path is clickable for an expanded Backup card`() {
        var clicked = false
        val deviceStoragePath = "Device Path"
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_BACKUP,
                    folderPairName = "Backup Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = deviceStoragePath,
                    hasStalledIssues = false,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = true,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = { clicked = true },
                isLowBatteryLevel = false,
                isFreeAccount = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }
        composeTestRule.onNodeWithText(deviceStoragePath).performClick()
        assertThat(clicked).isTrue()
    }
}