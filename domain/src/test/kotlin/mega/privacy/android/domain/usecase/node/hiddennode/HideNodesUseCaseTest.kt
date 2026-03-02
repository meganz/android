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
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFailsWith

/**
 * Test class for [HideNodesUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HideNodesUseCaseTest {

    private lateinit var underTest: HideNodesUseCase

    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase = mock()
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(
            isHiddenNodesOnboardedUseCase,
            updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
        )
    }

    @BeforeAll
    fun init() {
        underTest = HideNodesUseCase(
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
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
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

        assertThrows<HideNodesException.Unauthorized> {
            underTest(setOf(NodeId(1L)))
        }
    }

    @Test
    fun `test that Unauthorized is thrown when business account is expired`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Expired)
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

        assertFailsWith<HideNodesException.Unauthorized> {
            underTest(setOf(NodeId(1L)))
        }
    }

    @Test
    fun `test that NotOnboarded is thrown when user has not been onboarded`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(false)

        assertFailsWith<HideNodesException.NotOnboarded> {
            underTest(setOf(NodeId(1L)))
        }
    }

    @Test
    fun `test that NotOnboarded is thrown when isHiddenNodesOnboardedUseCase throws`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        whenever(isHiddenNodesOnboardedUseCase()).thenThrow(RuntimeException("onboarding check failed"))

        assertFailsWith<HideNodesException.NotOnboarded> {
            underTest(setOf(NodeId(1L)))
        }
    }

    @Test
    fun `test that HideNodesResult with all success is returned when all nodes are hidden`() =
        runTest {
            val nodes = setOf(NodeId(1L), NodeId(2L), NodeId(3L))
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

            val result = underTest(nodes)

            assertThat(result).isEqualTo(HideNodesResult(success = 3, failed = 0))
        }

    @Test
    fun `test that HideNodesResult reflects partial failure when some nodes fail to hide`() =
        runTest {
            val nodeId1 = NodeId(1L)
            val nodeId2 = NodeId(2L)
            val nodeId3 = NodeId(3L)
            val nodes = setOf(nodeId1, nodeId2, nodeId3)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)
            whenever(updateNodeSensitiveUseCase(nodeId = nodeId1, isSensitive = true))
                .thenThrow(RuntimeException("node update failed"))

            val result = underTest(nodes)

            assertThat(result).isEqualTo(HideNodesResult(success = 2, failed = 1))
        }

    @Test
    fun `test that HideNodesResult reflects all failed when all nodes fail to hide`() = runTest {
        val nodeId1 = NodeId(1L)
        val nodeId2 = NodeId(2L)
        val nodes = setOf(nodeId1, nodeId2)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)
        whenever(updateNodeSensitiveUseCase(nodeId = nodeId1, isSensitive = true))
            .thenThrow(RuntimeException("node update failed"))
        whenever(updateNodeSensitiveUseCase(nodeId = nodeId2, isSensitive = true))
            .thenThrow(RuntimeException("node update failed"))

        val result = underTest(nodes)

        assertThat(result).isEqualTo(HideNodesResult(success = 0, failed = 2))
    }

    @Test
    fun `test that updateNodeSensitiveUseCase is called with isSensitive true for each node`() =
        runTest {
            val nodeId1 = NodeId(1L)
            val nodeId2 = NodeId(2L)
            val nodes = setOf(nodeId1, nodeId2)
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
            whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

            underTest(nodes)

            verify(updateNodeSensitiveUseCase).invoke(nodeId = nodeId1, isSensitive = true)
            verify(updateNodeSensitiveUseCase).invoke(nodeId = nodeId2, isSensitive = true)
        }

    @Test
    fun `test that HideNodesResult with zero counts is returned for empty node set`() = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(createAccountDetail(isPaid = true)))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        whenever(isHiddenNodesOnboardedUseCase()).thenReturn(true)

        val result = underTest(emptySet())

        assertThat(result).isEqualTo(HideNodesResult(success = 0, failed = 0))
    }
}
