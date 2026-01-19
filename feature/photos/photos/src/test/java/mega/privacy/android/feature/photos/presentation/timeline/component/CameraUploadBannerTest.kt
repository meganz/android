package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.shared.resources.R as SharedR
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CameraUploadBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val analyticsTracker: AnalyticsTracker = mock()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Analytics.initialise(analyticsTracker)
    }

    @Test
    fun `test that the enable CU banner is displayed when should notify users`() {
        composeRuleScope {
            setBanner(
                status = CUStatusUiState.Disabled(shouldNotifyUser = true),
            )

            onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the enable CU banner is not displayed when shouldn't notify users`() {
        composeRuleScope {
            setBanner(status = CUStatusUiState.Disabled(shouldNotifyUser = false))

            onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_TEST_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the enable CU banner is successfully dismissed`() {
        val status = CUStatusUiState.Disabled(shouldNotifyUser = true)
        composeRuleScope {
            val onDismissRequest = mock<(status: CUStatusUiState) -> Unit>()
            setBanner(
                status = status,
                onDismissRequest = onDismissRequest
            )

            onNodeWithTag(TIMELINE_ENABLE_CAMERA_UPLOADS_BANNER_DISMISS_ICON_TEST_TAG).performClick()

            verify(onDismissRequest).invoke(status)
        }
    }

    @Test
    fun `test that the no full access banner is displayed`() {
        composeRuleScope {
            setBanner(status = CUStatusUiState.Warning.HasLimitedAccess)

            onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NO_FULL_ACCESS_BANNER_TEST_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the permission is successfully changed`() {
        composeRuleScope {
            val onChangeCameraUploadsPermissions = mock<() -> Unit>()
            setBanner(
                status = CUStatusUiState.Warning.HasLimitedAccess,
                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions
            )

            val text =
                context.getString(SharedR.string.timeline_tab_cu_permission_warning_banner_action)
            onNodeWithText(text).performClick()

            verify(onChangeCameraUploadsPermissions).invoke()
        }
    }

    @Test
    fun `test that the low battery banner is displayed`() {
        composeRuleScope {
            setBanner(status = CUStatusUiState.Warning.BatteryLevelTooLow)

            onNodeWithTag(TIMELINE_CAMERA_UPLOADS_LOW_BATTERY_BANNER_TEST_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the device charging not met banner is displayed`() {
        composeRuleScope {
            setBanner(status = CUStatusUiState.Warning.DeviceChargingRequirementNotMet)

            onNodeWithTag(TIMELINE_CAMERA_UPLOADS_DEVICE_CHARGING_NOT_MET_BANNER_TEST_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the network requirement not met banner is displayed`() {
        composeRuleScope {
            setBanner(status = CUStatusUiState.Warning.NetworkConnectionRequirementNotMet)

            onNodeWithTag(TIMELINE_CAMERA_UPLOADS_NETWORK_REQUIREMENT_NOT_MET_PAUSED_BANNER_TEST_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the full storage banner is displayed`() {
        composeRuleScope {
            setBanner(status = CUStatusUiState.Warning.AccountStorageOverQuota)

            onNodeWithTag(TIMELINE_CAMERA_UPLOADS_FULL_STORAGE_BANNER_TEST_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the user is navigated to the upgrade screen`() {
        composeRuleScope {
            val onNavigateUpgradeScreen = mock<() -> Unit>()
            setBanner(
                status = CUStatusUiState.Warning.AccountStorageOverQuota,
                onNavigateUpgradeScreen = onNavigateUpgradeScreen
            )

            val text =
                context.getString(SharedR.string.account_storage_over_quota_inline_error_banner_upgrade_link)
            onNodeWithText(text).performClick()

            verify(onNavigateUpgradeScreen).invoke()
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setBanner(
        status: CUStatusUiState = CUStatusUiState.None,
        onEnableCameraUploads: () -> Unit = {},
        onDismissRequest: (status: CUStatusUiState) -> Unit = {},
        onChangeCameraUploadsPermissions: () -> Unit = {},
        onNavigateToCameraUploadsSettings: () -> Unit = {},
        onNavigateMobileDataSetting: () -> Unit = {},
        onNavigateUpgradeScreen: () -> Unit = {},
    ) {
        setContent {
            CameraUploadsBanner(
                status = status,
                onEnableCameraUploads = onEnableCameraUploads,
                onDismissRequest = onDismissRequest,
                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                onNavigateMobileDataSetting = onNavigateMobileDataSetting,
                onNavigateUpgradeScreen = onNavigateUpgradeScreen
            )
        }
    }
}
