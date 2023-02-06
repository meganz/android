package test.mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.upgradeAccount.view.UpgradeAccountView
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.view.BuyNewSubscriptionDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText
import java.text.DecimalFormat

@RunWith(AndroidJUnit4::class)
class UpgradeAccountViewTest {
    private val subscriptionProIMonthly = Subscription(
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(9.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIMonthly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyAmount(19.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIIMonthly = Subscription(
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = 16384,
        transfer = 16384,
        amount = CurrencyAmount(29.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProLiteMonthly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = 400,
        transfer = 1024,
        amount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
    )

    private val expectedSubscriptionsList = listOf(
        subscriptionProLiteMonthly,
        subscriptionProIMonthly,
        subscriptionProIIMonthly,
        subscriptionProIIIMonthly
    )

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that comment about recurring subscription plan cancellation is shown`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onPlanClicked = {},
                onCustomLabelClicked = {},
                hideBillingWarning = {},
                onDialogPositiveButtonClicked = {},
                onDialogDismissButtonClicked = {},
            )
        }

        composeRule.onNodeWithText(R.string.upgrade_comment)
    }

    @Test
    fun `test that current subscription is shown`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onPlanClicked = {},
                onCustomLabelClicked = {},
                hideBillingWarning = {},
                onDialogPositiveButtonClicked = {},
                onDialogDismissButtonClicked = {},
            )
        }

        val text =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_of_my_account,
                "Free").replace("[A]", "").replace("[/A]", "")
        composeRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun `test the list of subscription plans is shown correctly`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onPlanClicked = {},
                onCustomLabelClicked = {},
                hideBillingWarning = {},
                onDialogPositiveButtonClicked = {},
                onDialogDismissButtonClicked = {},
            )
        }

        val expectedResults = listOf(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.prolite_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€4.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(400)).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(1024)).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro1_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€9.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(2048)).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(2048)).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro2_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€19.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(8192)).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(8192)).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro3_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€29.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(16384)).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(16384)).replace("[A]", "")
                .replace("[/A]", "")
        )
        expectedResults.forEach {
            composeRule.onNodeWithText(it).assertExists()
        }

    }

    @Test
    fun `test that top bar is shown correctly with a title`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onPlanClicked = {},
                onCustomLabelClicked = {},
                hideBillingWarning = {},
                onDialogPositiveButtonClicked = {},
                onDialogDismissButtonClicked = {},
            )
        }
        composeRule.onNodeWithText(R.string.action_upgrade_account)
    }

    @Test
    fun `test that billing warning is displayed when billing is not available`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.PRO_III, true),
                onBackPressed = {},
                onPlanClicked = {},
                onCustomLabelClicked = {},
                hideBillingWarning = {},
                onDialogPositiveButtonClicked = {},
                onDialogDismissButtonClicked = {},
            )
        }

        composeRule.onNodeWithText(R.string.upgrade_billing_warning)
    }

    @Test
    fun `test that custom label is displayed if user has pro iii plan`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.PRO_III, true),
                onBackPressed = {},
                onPlanClicked = {},
                onCustomLabelClicked = {},
                hideBillingWarning = {},
                onDialogPositiveButtonClicked = {},
                onDialogDismissButtonClicked = {},
            )
        }

        composeRule.onNodeWithText(R.string.label_custom_plan)
    }

    @Test
    fun `test that clicking the positive dialog button calls the correct function`() {
        val onDialogPositiveButtonClicked = mock<(Int) -> Unit>()

        composeRule.setContent {
            BuyNewSubscriptionDialog(
                upgradeTypeInt = 2,
                paymentMethod = PaymentMethod.GOOGLE_WALLET,
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                onDialogDismissButtonClicked = mock(),
            )
        }

        composeRule.onNodeWithText(R.string.button_buy_new_subscription).performClick()

        verify(onDialogPositiveButtonClicked).invoke(2)
    }

    @Test
    fun `test that clicking the dismiss dialog button calls the correct function`() {
        val onDialogDismissButtonClicked = mock<() -> Unit>()

        composeRule.setContent {
            BuyNewSubscriptionDialog(
                upgradeTypeInt = 2,
                paymentMethod = PaymentMethod.GOOGLE_WALLET,
                onDialogPositiveButtonClicked = mock(),
                onDialogDismissButtonClicked = onDialogDismissButtonClicked,
            )
        }

        composeRule.onNodeWithText(R.string.general_dismiss).performClick()

        verify(onDialogDismissButtonClicked).invoke()
    }

    private fun getUpgradeAccountState(
        accountType: AccountType,
        showBillingWarning: Boolean,
    ): UpgradeAccountState =
        UpgradeAccountState(
            subscriptionsList = expectedSubscriptionsList,
            currentSubscriptionPlan = accountType,
            showBillingWarning = showBillingWarning,
            currentPayment = UpgradePayment(
                upgradeType = Constants.INVALID_VALUE,
                currentPayment = null
            )
        )

    private fun getStorageTransferStringGBBased(gbSize: Long): String {
        val sizeString: String
        val decf = DecimalFormat("###.##")

        val TB = 1024

        if (gbSize < TB) {
            sizeString =
                InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.label_file_size_giga_byte,
                    decf.format(gbSize))
        } else {
            sizeString =
                InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.label_file_size_tera_byte,
                    decf.format(gbSize / TB))
        }

        return sizeString
    }
}