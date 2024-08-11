package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.AccountDetailMapper.Companion.HAS_PRO_DETAILS
import mega.privacy.android.data.mapper.AccountDetailMapper.Companion.HAS_SESSIONS_DETAILS
import mega.privacy.android.data.mapper.account.AccountPlanDetailMapper
import mega.privacy.android.data.mapper.account.AccountSubscriptionDetailListMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountSessionDetail
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaAccountPlan
import nz.mega.sdk.MegaAccountSession
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaStringList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountDetailMapperTest {
    private val accountStorageDetailMapper: AccountStorageDetailMapper = mock()
    private val accountSessionDetailMapper: AccountSessionDetailMapper = mock()
    private val accountTransferDetailMapper: AccountTransferDetailMapper = mock()
    private val accountLevelDetailMapper: AccountLevelDetailMapper = mock()
    private val accountTypeMapper: AccountTypeMapper = mock()
    private val subscriptionStatusMapper: SubscriptionStatusMapper = mock()
    private val accountSubscriptionCycleMapper: AccountSubscriptionCycleMapper = mock()
    private val megaAccountSession: MegaAccountSession = mock()
    private val accountSubscriptionDetailListMapper: AccountSubscriptionDetailListMapper = mock()
    private val accountPlanDetailMapper: AccountPlanDetailMapper = mock()

    private val underTest: AccountDetailMapper = AccountDetailMapper(
        accountStorageDetailMapper,
        accountSessionDetailMapper,
        accountTransferDetailMapper,
        accountLevelDetailMapper,
        accountTypeMapper,
        subscriptionStatusMapper,
        accountSubscriptionCycleMapper,
        accountPlanDetailMapper,
        accountSubscriptionDetailListMapper
    )

    private val expectedUsedStorage: Long = 1003L
    private val expectedSubscriptionMethodId: Int = 100
    private val expectedTransferMax: Long = 10000L
    private val expectedTransferUsed: Long = 7777L
    private val expectedAccountType: AccountType = AccountType.PRO_I
    private val expectedSubscriptionStatus: SubscriptionStatus = SubscriptionStatus.VALID
    private val expectedSubscriptionRenewTime: Long = 1873874783274L
    private val expectedProExpirationTime: Long = 378672463728467L
    private val expectedSubscriptionRenewCycle = "1 Y"
    private val expectedAccountSubscriptionCycle = AccountSubscriptionCycle.YEARLY
    private val expectedSubscriptionId = "test123"
    private val expectedAccountPlanLevel = 1L
    private val expectedAccountLevel = MegaAccountDetails.ACCOUNT_TYPE_PROI
    private val subscriptionStatusSdk = MegaAccountDetails.SUBSCRIPTION_STATUS_VALID

    private val megaStringList = mock<MegaStringList> {
        on { size() } doReturn 2
        on { get(0) } doReturn "vpn"
        on { get(1) } doReturn "pwm"
    }

    private val megaAccountPlan = mock<MegaAccountPlan> {
        on { this.accountLevel }.thenReturn(expectedAccountPlanLevel)
        on { this.isProPlan }.thenReturn(true)
        on { this.expirationTime }.thenReturn(expectedProExpirationTime)
        on { this.id }.thenReturn(expectedSubscriptionId)
        on { this.features }.thenReturn(megaStringList)
        on { this.isTrial }.thenReturn(false)
    }

    private val megaAccountDetails = mock<MegaAccountDetails> {
        on { this.storageUsed }.thenReturn(expectedUsedStorage)
        on { this.subscriptionMethodId }.thenReturn(expectedSubscriptionMethodId)
        on { this.transferMax }.thenReturn(expectedTransferMax)
        on { this.transferUsed }.thenReturn(expectedTransferUsed)
        on { this.subscriptionRenewTime }.thenReturn(expectedSubscriptionRenewTime)
        on { this.subscriptionCycle }.thenReturn(expectedSubscriptionRenewCycle)
        on { this.proExpiration }.thenReturn(expectedProExpirationTime)
        on { this.proLevel }.thenReturn(expectedAccountLevel)
        on { this.subscriptionStatus }.thenReturn(subscriptionStatusSdk)
        on { this.getSession(0) }.thenReturn(megaAccountSession)
        on { this.numPlans }.thenReturn(1)
        on { this.getPlan(0) }.thenReturn(megaAccountPlan)
    }

    @Test
    fun `test that return levelDetail when numDetails enable HAS_PRO_DETAILS flag`() =
        runTest {
            val rootNode = mock<MegaNode>()
            val rubbishNode = mock<MegaNode>()
            val inShareNodes = listOf(mock<MegaNode>())
            val numDetails = HAS_PRO_DETAILS
            whenever(accountTypeMapper(expectedAccountLevel)).thenReturn(
                expectedAccountType
            )
            whenever(subscriptionStatusMapper(subscriptionStatusSdk)).thenReturn(
                expectedSubscriptionStatus
            )
            whenever(accountSubscriptionCycleMapper(expectedSubscriptionRenewCycle)).thenReturn(
                expectedAccountSubscriptionCycle
            )
            whenever(accountPlanDetailMapper.invoke(megaAccountPlan)).thenReturn(
                null
            )
            whenever(accountSubscriptionDetailListMapper(megaAccountDetails)).thenReturn(
                listOf()
            )
            whenever(
                accountLevelDetailMapper.invoke(
                    expectedSubscriptionRenewTime,
                    expectedProExpirationTime,
                    expectedAccountType,
                    expectedSubscriptionStatus,
                    accountSubscriptionCycleMapper(expectedSubscriptionRenewCycle),
                    accountPlanDetailMapper(megaAccountPlan),
                    accountSubscriptionDetailListMapper(megaAccountDetails),
                ),
            ).thenReturn(
                AccountLevelDetail(
                    accountType = expectedAccountType,
                    subscriptionStatus = expectedSubscriptionStatus,
                    subscriptionRenewTime = expectedSubscriptionRenewTime,
                    proExpirationTime = expectedProExpirationTime,
                    accountSubscriptionCycle = expectedAccountSubscriptionCycle,
                    accountPlanDetail = null,
                    accountSubscriptionDetailList = listOf(),
                )
            )
            val actual = underTest(
                details = megaAccountDetails,
                numDetails = numDetails,
                rootNode = rootNode,
                rubbishNode = rubbishNode,
                inShares = inShareNodes,
            )
            assertThat(actual.levelDetail?.subscriptionRenewTime)
                .isEqualTo(expectedSubscriptionRenewTime)
            assertThat(actual.levelDetail?.proExpirationTime)
                .isEqualTo(expectedProExpirationTime)
            assertThat(actual.levelDetail?.accountType)
                .isEqualTo(expectedAccountType)
            assertThat(actual.levelDetail?.subscriptionStatus)
                .isEqualTo(expectedSubscriptionStatus)
            assertThat(actual.levelDetail?.accountSubscriptionCycle)
                .isEqualTo(expectedAccountSubscriptionCycle)
            assertThat(actual.levelDetail?.accountPlanDetail).isNull()
            assertThat(actual.levelDetail?.accountSubscriptionDetailList).isEmpty()
            assertThat(actual.sessionDetail).isNull()
            assertThat(actual.transferDetail).isNull()
            assertThat(actual.storageDetail).isNull()
        }

    @Test
    fun `test that return sessionDetail when numDetails enable HAS_SESSIONS_DETAILS flag`() =
        runTest {
            val rootNode = mock<MegaNode>()
            val rubbishNode = mock<MegaNode>()
            val inShareNodes = listOf(mock<MegaNode>())
            val numDetails = HAS_SESSIONS_DETAILS
            val expectedMostRecentUsage = 12929L
            val expectedCreationTimestamp = 12939L
            whenever(megaAccountSession.mostRecentUsage).thenReturn(expectedMostRecentUsage)
            whenever(megaAccountSession.creationTimestamp).thenReturn(expectedCreationTimestamp)
            whenever(accountTypeMapper(MegaAccountDetails.ACCOUNT_TYPE_FREE)).thenReturn(
                expectedAccountType
            )
            whenever(subscriptionStatusMapper(MegaAccountDetails.SUBSCRIPTION_STATUS_NONE)).thenReturn(
                expectedSubscriptionStatus
            )
            whenever(
                accountSessionDetailMapper.invoke(
                    expectedMostRecentUsage,
                    expectedCreationTimestamp,
                ),
            ).thenReturn(
                AccountSessionDetail(
                    mostRecentSessionTimeStamp = expectedMostRecentUsage,
                    createSessionTimeStamp = expectedCreationTimestamp,
                ),
            )
            val actual = underTest(
                details = megaAccountDetails,
                numDetails = numDetails,
                rootNode = rootNode,
                rubbishNode = rubbishNode,
                inShares = inShareNodes,
            )
            assertThat(actual.sessionDetail?.mostRecentSessionTimeStamp)
                .isEqualTo(expectedMostRecentUsage)
            assertThat(actual.sessionDetail?.createSessionTimeStamp)
                .isEqualTo(expectedCreationTimestamp)
            assertThat(actual.levelDetail).isNull()
            assertThat(actual.transferDetail).isNull()
            assertThat(actual.storageDetail).isNull()
        }
}