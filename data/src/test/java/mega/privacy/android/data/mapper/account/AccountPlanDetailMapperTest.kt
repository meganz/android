package mega.privacy.android.data.mapper.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.AccountTypeMapper
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import nz.mega.sdk.MegaAccountPlan
import nz.mega.sdk.MegaStringList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountPlanDetailMapperTest {
    private val accountTypeMapper = mock<AccountTypeMapper>()
    private val stringListMapper = mock<StringListMapper>()

    private val underTest = AccountPlanDetailMapper(accountTypeMapper, stringListMapper)

    private val expectedAccountLevel = 1L
    private val expectedExpirationTime = 123456789L
    private val expectedSubscriptionId = "test"
    private val megaStringList = mock<MegaStringList> {
        on { size() } doReturn 2
        on { get(0) } doReturn "vpn"
        on { get(1) } doReturn "pwm"
    }

    private val megaAccountPlan = mock<MegaAccountPlan> {
        on { this.accountLevel }.thenReturn(expectedAccountLevel)
        on { this.isProPlan }.thenReturn(true)
        on { this.expirationTime }.thenReturn(expectedExpirationTime)
        on { this.id }.thenReturn(expectedSubscriptionId)
        on { this.features }.thenReturn(megaStringList)
        on { this.isTrial }.thenReturn(false)
    }

    private val expectedAccountPlanDetail = AccountPlanDetail(
        accountType = AccountType.PRO_I,
        isProPlan = true,
        expirationTime = expectedExpirationTime,
        subscriptionId = expectedSubscriptionId,
        featuresList = listOf("vpn", "pwm"),
        isFreeTrial = false
    )

    @Test
    fun `test that account plan detail is mapped correctly`() = runTest {
        whenever(accountTypeMapper(expectedAccountLevel.toInt())).thenReturn(
            expectedAccountPlanDetail.accountType
        )
        whenever(stringListMapper(megaStringList)).thenReturn(expectedAccountPlanDetail.featuresList)
        val actual = underTest(megaAccountPlan)
        assertThat(actual?.accountType).isEqualTo(expectedAccountPlanDetail.accountType)
        assertThat(actual?.isProPlan).isEqualTo(expectedAccountPlanDetail.isProPlan)
        assertThat(actual?.expirationTime).isEqualTo(expectedAccountPlanDetail.expirationTime)
        assertThat(actual?.subscriptionId).isEqualTo(expectedAccountPlanDetail.subscriptionId)
        assertThat(actual?.featuresList).isEqualTo(expectedAccountPlanDetail.featuresList)
        assertThat(actual?.isFreeTrial).isEqualTo(expectedAccountPlanDetail.isFreeTrial)
    }
}