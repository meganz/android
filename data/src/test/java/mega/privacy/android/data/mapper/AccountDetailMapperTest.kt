package mega.privacy.android.data.mapper

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountSessionDetail
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaAccountSession
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
internal class AccountDetailMapperTest {
    private val expectedUsedStorage: Long = 1003L
    private val expectedSubscriptionMethodId: Int = 100
    private val expectedTransferMax: Long = 10000L
    private val expectedTransferUsed: Long = 7777L
    private val expectedAccountType: AccountType = AccountType.PRO_I
    private val expectedSubscriptionStatus: SubscriptionStatus = SubscriptionStatus.VALID
    private val expectedSubscriptionRenewTime: Long = 1873874783274L
    private val expectedProExpirationTime: Long = 378672463728467L
    private val accountStorageDetailMapper: AccountStorageDetailMapper = mock()
    private val accountSessionDetailMapper: AccountSessionDetailMapper = mock()
    private val accountTransferDetailMapper: AccountTransferDetailMapper = mock()
    private val accountLevelDetailMapper: AccountLevelDetailMapper = mock()
    private val accountTypeMapper: AccountTypeMapper = mock()
    private val subscriptionStatusMapper: SubscriptionStatusMapper = mock()
    private val megaAccountSession: MegaAccountSession = mock()

    private val megaAccountDetails = mock<MegaAccountDetails> {
        on { this.storageUsed }.thenReturn(expectedUsedStorage)
        on { this.subscriptionMethodId }.thenReturn(expectedSubscriptionMethodId)
        on { this.transferMax }.thenReturn(expectedTransferMax)
        on { this.transferUsed }.thenReturn(expectedTransferUsed)
        on { this.subscriptionRenewTime }.thenReturn(expectedSubscriptionRenewTime)
        on { this.proExpiration }.thenReturn(expectedProExpirationTime)
        on { this.proLevel }.thenReturn(MegaAccountDetails.ACCOUNT_TYPE_FREE)
        on { this.subscriptionStatus }.thenReturn(MegaAccountDetails.SUBSCRIPTION_STATUS_NONE)
        on { this.getSession(0) }.thenReturn(megaAccountSession)
    }

    @Test
    fun `test that return levelDetail when numDetails enable HAS_PRO_DETAILS flag`() =
        runTest {
            val rootNode = mock<MegaNode>()
            val rubbishNode = mock<MegaNode>()
            val inShareNodes = listOf(mock<MegaNode>())
            val numDetails = HAS_PRO_DETAILS
            whenever(accountTypeMapper(MegaAccountDetails.ACCOUNT_TYPE_FREE)).thenReturn(
                expectedAccountType)
            whenever(subscriptionStatusMapper(MegaAccountDetails.SUBSCRIPTION_STATUS_NONE)).thenReturn(
                expectedSubscriptionStatus)
            whenever(
                accountLevelDetailMapper.invoke(
                    expectedSubscriptionRenewTime,
                    expectedProExpirationTime,
                    expectedAccountType,
                    expectedSubscriptionStatus,
                ),
            ).thenReturn(
                AccountLevelDetail(
                    accountType = expectedAccountType,
                    subscriptionStatus = expectedSubscriptionStatus,
                    subscriptionRenewTime = expectedSubscriptionRenewTime,
                    proExpirationTime = expectedProExpirationTime,
                ))
            val actual = toAccountDetail(
                details = megaAccountDetails,
                numDetails = numDetails,
                rootNode = rootNode,
                rubbishNode = rubbishNode,
                inShares = inShareNodes,
                accountStorageDetailMapper = accountStorageDetailMapper,
                accountSessionDetailMapper = accountSessionDetailMapper,
                accountTransferDetailMapper = accountTransferDetailMapper,
                accountLevelDetailMapper = accountLevelDetailMapper,
                accountTypeMapper = accountTypeMapper,
                subscriptionStatusMapper = subscriptionStatusMapper
            )
            assertEquals(expectedSubscriptionRenewTime, actual.levelDetail?.subscriptionRenewTime)
            assertEquals(expectedProExpirationTime, actual.levelDetail?.proExpirationTime)
            assertEquals(expectedAccountType, actual.levelDetail?.accountType)
            assertEquals(expectedSubscriptionStatus, actual.levelDetail?.subscriptionStatus)
            assertNull(actual.sessionDetail)
            assertNull(actual.transferDetail)
            assertNull(actual.storageDetail)
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
                expectedAccountType)
            whenever(subscriptionStatusMapper(MegaAccountDetails.SUBSCRIPTION_STATUS_NONE)).thenReturn(
                expectedSubscriptionStatus)
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
            val actual = toAccountDetail(
                details = megaAccountDetails,
                numDetails = numDetails,
                rootNode = rootNode,
                rubbishNode = rubbishNode,
                inShares = inShareNodes,
                accountStorageDetailMapper = accountStorageDetailMapper,
                accountSessionDetailMapper = accountSessionDetailMapper,
                accountTransferDetailMapper = accountTransferDetailMapper,
                accountLevelDetailMapper = accountLevelDetailMapper,
                accountTypeMapper = accountTypeMapper,
                subscriptionStatusMapper = subscriptionStatusMapper
            )
            assertEquals(expectedMostRecentUsage, actual.sessionDetail?.mostRecentSessionTimeStamp)
            assertEquals(expectedCreationTimestamp, actual.sessionDetail?.createSessionTimeStamp)
            assertNull(actual.levelDetail)
            assertNull(actual.transferDetail)
            assertNull(actual.storageDetail)
        }
}