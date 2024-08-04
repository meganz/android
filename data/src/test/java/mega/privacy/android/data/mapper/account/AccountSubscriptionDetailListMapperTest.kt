package mega.privacy.android.data.mapper.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.AccountSubscriptionCycleMapper
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.PaymentMethodTypeMapper
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.SubscriptionStatusMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountSubscriptionDetail
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaAccountSubscription
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaStringList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountSubscriptionDetailListMapperTest {
    private val subscriptionStatusMapper = mock<SubscriptionStatusMapper>()
    private val subscriptionCycleMapper = mock<AccountSubscriptionCycleMapper>()
    private val accountTypeMapper = mock<AccountTypeMapper>()
    private val paymentMethodTypeMapper = mock<PaymentMethodTypeMapper>()
    private val stringListMapper = mock<StringListMapper>()

    private val underTest = AccountSubscriptionDetailListMapper(
        subscriptionStatusMapper,
        subscriptionCycleMapper,
        accountTypeMapper,
        paymentMethodTypeMapper,
        stringListMapper,
    )

    private val expectedAccountLevel = 1L
    private val expectedSubscriptionCycle = "1 M"
    private val expectedSubscriptionStatus = MegaAccountSubscription.SUBSCRIPTION_STATUS_VALID
    private val expectedPaymentMethodId = MegaApiJava.PAYMENT_METHOD_STRIPE2
    private val expectedSubscriptionId = "123TestID"
    private val expectedRenewTime = 123456789L
    private val megaStringList = mock<MegaStringList> {
        on { size() } doReturn 2
        on { get(0) } doReturn "vpn"
        on { get(1) } doReturn "pwm"
    }

    private val megaAccountSubscription = mock<MegaAccountSubscription> {
        on { this.id }.thenReturn(expectedSubscriptionId)
        on { this.status }.thenReturn(expectedSubscriptionStatus)
        on { this.cycle }.thenReturn(expectedSubscriptionCycle)
        on { this.paymentMethodId }.thenReturn(expectedPaymentMethodId.toLong())
        on { this.renewTime }.thenReturn(expectedRenewTime)
        on { this.accountLevel }.thenReturn(expectedAccountLevel)
        on { this.features }.thenReturn(megaStringList)
        on { this.isTrial }.thenReturn(false)
    }

    private val megaAccountDetails = mock<MegaAccountDetails> {
        on { this.numSubscriptions }.thenReturn(1)
        on { this.getSubscription(any()) }.thenReturn(megaAccountSubscription)
    }

    private val accountSubscriptionDetail = AccountSubscriptionDetail(
        subscriptionId = expectedSubscriptionId,
        subscriptionStatus = SubscriptionStatus.VALID,
        subscriptionCycle = AccountSubscriptionCycle.MONTHLY,
        paymentMethodType = PaymentMethodType.STRIPE2,
        renewalTime = expectedRenewTime,
        subscriptionLevel = AccountType.PRO_I,
        featuresList = listOf("vpn", "pwm"),
        isFreeTrial = false,
    )

    @Test
    fun `test that account subscription detail is mapped correctly to list of account subscription details`() =
        runTest {
            whenever(subscriptionStatusMapper(expectedSubscriptionStatus)).thenReturn(
                accountSubscriptionDetail.subscriptionStatus
            )
            whenever(subscriptionCycleMapper(expectedSubscriptionCycle)).thenReturn(
                accountSubscriptionDetail.subscriptionCycle
            )
            whenever(accountTypeMapper(expectedAccountLevel.toInt())).thenReturn(
                accountSubscriptionDetail.subscriptionLevel
            )
            whenever(paymentMethodTypeMapper(expectedPaymentMethodId)).thenReturn(
                accountSubscriptionDetail.paymentMethodType
            )
            whenever(stringListMapper(megaStringList)).thenReturn(accountSubscriptionDetail.featuresList)

            val actual = underTest(megaAccountDetails)

            assertThat(actual.size).isEqualTo(1)
            assertThat(actual[0].subscriptionId).isEqualTo(accountSubscriptionDetail.subscriptionId)
            assertThat(actual[0].subscriptionStatus).isEqualTo(accountSubscriptionDetail.subscriptionStatus)
            assertThat(actual[0].subscriptionCycle).isEqualTo(accountSubscriptionDetail.subscriptionCycle)
            assertThat(actual[0].paymentMethodType).isEqualTo(accountSubscriptionDetail.paymentMethodType)
            assertThat(actual[0].renewalTime).isEqualTo(accountSubscriptionDetail.renewalTime)
            assertThat(actual[0].subscriptionLevel).isEqualTo(accountSubscriptionDetail.subscriptionLevel)
            assertThat(actual[0].featuresList).isEqualTo(accountSubscriptionDetail.featuresList)
            assertThat(actual[0].isFreeTrial).isEqualTo(accountSubscriptionDetail.isFreeTrial)
        }

}