package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock

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

    @Test
    fun `test when rootNode and rubbishNode and inSharesNodes different than null then return correct value`() =
        runTest {
            val rootNode = mock<MegaNode>()
            val rubbishNode = mock<MegaNode>()
            val inShareNodes = listOf(mock<MegaNode>())
            val megaAccountDetails = mock<MegaAccountDetails> {
                on { this.storageUsed }.thenReturn(expectedUsedStorage)
                on { this.subscriptionMethodId }.thenReturn(expectedSubscriptionMethodId)
                on { this.transferMax }.thenReturn(expectedTransferMax)
                on { this.transferUsed }.thenReturn(expectedTransferUsed)
                on { this.subscriptionRenewTime }.thenReturn(expectedSubscriptionRenewTime)
                on { this.proExpiration }.thenReturn(expectedProExpirationTime)
            }
            val actual = toAccountDetail(
                details = megaAccountDetails,
                rootNode = rootNode,
                rubbishNode = rubbishNode,
                inShares = inShareNodes,
                getStorageUsed = { _, _ -> expectedUsedStorage },
                accountTypeMapper = { expectedAccountType },
                subscriptionStatusMapper = { expectedSubscriptionStatus }
            )
            assertThat(actual.accountType).isEqualTo(expectedAccountType)
            assertThat(actual.accountType).isEqualTo(expectedAccountType)
            assertThat(actual.subscriptionMethodId).isEqualTo(expectedSubscriptionMethodId)
            assertThat(actual.transferUsed).isEqualTo(expectedTransferUsed)
            assertThat(actual.transferMax).isEqualTo(expectedTransferMax)
            assertThat(actual.proExpirationTime).isEqualTo(expectedProExpirationTime)
            assertThat(actual.subscriptionRenewTime).isEqualTo(expectedSubscriptionRenewTime)
            assertThat(actual.usedRubbish).isEqualTo(expectedUsedStorage)
            assertThat(actual.usedCloudDrive).isEqualTo(expectedUsedStorage)
            assertThat(actual.usedIncoming).isEqualTo(expectedUsedStorage)
        }

    @Test
    fun `test when rootNode or rubbishNode or inSharesNodes null then return correct value`() =
        runTest {
            val megaAccountDetails = mock<MegaAccountDetails> {
                on { this.storageUsed }.thenReturn(expectedUsedStorage)
                on { this.subscriptionMethodId }.thenReturn(expectedSubscriptionMethodId)
                on { this.transferMax }.thenReturn(expectedTransferMax)
                on { this.transferUsed }.thenReturn(expectedTransferUsed)
                on { this.subscriptionRenewTime }.thenReturn(expectedSubscriptionRenewTime)
                on { this.proExpiration }.thenReturn(expectedProExpirationTime)
            }
            val actual = toAccountDetail(
                details = megaAccountDetails,
                rootNode = null,
                rubbishNode = null,
                inShares = null,
                getStorageUsed = { _, _ -> expectedUsedStorage },
                accountTypeMapper = { expectedAccountType },
                subscriptionStatusMapper = { expectedSubscriptionStatus }
            )
            assertThat(actual.accountType).isEqualTo(expectedAccountType)
            assertThat(actual.accountType).isEqualTo(expectedAccountType)
            assertThat(actual.subscriptionMethodId).isEqualTo(expectedSubscriptionMethodId)
            assertThat(actual.transferUsed).isEqualTo(expectedTransferUsed)
            assertThat(actual.transferMax).isEqualTo(expectedTransferMax)
            assertThat(actual.proExpirationTime).isEqualTo(expectedProExpirationTime)
            assertThat(actual.subscriptionRenewTime).isEqualTo(expectedSubscriptionRenewTime)
            assertThat(actual.usedRubbish).isEqualTo(0L)
            assertThat(actual.usedCloudDrive).isEqualTo(0L)
            assertThat(actual.usedIncoming).isEqualTo(0L)
        }
}