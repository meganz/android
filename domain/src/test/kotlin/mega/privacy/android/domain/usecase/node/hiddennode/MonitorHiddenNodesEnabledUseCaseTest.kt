package mega.privacy.android.domain.usecase.node.hiddennode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MonitorHiddenNodesEnabledUseCaseTest {

    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()

    private lateinit var underTest: MonitorHiddenNodesEnabledUseCase

    @BeforeEach
    fun setUp() {
        underTest = MonitorHiddenNodesEnabledUseCase(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
        )
    }

    @Test
    fun `test that hidden nodes are enabled for paid account`() = runTest {
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_I
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for PRO_LITE account`() = runTest {
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_LITE
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for PRO_II account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_II
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for PRO_III account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_III
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for PRO_FLEXI account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_FLEXI
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for STARTER account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.STARTER
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for BASIC account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.BASIC
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for ESSENTIAL account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.ESSENTIAL
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are disabled for FREE account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.FREE
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are disabled for UNKNOWN account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.UNKNOWN
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for active BUSINESS account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.BUSINESS
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for grace period BUSINESS account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.BUSINESS
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.GracePeriod)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are disabled for expired BUSINESS account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.BUSINESS
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are disabled for inactive BUSINESS account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.BUSINESS
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Inactive)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are enabled for active PRO_FLEXI account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_FLEXI
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are disabled for expired PRO_FLEXI account`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_FLEXI
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are disabled when level detail is null`() = runTest {

        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn null
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that hidden nodes are disabled when account type is null`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn null
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that business status check handles exceptions gracefully`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.BUSINESS
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenThrow(RuntimeException("Test exception"))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that business status check handles null return value gracefully`() = runTest {

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.BUSINESS
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
        whenever(getBusinessStatusUseCase()).thenReturn(null)

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            awaitComplete()
        }
    }
}