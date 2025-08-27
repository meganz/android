package mega.privacy.android.app.presentation.transfers.transferoverquota.view.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.TransferOverQuotaDialogEvent
import mega.privacy.mobile.analytics.event.TransferOverQuotaUpgradeAccountButtonEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class TransferOverQuotaDialogTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsRule = AnalyticsTestRule()

    private val onNavigateToUpgradeAccount = mock<() -> Unit>()
    private val onNavigateToLogin = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()
    private val overQuotaDelay = "1m 12s"
    private val bodyString by lazy {
        composeTestRule.activity.getString(
            R.string.text_depleted_transfer_overquota,
            overQuotaDelay
        )
    }

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that view is correctly shown for not logged in user`() {
        initComposeTestRule(isLoggedIn = false, isFreeAccount = true)

        with(composeTestRule) {
            onNodeWithText(R.string.title_depleted_transfer_overquota).assertIsDisplayed()
            onNodeWithText(bodyString).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_upgrade_button).assertIsNotDisplayed()
            onNodeWithText(R.string.plans_depleted_transfer_overquota).assertIsNotDisplayed()
            onNodeWithText(sharedR.string.login_text).assertIsDisplayed()
            onNodeWithText(R.string.general_dismiss).assertIsDisplayed()
        }
    }

    @Test
    fun `test that view is correctly shown for logged in and free account`() {
        initComposeTestRule(isLoggedIn = true, isFreeAccount = true)

        with(composeTestRule) {
            onNodeWithText(R.string.title_depleted_transfer_overquota).assertIsDisplayed()
            onNodeWithText(bodyString).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_upgrade_button).assertIsDisplayed()
            onNodeWithText(R.string.plans_depleted_transfer_overquota).assertIsNotDisplayed()
            onNodeWithText(sharedR.string.login_text).assertIsNotDisplayed()
            onNodeWithText(R.string.general_dismiss).assertIsDisplayed()
        }
    }

    @Test
    fun `test that view is correctly shown for logged in and paid account`() {
        initComposeTestRule(isLoggedIn = true, isFreeAccount = false)

        with(composeTestRule) {
            onNodeWithText(R.string.title_depleted_transfer_overquota).assertIsDisplayed()
            onNodeWithText(bodyString).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_upgrade_button).assertIsNotDisplayed()
            onNodeWithText(R.string.plans_depleted_transfer_overquota).assertIsDisplayed()
            onNodeWithText(sharedR.string.login_text).assertIsNotDisplayed()
            onNodeWithText(R.string.general_dismiss).assertIsDisplayed()
        }
    }

    @Test
    fun `test that view is correctly shown for non logged in and paid account`() {
        initComposeTestRule(isLoggedIn = false, isFreeAccount = false)

        with(composeTestRule) {
            onNodeWithText(R.string.title_depleted_transfer_overquota).assertIsDisplayed()
            onNodeWithText(bodyString).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_upgrade_button).assertIsNotDisplayed()
            onNodeWithText(R.string.plans_depleted_transfer_overquota).assertIsNotDisplayed()
            onNodeWithText(sharedR.string.login_text).assertIsDisplayed()
            onNodeWithText(R.string.general_dismiss).assertIsDisplayed()
        }
    }

    @Test
    fun `test that dialog event is tracked`() {
        initComposeTestRule()

        assertThat(analyticsRule.events).contains(TransferOverQuotaDialogEvent)
    }

    @Test
    fun `test that upgrade account button event is tracked when logged in free user clicks confirmation button and correct event is tracked`() {
        initComposeTestRule(isLoggedIn = true, isFreeAccount = true)

        composeTestRule.onNodeWithText(sharedR.string.general_upgrade_button).performClick()

        assertThat(analyticsRule.events).contains(TransferOverQuotaUpgradeAccountButtonEvent)
        verify(onNavigateToUpgradeAccount).invoke()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that see plans button works properly`() {
        initComposeTestRule(isLoggedIn = true, isFreeAccount = false)

        composeTestRule.onNodeWithText(R.string.plans_depleted_transfer_overquota).performClick()

        assertThat(analyticsRule.events).contains(TransferOverQuotaUpgradeAccountButtonEvent)
        verify(onNavigateToUpgradeAccount).invoke()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that log in button event works properly`() {
        initComposeTestRule(isLoggedIn = false)

        composeTestRule.onNodeWithText(sharedR.string.login_text).performClick()

        assertThat(analyticsRule.events).doesNotContain(TransferOverQuotaUpgradeAccountButtonEvent)
        verify(onNavigateToLogin).invoke()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that dismiss button works properly`() {
        initComposeTestRule()

        composeTestRule.onNodeWithText(R.string.general_dismiss).performClick()

        assertThat(analyticsRule.events).doesNotContain(TransferOverQuotaUpgradeAccountButtonEvent)
        verify(onDismiss).invoke()
        verifyNoInteractions(onNavigateToUpgradeAccount)
        verifyNoInteractions(onNavigateToLogin)
    }

    private fun initComposeTestRule(isLoggedIn: Boolean = true, isFreeAccount: Boolean = true) {
        composeTestRule.setContent {
            TransferOverQuotaDialogContent(
                isLoggedIn = isLoggedIn,
                isFreeAccount = isFreeAccount,
                overQuotaDelay = overQuotaDelay,
                onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
                onNavigateToLogin = onNavigateToLogin,
                onDismiss = onDismiss,
            )
        }
    }

}