package mega.privacy.mobile.home.presentation.home.widget.banner.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.Test
import java.util.Locale

class SubscriptionOfferBannerMapperTest {

    private val underTest = SubscriptionOfferBannerMapper()

    private val subscription = Subscription(
        sku = "mega.android.pro1.onemonth",
        accountType = AccountType.PRO_I,
        handle = 1L,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(9.99f, Currency("EUR")),
        discountedAmountMonthly = CurrencyAmount(4.99f, Currency("EUR")),
        discountedPercentage = 50,
        discountName = "Black Friday",
        offerValidUntil = 1_785_000_000L,
        offerCampaignId = 90210L,
    )

    @Test
    fun `test that invoke maps a discounted subscription to the offer banner model`() {
        val result = underTest(subscription, Locale.US)

        assertThat(result).isNotNull()
        assertThat(result?.campaignName).isEqualTo(LocalizedText.Literal("Black Friday"))
        assertThat(result?.discountPercentage).isEqualTo(50)
        assertThat(result?.formattedPrice).isEqualTo("€4.99")
        assertThat(result?.planNameRes).isEqualTo(sharedR.string.pro1_account)
        assertThat(result?.validUntil).isEqualTo(1_785_000_000L)
        assertThat(result?.campaignId).isEqualTo(90210L)
    }

    @Test
    fun `test that invoke maps a missing expiry to zero`() {
        val result = underTest(subscription.copy(offerValidUntil = null), Locale.US)

        assertThat(result?.validUntil).isEqualTo(0L)
    }

    @Test
    fun `test that invoke maps a missing campaign to the no campaign id`() {
        val result = underTest(subscription.copy(offerCampaignId = null), Locale.US)

        assertThat(result?.campaignId).isEqualTo(Subscription.NO_OFFER_CAMPAIGN_ID)
    }

    @Test
    fun `test that invoke falls back to the special offer label when the discount name is blank`() {
        val result = underTest(subscription.copy(discountName = " "), Locale.US)

        assertThat(result?.campaignName).isEqualTo(
            LocalizedText.StringRes(sharedR.string.subscription_offer_special_offer_badge)
        )
    }

    @Test
    fun `test that invoke returns null when there is no discounted monthly amount`() {
        val result = underTest(subscription.copy(discountedAmountMonthly = null), Locale.US)

        assertThat(result).isNull()
    }

    @Test
    fun `test that invoke returns null when there is no discount percentage`() {
        val result = underTest(subscription.copy(discountedPercentage = null), Locale.US)

        assertThat(result).isNull()
    }

    @Test
    fun `test that invoke returns null when the account type is not supported`() {
        val result = underTest(subscription.copy(accountType = AccountType.FREE), Locale.US)

        assertThat(result).isNull()
    }
}
