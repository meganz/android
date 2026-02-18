package mega.privacy.android.core.nodecomponents.components.banners

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.AlmostFullStorageOverQuotaBannerUpgradeButtonPressedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerDisplayedEvent
import mega.privacy.mobile.analytics.event.FullStorageOverQuotaBannerUpgradeButtonPressedEvent
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
        setContent(overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.Full))

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).contains(FullStorageOverQuotaBannerDisplayedEvent)
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
    fun `test that warning banner is not displayed when isBlockingAware and severity is NonBlocking`() {
        setContent(
            overQuotaStatus = OverQuotaStatus(OverQuotaIssue.Storage.AlmostFull),
            isBlockingAware = true,
        )

        composeTestRule.onNodeWithTag(STORAGE_WARNING_BANNER_ROOT_TEST_TAG).assertDoesNotExist()
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
