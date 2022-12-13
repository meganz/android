package test.mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.upgradeAccount.UpgradeAccountView
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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

    private val upgradeAccountState =
        UpgradeAccountState(subscriptionsList = expectedSubscriptionsList,
            currentSubscriptionPlan = AccountType.FREE)

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that comment about recurring subscription plan cancellation is shown`() {
        composeRule.setContent {
            UpgradeAccountView(upgradeAccountState,
                onBackPressed = {},
                onPlanClicked = {})
        }

        composeRule.onNodeWithText(R.string.upgrade_comment)
    }

    @Test
    fun `test that current subscription is shown`() {
        composeRule.setContent {
            UpgradeAccountView(upgradeAccountState,
                onBackPressed = {},
                onPlanClicked = {})
        }

        var text =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_of_my_account,
                "Free").replace("[A]", "").replace("[/A]", "")
        composeRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun `test the list of subscription plans is shown correctly`() {
        composeRule.setContent {
            UpgradeAccountView(upgradeAccountState,
                onBackPressed = {},
                onPlanClicked = {})
        }

        val expectedResults = listOf(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.prolite_account)
                .uppercase(),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€4.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(400)).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(1024)).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro1_account)
                .uppercase(),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€9.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(2048)).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(2048)).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro2_account)
                .uppercase(),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€19.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(8192)).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(8192)).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro3_account)
                .uppercase(),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.type_month,
                "€29.99").replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(16384)).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(16384)).replace("[A]", "").replace("[/A]", "")
        )
        expectedResults.forEach {
            composeRule.onNodeWithText(it).assertExists()
        }

    }

    @Test
    fun `test that top bar is shown correctly with a title`() {
        composeRule.setContent {
            UpgradeAccountView(upgradeAccountState,
                onBackPressed = {},
                onPlanClicked = {})
        }
        composeRule.onNodeWithText(R.string.action_upgrade_account)
    }

    private fun getStorageTransferStringGBBased(gbSize: Long): String {
        var sizeString: String
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