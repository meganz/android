package mega.privacy.android.feature.sync.ui.views

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
@RunWith(AndroidJUnit4::class)
class SyncPermissionWarningBannerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val syncPermissionsManager: SyncPermissionsManager = mock()

    @Test
    fun `test that storage permission banner is displayed when allFileAccess is false`() {
        whenever(syncPermissionsManager.isSDKAboveOrEqualToR()).thenReturn(true)
        whenever(syncPermissionsManager.isManageExternalStoragePermissionGranted()).thenReturn(false)
        composeTestRule.setContent {
            SyncPermissionWarningBanner(
                syncPermissionsManager = syncPermissionsManager,
                isDisableBatteryOptimizationEnabled = false,
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.sync_storage_permission_banner)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that battery optimization banner is displayed when hasUnrestrictedBatteryUsage is false`() {
        whenever(syncPermissionsManager.isSDKAboveOrEqualToR()).thenReturn(true)
        whenever(syncPermissionsManager.isManageExternalStoragePermissionGranted()).thenReturn(true)
        whenever(syncPermissionsManager.isDisableBatteryOptimizationGranted()).thenReturn(false)
        composeTestRule.setContent {
            SyncPermissionWarningBanner(
                syncPermissionsManager = syncPermissionsManager,
                isDisableBatteryOptimizationEnabled = true,
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.sync_battery_optimisation_banner)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that clicking storage permission banner triggers the correct action`() {
        whenever(syncPermissionsManager.isSDKAboveOrEqualToR()).thenReturn(true)
        whenever(syncPermissionsManager.isManageExternalStoragePermissionGranted()).thenReturn(false)

        composeTestRule.setContent {
            SyncPermissionWarningBanner(
                syncPermissionsManager = syncPermissionsManager,
                isDisableBatteryOptimizationEnabled = false
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.sync_storage_permission_banner)
        ).performClick()

        verify(syncPermissionsManager).launchAppSettingFileStorageAccess()
    }

    @Test
    fun `test that clicking battery optimization banner triggers the correct action`() {
        whenever(syncPermissionsManager.isSDKAboveOrEqualToR()).thenReturn(true)
        whenever(syncPermissionsManager.isManageExternalStoragePermissionGranted()).thenReturn(true)
        whenever(syncPermissionsManager.isDisableBatteryOptimizationGranted()).thenReturn(false)

        composeTestRule.setContent {
            SyncPermissionWarningBanner(
                syncPermissionsManager = syncPermissionsManager,
                isDisableBatteryOptimizationEnabled = true
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.sync_battery_optimisation_banner)
        ).performClick()

        verify(syncPermissionsManager).launchAppSettingBatteryOptimisation()
    }
}
