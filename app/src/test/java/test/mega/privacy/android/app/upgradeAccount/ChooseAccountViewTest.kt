package test.mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountView
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText
import java.text.DecimalFormat

@RunWith(AndroidJUnit4::class)
internal class ChooseAccountViewTest {
    private val localisedPriceStringMapper = LocalisedPriceStringMapper()
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()
    private val subscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = PRO_I_STORAGE_TRANSFER,
        monthlyTransfer = PRO_I_STORAGE_TRANSFER,
        yearlyTransfer = PRO_I_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_I_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_I_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val subscriptionProII = LocalisedSubscription(
        accountType = AccountType.PRO_II,
        storage = PRO_II_STORAGE_TRANSFER,
        monthlyTransfer = PRO_II_STORAGE_TRANSFER,
        yearlyTransfer = PRO_II_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_II_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_II_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val subscriptionProIII = LocalisedSubscription(
        accountType = AccountType.PRO_III,
        storage = PRO_III_STORAGE_TRANSFER,
        monthlyTransfer = PRO_III_STORAGE_TRANSFER,
        yearlyTransfer = PRO_III_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_III_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_III_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val subscriptionProLite = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        storage = PRO_LITE_STORAGE,
        monthlyTransfer = PRO_LITE_TRANSFER_MONTHLY,
        yearlyTransfer = PRO_LITE_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_LITE_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_LITE_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val expectedLocalisedSubscriptionsList = listOf(
        subscriptionProLite,
        subscriptionProI,
        subscriptionProII,
        subscriptionProIII
    )

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that comment about recurring subscription plan cancellation is shown`() {
        setContent()

        composeRule.onNodeWithText(R.string.upgrade_comment).assertExists()
    }

    @Test
    fun `test the free plan row is shown correctly`() {
        setContent()

        val expectedResults = listOf(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.free_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_storage_label,
                "20 GB+"
            ).replace("[A]", "").replace("[/A]", "") + "1",
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.account_choose_free_limited_transfer_quota)
                .replace("[A]", "").replace("[/A]", ""),
            "1 " + InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.footnote_achievements)
        )

        expectedResults.forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `test the list of subscription plans is shown correctly`() {
        setContent()

        val expectedResults = listOf(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.prolite_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.type_month,
                "€4.99"
            ).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(400)
            ).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(1024)
            ).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro1_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.type_month,
                "€9.99"
            ).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(2048)
            ).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(PRO_I_STORAGE_TRANSFER.toLong())
            ).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro2_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.type_month,
                "€19.99"
            ).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(PRO_II_STORAGE_TRANSFER.toLong())
            ).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(8192)
            ).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro3_account),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.type_month,
                "€29.99"
            ).replace("[A]", "").replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_storage_label,
                getStorageTransferStringGBBased(PRO_III_STORAGE_TRANSFER.toLong())
            ).replace("[A]", "")
                .replace("[/A]", ""),
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.account_upgrade_transfer_quota_label,
                getStorageTransferStringGBBased(PRO_III_STORAGE_TRANSFER.toLong())
            ).replace("[A]", "")
                .replace("[/A]", "")
        )
        expectedResults.forEach {
            composeRule.onNodeWithText(it).assertExists()
        }

    }

    @Test
    fun `test that top bar is shown correctly with a title`() {
        setContent()
        composeRule.onNodeWithText(R.string.choose_account_fragment).assertExists()
    }

    private fun setContent() = composeRule.setContent {
        ChooseAccountView(
            getChooseAccountState(),
            onBackPressed = {},
            onPlanClicked = {},
        )
    }

    private fun getChooseAccountState(): ChooseAccountState =
        ChooseAccountState(
            localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
        )

    private fun getStorageTransferStringGBBased(gbSize: Long): String {
        val sizeString: String
        val decf = DecimalFormat("###.##")

        val TB = 1024

        if (gbSize < TB) {
            sizeString =
                InstrumentationRegistry.getInstrumentation().targetContext.getString(
                    R.string.label_file_size_giga_byte,
                    decf.format(gbSize)
                )
        } else {
            sizeString =
                InstrumentationRegistry.getInstrumentation().targetContext.getString(
                    R.string.label_file_size_tera_byte,
                    decf.format(gbSize / TB)
                )
        }

        return sizeString
    }

    companion object {
        const val PRO_I_STORAGE_TRANSFER = 2048
        const val PRO_II_STORAGE_TRANSFER = 8192
        const val PRO_III_STORAGE_TRANSFER = 16384
        const val PRO_LITE_STORAGE = 400
        const val PRO_LITE_TRANSFER_MONTHLY = 1024
        const val PRO_LITE_TRANSFER_YEARLY = 12288
        const val PRO_I_TRANSFER_YEARLY = 24576
        const val PRO_II_TRANSFER_YEARLY = 98304
        const val PRO_III_TRANSFER_YEARLY = 196608
        const val PRO_I_PRICE_MONTHLY = 9.99F
        const val PRO_II_PRICE_MONTHLY = 19.99F
        const val PRO_III_PRICE_MONTHLY = 29.99F
        const val PRO_LITE_PRICE_MONTHLY = 4.99F
        const val PRO_I_PRICE_YEARLY = 99.99F
        const val PRO_II_PRICE_YEARLY = 199.99F
        const val PRO_III_PRICE_YEARLY = 299.99F
        const val PRO_LITE_PRICE_YEARLY = 49.99F
    }
}