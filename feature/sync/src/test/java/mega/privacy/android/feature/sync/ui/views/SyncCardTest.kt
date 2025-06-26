package mega.privacy.android.feature.sync.ui.views

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.R
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import mega.privacy.mobile.analytics.event.SyncCardIssuesInfoButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardOpenDeviceFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardOpenMegaFolderButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardPauseRunButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncCardStopButtonPressedEvent
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsNotDisplayed()
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that Sync card buttons are clickable`() {
        var clicked = false
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_TWOWAY,
                    folderPairName = "Sync Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = { clicked = true },
                removeFolderClicked = { clicked = true },
                issuesInfoClicked = { clicked = true },
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = { clicked = true },
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
    }

    @Test
    fun `test that Sync card resume button is not clickable if the account is over quota`() {
        var clicked = false
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_TWOWAY,
                    folderPairName = "Sync Name",
                    status = SyncStatus.ERROR,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = { clicked = true },
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = true,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_run_sync))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `test that tap Sync card buttons send the right analytics tracker event`() {
        val deviceStoragePath = "Device Path"
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_TWOWAY,
                    folderPairName = "Sync Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = deviceStoragePath,
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = true,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(deviceStoragePath).performClick()
        assertThat(analyticsRule.events).contains(SyncCardOpenDeviceFolderButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardIssuesInfoButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardOpenMegaFolderButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardPauseRunButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardStopButtonPressedEvent)
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_backup_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsNotDisplayed()
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_backup_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that Backup card buttons are clickable`() {
        var clicked = false
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_BACKUP,
                    folderPairName = "Sync Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = { clicked = true },
                removeFolderClicked = { clicked = true },
                issuesInfoClicked = { clicked = true },
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = { clicked = true },
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_backup_button))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
    }

    @Test
    fun `test that Backup card resume button is not clickable if the account is over quota`() {
        var clicked = false
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_BACKUP,
                    folderPairName = "Sync Name",
                    status = SyncStatus.ERROR,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = { clicked = true },
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = true,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_run_sync))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `test that tap Backup card buttons send the right analytics tracker event`() {
        val deviceStoragePath = "Device Path"
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_BACKUP,
                    folderPairName = "Sync Name",
                    status = SyncStatus.SYNCING,
                    deviceStoragePath = deviceStoragePath,
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = true,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(deviceStoragePath).performClick()
        assertThat(analyticsRule.events).contains(SyncCardOpenDeviceFolderButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_sync_issues_info))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardIssuesInfoButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardOpenMegaFolderButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardPauseRunButtonPressedEvent)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_backup_button))
            .assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(SyncCardStopButtonPressedEvent)
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }
        composeTestRule.onNodeWithText(deviceStoragePath).performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that required components are visible for a collapsed CU card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_CAMERA_UPLOADS,
                    folderPairName = "Camera Uploads",
                    status = SyncStatus.SYNCED,
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_camera_uploads))
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsDisplayed()
    }

    @Test
    fun `test that required components are visible for an expanded CU card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_CAMERA_UPLOADS,
                    folderPairName = "Camera Uploads",
                    status = SyncStatus.SYNCED,
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_camera_uploads))
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsDisplayed()
    }

    @Test
    fun `test that CU card buttons are clickable`() {
        var clicked = false
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_CAMERA_UPLOADS,
                    folderPairName = "Camera Uploads",
                    status = SyncStatus.SYNCED,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = { clicked = true },
                onCameraUploadsSettingsClicked = { clicked = true },
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
    }

    @Test
    fun `test that device folder path is clickable for an expanded CU card`() {
        var clicked = false
        val deviceStoragePath = "Device Path"
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_CAMERA_UPLOADS,
                    folderPairName = "Camera Uploads",
                    status = SyncStatus.SYNCED,
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }
        composeTestRule.onNodeWithText(deviceStoragePath).performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that required components are visible for a collapsed MU card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_MEDIA_UPLOADS,
                    folderPairName = "Media Uploads",
                    status = SyncStatus.SYNCED,
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_media_uploads))
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsDisplayed()
    }

    @Test
    fun `test that required components are visible for an expanded MU card`() {
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_MEDIA_UPLOADS,
                    folderPairName = "Media Uploads",
                    status = SyncStatus.SYNCED,
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_media_uploads))
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
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sync_card_pause_sync))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_stop_sync_button))
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsDisplayed()
    }

    @Test
    fun `test that MU card buttons are clickable`() {
        var clicked = false
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_MEDIA_UPLOADS,
                    folderPairName = "Media Uploads",
                    status = SyncStatus.SYNCED,
                    deviceStoragePath = "Device Path",
                    hasStalledIssues = true,
                    megaStoragePath = "MEGA Path",
                    megaStorageNodeId = NodeId(1111L),
                    expanded = false,
                ),
                expandClicked = {},
                pauseRunClicked = {},
                removeFolderClicked = {},
                issuesInfoClicked = {},
                onOpenDeviceFolderClicked = {},
                onOpenMegaFolderClicked = { clicked = true },
                onCameraUploadsSettingsClicked = { clicked = true },
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_open_button))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_settings))
            .assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
        clicked = false
    }

    @Test
    fun `test that device folder path is clickable for an expanded MU card`() {
        var clicked = false
        val deviceStoragePath = "Device Path"
        composeTestRule.setContent {
            SyncCard(
                sync = SyncUiItem(
                    id = 1L,
                    syncType = SyncType.TYPE_MEDIA_UPLOADS,
                    folderPairName = "Media Uploads",
                    status = SyncStatus.SYNCED,
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
                onOpenMegaFolderClicked = {},
                onCameraUploadsSettingsClicked = {},
                isLowBatteryLevel = false,
                isStorageOverQuota = false,
                errorRes = null,
                deviceName = "Device Name",
            )
        }
        composeTestRule.onNodeWithText(deviceStoragePath).performClick()
        assertThat(clicked).isTrue()
    }
}