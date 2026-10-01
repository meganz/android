package mega.privacy.android.domain.usecase.node.hiddennode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.HideNodesResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.HideNodesException
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

/**
 * Test class for [UnhideNodesUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UnhideNodesUseCaseTest {

    private lateinit var underTest: UnhideNodesUseCase

    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(
            updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
        )
    }

    private fun initUseCase() {
        underTest = UnhideNodesUseCase(
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isBusinessStatusUseCase = getBusinessStatusUseCase,
        )
    }

    private fun createAccountDetail(isPaid: Boolean = true): AccountDetail {
        val accountType = if (isPaid) AccountType.PRO_I else AccountType.FREE
        val accountPlanDetail = AccountPlanDetail(
            accountType = accountType,
            isProPlan = isPaid,
            expirationTime = null,
            subscriptionId = null,
            featuresList = emptyList(),
            isFreeTrial = false,
        )
        val levelDetail = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = null,
            subscriptionRenewTime = 0L,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            proExpirationTime = 0L,
            accountPlanDetail = accountPlanDetail,
            accountSubscriptionDetailList = emptyList(),
        )
        return AccountDetail(levelDetail = levelDetail)
    }

    @Test
    fun `test that Unauthorized is thrown when account is not paid`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = false)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        initUseCase()

        assertFailsWith<HideNodesException.Unauthorized> {
            underTest(setOf(NodeId(1L)))
        }
    }

    @Test
    fun `test that Unauthorized is thrown when business account is expired`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)
        initUseCase()

        assertFailsWith<HideNodesException.Unauthorized> {
            underTest(setOf(NodeId(1L)))
        }
    }

    @Test
    fun `test that HideNodesResult with all success is returned when all nodes are unhidden`() =
        runTest {
            val nodes = setOf(NodeId(1L), NodeId(2L), NodeId(3L))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            initUseCase()

            val result = underTest(nodes)

            assertThat(result).isEqualTo(HideNodesResult(success = 3, failed = 0))
        }

    @Test
    fun `test that HideNodesResult reflects partial failure when some nodes fail to unhide`() =
        runTest {
            val nodeId1 = NodeId(1L)
            val nodeId2 = NodeId(2L)
            val nodeId3 = NodeId(3L)
            val nodes = setOf(nodeId1, nodeId2, nodeId3)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            whenever(updateNodeSensitiveUseCase(nodeId = nodeId1, isSensitive = false))
                .thenThrow(RuntimeException("node update failed"))
            initUseCase()

            val result = underTest(nodes)

            assertThat(result).isEqualTo(HideNodesResult(success = 2, failed = 1))
        }

    @Test
    fun `test that HideNodesResult reflects all failed when all nodes fail to unhide`() = runTest {
        val nodeId1 = NodeId(1L)
        val nodeId2 = NodeId(2L)
        val nodes = setOf(nodeId1, nodeId2)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        whenever(updateNodeSensitiveUseCase(nodeId = nodeId1, isSensitive = false))
            .thenThrow(RuntimeException("node update failed"))
        whenever(updateNodeSensitiveUseCase(nodeId = nodeId2, isSensitive = false))
            .thenThrow(RuntimeException("node update failed"))
        initUseCase()

        val result = underTest(nodes)

        assertThat(result).isEqualTo(HideNodesResult(success = 0, failed = 2))
    }

    @Test
    fun `test that updateNodeSensitiveUseCase is called with isSensitive false for each node`() =
        runTest {
            val nodeId1 = NodeId(1L)
            val nodeId2 = NodeId(2L)
            val nodes = setOf(nodeId1, nodeId2)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            initUseCase()

            underTest(nodes)

            verify(updateNodeSensitiveUseCase).invoke(nodeId = nodeId1, isSensitive = false)
            verify(updateNodeSensitiveUseCase).invoke(nodeId = nodeId2, isSensitive = false)
        }

    @Test
    fun `test that HideNodesResult with zero counts is returned for empty node set`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        initUseCase()

        val result = underTest(emptySet())

        assertThat(result).isEqualTo(HideNodesResult(success = 0, failed = 0))
    }

    @Test
    fun `test that nodes are unhidden successfully when business account is active`() = runTest {
        val nodeId = NodeId(1L)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        initUseCase()

        val result = underTest(setOf(nodeId))

        assertThat(result).isEqualTo(HideNodesResult(success = 1, failed = 0))
    }
}
