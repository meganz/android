package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaPricing
import nz.mega.sdk.MegaRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SubscriptionOptionListMapperTest {
    private val currencyMapper = ::Currency

    private val pricing = mock<MegaPricing> {
        on { numProducts }.thenReturn(1)
        on { getHandle(0) }.thenReturn(1560943707714440503)
        on { getProLevel(0) }.thenReturn(1)
        on { getMonths(0) }.thenReturn(1)
        on { getGBStorage(0) }.thenReturn(450)
        on { getGBTransfer(0) }.thenReturn(450)
        on { getAmount(0) }.thenReturn(13)
        on { getAndroidID(0) }.thenReturn("com.mega.pro1.monthly")
        on { hasMobileOffers(0) }.thenReturn(false)
        on { getMobileOfferLabel(0) }.thenReturn("World Backup Day Sale")
        on { getMobileOfferExpiryTimestamp(0) }.thenReturn(1787464050)
        on { getMobileOfferFlags(0) }.thenReturn(5)
        on { getMobileOfferReshowInterval(0) }.thenReturn(86400)
        on { getMobileOfferCampaignId(0) }.thenReturn(90210)
    }

    private val currency = mock<MegaCurrency> {
        on { currencyName }.thenReturn("EUR")
    }

    private val request = mock<MegaRequest> {
        on { pricing }.thenReturn(pricing)
        on { currency }.thenReturn(currency)
    }

    private val accountTypeMapper = mock<AccountTypeMapper>()

    private val deviceGateway = mock<DeviceGateway> {
        on { now }.thenReturn(NOW_MILLIS)
    }
    private val subscriptionOption = SubscriptionOption(
        accountType = AccountType.PRO_I,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(13),
        currency = currencyMapper("EUR"),
        sku = "com.mega.pro1.monthly",
        hasOffer = false,
        discountName = "World Backup Day Sale",
        offerValidUntil = 1787464050,
        offerFlags = 5,
        offerReshowInterval = 86400,
        offerCampaignId = 90210,
    )

    private val underTest = SubscriptionOptionListMapper(
        currencyMapper,
        accountTypeMapper,
        deviceGateway,
    )

    @Test
    fun `test that subscription option is mapped correctly to the list of subscription options`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.hasMobileOffers(0)).thenReturn(false)
        whenever(pricing.getMobileOfferExpiryTimestamp(0)).thenReturn(1787464050)
        whenever(pricing.getMobileOfferFlags(0)).thenReturn(5)
        whenever(pricing.getMobileOfferCampaignId(0)).thenReturn(90210)
        val actual = underTest(request)
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual).isEqualTo(listOf(subscriptionOption))
    }

    @Test
    fun `test that offerValidUntil is null when the mobile offer has no expiry`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.getMobileOfferExpiryTimestamp(0)).thenReturn(0)
        val actual = underTest(request)
        assertThat(actual.single().offerValidUntil).isNull()
    }

    @Test
    fun `test that offerFlags is null when the mobile offer has no flags`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.getMobileOfferFlags(0)).thenReturn(0)
        val actual = underTest(request)
        assertThat(actual.single().offerFlags).isNull()
    }

    @Test
    fun `test that offerReshowInterval is mapped when the mobile offer has a reshow interval`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.getMobileOfferReshowInterval(0)).thenReturn(86400)
        val actual = underTest(request)
        assertThat(actual.single().offerReshowInterval).isEqualTo(86400)
    }

    @Test
    fun `test that offerReshowInterval is null when the mobile offer has no reshow interval`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.getMobileOfferReshowInterval(0)).thenReturn(0)
        val actual = underTest(request)
        assertThat(actual.single().offerReshowInterval).isNull()
    }

    @Test
    fun `test that offerCampaignId is mapped when the mobile offer belongs to a campaign`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.getMobileOfferCampaignId(0)).thenReturn(90210)
        val actual = underTest(request)
        assertThat(actual.single().offerCampaignId).isEqualTo(90210)
    }

    @Test
    fun `test that offerCampaignId is null when the mobile offer belongs to no campaign`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.getMobileOfferCampaignId(0)).thenReturn(0)
        val actual = underTest(request)
        assertThat(actual.single().offerCampaignId).isNull()
    }

    @Test
    fun `test that hasOffer is true when the mobile offer has not expired`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.hasMobileOffers(0)).thenReturn(true)
        whenever(pricing.getMobileOfferExpiryTimestamp(0)).thenReturn(NOW_SECONDS + 3600)
        val actual = underTest(request)
        assertThat(actual.single().hasOffer).isTrue()
    }

    @Test
    fun `test that hasOffer is false when the mobile offer has already expired`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.hasMobileOffers(0)).thenReturn(true)
        whenever(pricing.getMobileOfferExpiryTimestamp(0)).thenReturn(NOW_SECONDS - 3600)
        val actual = underTest(request)
        assertThat(actual.single().hasOffer).isFalse()
    }

    @Test
    fun `test that hasOffer is true when the mobile offer has no expiry`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.hasMobileOffers(0)).thenReturn(true)
        whenever(pricing.getMobileOfferExpiryTimestamp(0)).thenReturn(0)
        val actual = underTest(request)
        assertThat(actual.single().hasOffer).isTrue()
        assertThat(actual.single().offerValidUntil).isNull()
    }

    @Test
    fun `test that hasOffer is false when there is no mobile offer despite a future expiry`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        whenever(pricing.hasMobileOffers(0)).thenReturn(false)
        whenever(pricing.getMobileOfferExpiryTimestamp(0)).thenReturn(NOW_SECONDS + 3600)
        val actual = underTest(request)
        assertThat(actual.single().hasOffer).isFalse()
    }

    private companion object {
        const val NOW_SECONDS = 1_785_000_000L
        const val NOW_MILLIS = NOW_SECONDS * 1000L
    }
}