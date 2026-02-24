package mega.privacy.android.core.nodecomponents.components.banners

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.core.sharedcomponents.coroutine.resetLaunchedOncePerAppEffect
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.AlmostFullStorageAndTransferOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.FullStorageAndTransferOverQuotaErrorBannerDisplayeEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.TransferOverQuotaErrorBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.TransferOverQuotaWarningBannerDisplayedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class OverQuotaBannerTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val onDismissed = mock<() -> Unit>()
    private val onUpgradeClicked = mock<() -> Unit>()

    @Test
    fun `test that OverQuotaBanner shows error banner when severity is Error`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.Full),
        )

        composeTestRule.onNodeWithTag(STORAGE_ERROR_BANNER_ROOT_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that OverQuotaBanner shows warning banner when severity is Warning`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
        )

        composeTestRule.onNodeWithTag(STORAGE_WARNING_BANNER_ROOT_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that clicking upgrade on error banner invokes onUpgradeClicked`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.Full),
        )

        val upgradeText =
            fromStringRes(R.string.account_storage_over_quota_inline_error_banner_upgrade_link)
        composeTestRule.onNodeWithText(upgradeText).performClick()

        verify(onUpgradeClicked).invoke()
    }

    @Test
    fun `test that clicking upgrade on warning banner invokes onUpgradeClicked`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
        )

        val upgradeText =
            fromStringRes(R.string.account_storage_over_quota_inline_error_banner_upgrade_link)
        composeTestRule.onNodeWithText(upgradeText).performClick()

        verify(onUpgradeClicked).invoke()
    }

    @Test
    fun `test that FullStorageOverQuotaBannerDisplayedEvent is tracked when error banner with Full storage is displayed`() {
        resetLaunchedOncePerAppEffect(FullStorageOverQuotaBannerDisplayedEvent)
        setContent(overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.Full))

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).contains(FullStorageOverQuotaBannerDisplayedEvent)
    }

    @Test
    fun `test that analytics event is tracked only once`() {
        resetLaunchedOncePerAppEffect(FullStorageOverQuotaBannerDisplayedEvent)

        composeTestRule.setContent {
            OverQuotaBanner(
                overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
                onDismissed = {},
                onUpgradeClicked = {},
            )
            OverQuotaBanner(
                overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
                onDismissed = {},
                onUpgradeClicked = {},
            )
        }
        composeTestRule.waitForIdle()


        assertThat(analyticsRule.events).containsNoDuplicates()
    }

    @Test
    fun `test that FullStorageOverQuotaBannerUpgradeButtonPressedEvent is tracked when upgrade is clicked on error banner`() {
        setContent(overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.Full))

        val upgradeText =
            fromStringRes(R.string.account_storage_over_quota_inline_error_banner_upgrade_link)
        composeTestRule.onNodeWithText(upgradeText).performClick()

        assertThat(analyticsRule.events).contains(
            FullStorageOverQuotaBannerUpgradeButtonPressedEvent
        )
    }

    @Test
    fun `test that AlmostFullStorageOverQuotaBannerDisplayedEvent is tracked when warning banner with AlmostFull is displayed`() {
        resetLaunchedOncePerAppEffect(AlmostFullStorageOverQuotaBannerDisplayedEvent)
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
        )

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).contains(AlmostFullStorageOverQuotaBannerDisplayedEvent)
    }

    @Test
    fun `test that AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent is tracked when upgrade is clicked on warning banner`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
        )

        val upgradeText =
            fromStringRes(R.string.account_storage_over_quota_inline_error_banner_upgrade_link)
        composeTestRule.onNodeWithText(upgradeText).performClick()

        assertThat(analyticsRule.events).contains(
            AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
        )
    }

    @Test
    fun `test that FullStorageAndTransferOverQuotaErrorBannerDisplayedEvent is tracked when error banner with Full storage and Transfer is displayed`() {
        resetLaunchedOncePerAppEffect(FullStorageAndTransferOverQuotaErrorBannerDisplayeEvent)
        setContent(
            overQuotaStatus = OverQuotaStatus(
                OverQuotaIssue.Storage.Full,
                OverQuotaIssue.Transfer.TransferOverQuota,
            ),
        )

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).contains(
            FullStorageAndTransferOverQuotaErrorBannerDisplayeEvent
        )
    }

    @Test
    fun `test that TransferOverQuotaErrorBannerDisplayedEvent is tracked when error banner with only Transfer is displayed`() {
        resetLaunchedOncePerAppEffect(TransferOverQuotaErrorBannerDisplayedEvent)
        setContent(
            overQuotaStatus = OverQuotaStatus(transfer = OverQuotaIssue.Transfer.TransferOverQuota),
        )

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).contains(
            TransferOverQuotaErrorBannerDisplayedEvent
        )
    }

    @Test
    fun `test that FullStorageOverQuotaBannerUpgradeButtonPressedEvent is tracked when upgrade is clicked on error banner with storage and transfer`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(
                OverQuotaIssue.Storage.Full,
                OverQuotaIssue.Transfer.TransferOverQuota,
            ),
        )

        val upgradeText =
            fromStringRes(R.string.account_storage_over_quota_inline_error_banner_upgrade_link)
        composeTestRule.onNodeWithText(upgradeText).performClick()

        assertThat(analyticsRule.events).contains(
            FullStorageOverQuotaBannerUpgradeButtonPressedEvent
        )
    }

    @Test
    fun `test that TransferOverQuotaErrorBannerDisplayedEvent is tracked and upgrade click tracks FullStorageOverQuotaBannerUpgradeButtonPressedEvent`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(transfer = OverQuotaIssue.Transfer.TransferOverQuota),
        )

        val upgradeText =
            fromStringRes(R.string.account_storage_over_quota_inline_error_banner_upgrade_link)
        composeTestRule.onNodeWithText(upgradeText).performClick()

        assertThat(analyticsRule.events).contains(
            FullStorageOverQuotaBannerUpgradeButtonPressedEvent
        )
    }

    @Test
    fun `test that AlmostFullStorageAndTransferOverQuotaBannerDisplayedEvent is tracked when warning banner with AlmostFull and Transfer is displayed`() {
        resetLaunchedOncePerAppEffect(AlmostFullStorageAndTransferOverQuotaBannerDisplayedEvent)
        setContent(
            overQuotaStatus = OverQuotaStatus(
                OverQuotaIssue.Storage.AlmostFull,
                OverQuotaIssue.Transfer.TransferOverQuotaFreeUser,
            ),
        )

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).contains(
            AlmostFullStorageAndTransferOverQuotaBannerDisplayedEvent
        )
    }

    @Test
    fun `test that TransferOverQuotaWarningBannerDisplayedEvent is tracked when warning banner with only Transfer is displayed`() {
        resetLaunchedOncePerAppEffect(TransferOverQuotaWarningBannerDisplayedEvent)
        setContent(
            overQuotaStatus = OverQuotaStatus(
                transfer = OverQuotaIssue.Transfer.TransferOverQuotaFreeUser,
            ),
        )

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).contains(
            TransferOverQuotaWarningBannerDisplayedEvent
        )
    }

    @Test
    fun `test that AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent is tracked when close is clicked on warning banner`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
        )

        composeTestRule.onNodeWithContentDescription("Banner Cancel").performClick()

        assertThat(analyticsRule.events).contains(
            AlmostFullStorageOverQuotaBannerCloseButtonPressedEvent
        )
    }

    @Test
    fun `test that warning banner is not displayed when isBlockingAware and severity is NonBlocking`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
            isBlockingAware = true,
        )

        composeTestRule.onNodeWithTag(STORAGE_WARNING_BANNER_ROOT_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that analytics event is tracked again when OverQuotaWarningBanner is shown again after closing via Cancel button`() {
        resetLaunchedOncePerAppEffect(AlmostFullStorageOverQuotaBannerDisplayedEvent)

        val showBanner = mutableStateOf(true)
        composeTestRule.setContent {
            if (showBanner.value) {
                OverQuotaBanner(
                    overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
                    onDismissed = { showBanner.value = false },
                    onUpgradeClicked = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        val countAfterFirstShow =
            analyticsRule.events.count { it == AlmostFullStorageOverQuotaBannerDisplayedEvent }
        assertThat(countAfterFirstShow).isEqualTo(1)

        composeTestRule.onNodeWithContentDescription("Banner Cancel").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread { showBanner.value = true }
        composeTestRule.waitForIdle()

        val countAfterSecondShow =
            analyticsRule.events.count { it == AlmostFullStorageOverQuotaBannerDisplayedEvent }
        assertThat(countAfterSecondShow).isEqualTo(2)
    }

    private fun setContent(
        overQuotaStatus: OverQuotaStatus,
        isBlockingAware: Boolean = false,
    ) {
        composeTestRule.setContent {
            OverQuotaBanner(
                overQuotaStatus = overQuotaStatus,
                onDismissed = onDismissed::invoke,
                onUpgradeClicked = onUpgradeClicked::invoke,
                isBlockingAware = isBlockingAware,
            )
        }
    }

    private fun fromStringRes(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
