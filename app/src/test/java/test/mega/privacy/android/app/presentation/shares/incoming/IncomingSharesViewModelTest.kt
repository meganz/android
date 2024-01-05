package test.mega.privacy.android.app.presentation.shares.incoming

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesViewModel
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorRefreshSessionUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetVerifiedIncomingSharesUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.di.TestSortOrderUseCases.getOthersSortOrder

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class])
class IncomingSharesViewModelTest {
    private lateinit var underTest: IncomingSharesViewModel

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getParentNodeUseCase = mock<GetParentNodeUseCase>()
    private val authorizeNode = mock<AuthorizeNode>()
    private val getIncomingSharesChildrenNode = mock<GetIncomingSharesChildrenNode>()
    private val monitorRefreshSessionUseCase = mock<MonitorRefreshSessionUseCase>()
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase = mock()
    private val getIncomingShareParentUserEmailUseCase: GetIncomingShareParentUserEmailUseCase =
        mock()
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase = mock()

    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val getOtherSortOrder = mock<GetOthersSortOrder>()
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorContactUpdates = MutableSharedFlow<UserUpdate>()

    private val getUnverifiedIncomingShares = mock<GetUnverifiedIncomingShares>()
    private val getVerifiedIncomingSharesUseCase = mock<GetVerifiedIncomingSharesUseCase>()
    private val refreshSessionFlow = MutableSharedFlow<Unit>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun resetMock() {
        reset(
            getNodeByHandle,
            authorizeNode,
            getParentNodeUseCase,
            getIncomingSharesChildrenNode,
            getCloudSortOrder,
            getOthersSortOrder,
            monitorNodeUpdatesUseCase,
            getUnverifiedIncomingShares,
            getVerifiedIncomingSharesUseCase,
            monitorRefreshSessionUseCase,
            getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase,
            getIncomingShareParentUserEmailUseCase,
        )
        runBlocking { stubCommon() }
        initViewModel()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        underTest = IncomingSharesViewModel(
            getNodeByHandle = getNodeByHandle,
            authorizeNode = authorizeNode,
            getParentNodeUseCase = getParentNodeUseCase,
            getIncomingSharesChildrenNode = getIncomingSharesChildrenNode,
            getCloudSortOrder = getCloudSortOrder,
            getOthersSortOrder = getOtherSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorContactUpdates = { monitorContactUpdates },
            getUnverifiedIncomingShares = getUnverifiedIncomingShares,
            getVerifiedIncomingSharesUseCase = getVerifiedIncomingSharesUseCase,
            monitorRefreshSessionUseCase = monitorRefreshSessionUseCase,
            getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
            getIncomingShareParentUserEmailUseCase = getIncomingShareParentUserEmailUseCase,
        )
    }

    private suspend fun stubCommon() {
        whenever(monitorRefreshSessionUseCase()).thenReturn(refreshSessionFlow)
        whenever(areCredentialsVerifiedUseCase(any())).thenReturn(true)
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(getOtherSortOrder()).thenReturn(SortOrder.ORDER_DEFAULT_DESC)
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(getUnverifiedIncomingShares(any())).thenReturn(emptyList())
        whenever(getVerifiedIncomingSharesUseCase(any())).thenReturn(emptyList())
        whenever(getContactVerificationWarningUseCase()).thenReturn(true)
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.currentViewType).isEqualTo(ViewType.LIST)
            assertThat(initial.incomingHandle).isEqualTo(-1L)
            assertThat(initial.incomingTreeDepth).isEqualTo(0)
            assertThat(initial.nodes).isEmpty()
            assertThat(initial.isInvalidHandle).isEqualTo(true)
            assertThat(initial.incomingParentHandle).isEqualTo(null)
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
        }
    }

    @Test
    fun `test that incoming tree depth is increased when calling increaseIncomingTreeDepth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.increaseIncomingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(1)
                }
        }

    @Test
    fun `test that incoming tree depth is decreased when calling decreaseIncomingTreeDepth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.setIncomingTreeDepth(3, any())
                    assertThat(awaitItem()).isEqualTo(3)
                    underTest.decreaseIncomingTreeDepth(any())
                    assertThat(awaitItem()).isEqualTo(2)
                }
        }

    @Test
    fun `test that incoming tree depth is updated when set incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    val newValue = 1
                    assertThat(awaitItem()).isEqualTo(0)
                    underTest.setIncomingTreeDepth(newValue, any())
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming tree depth is reset to 0 if fails to get node list when calling set incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())
            underTest.setIncomingTreeDepth(3, any())

            underTest.state.map { it.incomingTreeDepth }
                .test {
                    assertThat(awaitItem()).isEqualTo(3)
                    whenever(getIncomingSharesChildrenNode(any())).thenReturn(null)
                    underTest.setIncomingTreeDepth(2, any())
                    assertThat(awaitItem()).isEqualTo(0)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that incoming tree depth equals 0 if resetIncomingTreeDepth`() =
        runTest {
            underTest.state.map { it.incomingTreeDepth }.distinctUntilChanged()
                .test {
                    underTest.resetIncomingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(0)
                }
        }

    @Test
    fun `test that incoming handle is updated when increase incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseIncomingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming handle is updated when decrease incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.decreaseIncomingTreeDepth(newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming handle is updated when set incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())

            underTest.state.map { it.incomingHandle }.distinctUntilChanged()
                .test {
                    val newValue = 123456789L
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.setIncomingTreeDepth(any(), newValue)
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that incoming handle is reset to default if fails to get node list when calling set incoming tree depth`() =
        runTest {
            whenever(getVerifiedIncomingSharesUseCase(underTest.state.value.sortOrder))
                .thenReturn(emptyList())
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(null)
            underTest.setIncomingTreeDepth(0, 123456789L)

            underTest.state.map { it.incomingHandle }
                .test {
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    underTest.setIncomingTreeDepth(1, 987654321L)
                    assertThat(awaitItem()).isEqualTo(-1L)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that incoming handle is set to -1L when reset incoming tree depth`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.increaseIncomingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(123456789L)
                    underTest.resetIncomingTreeDepth()
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that is invalid handle is set to false when call set incoming tree depth with valid handle`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(listOf(mock()))
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.setIncomingTreeDepth(1, 123456789L)
                    assertThat(awaitItem()).isEqualTo(false)
                }
        }

    @Test
    fun `test that is invalid handle is set to true when call set incoming tree depth with invalid handle`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(listOf(mock()))
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.setIncomingTreeDepth(1, 123456789L)
                    assertThat(awaitItem()).isEqualTo(false)
                    underTest.setIncomingTreeDepth(1, -1L)
                    assertThat(awaitItem()).isEqualTo(true)
                }
        }


    @Test
    fun `test that is invalid handle is set to true when cannot retrieve node`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(listOf(mock()))
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.isInvalidHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(true)
                    underTest.setIncomingTreeDepth(1, 123456789L)
                    assertThat(awaitItem()).isEqualTo(false)

                    whenever(getNodeByHandle(any())).thenReturn(null)

                    underTest.setIncomingTreeDepth(1, 987654321L)
                    assertThat(awaitItem()).isEqualTo(true)
                }
        }

    @Test
    fun `test that getIncomingSharesNode executes when calling increaseIncomingTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.increaseIncomingTreeDepth(handle)
            verify(getIncomingSharesChildrenNode).invoke(handle)
        }

    @Test
    fun `test that getIncomingSharesNode executes when calling decreaseIncomingTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.decreaseIncomingTreeDepth(handle)
            verify(getIncomingSharesChildrenNode).invoke(handle)
        }

    @Test
    fun `test that getVerifiedIncomingSharesUseCase executes when set incoming tree depth to 0`() =
        runTest {
            val handle = 123456789L
            underTest.setIncomingTreeDepth(0, handle)
            verify(getVerifiedIncomingSharesUseCase).invoke(underTest.state.value.sortOrder)
        }

    @Test
    fun `test that getVerifiedIncomingSharesUseCase does not execute when set incoming tree depth to greater than 0`() =
        runTest {
            val handle = 123456789L
            // initialization
            verify(getVerifiedIncomingSharesUseCase).invoke(underTest.state.value.sortOrder)
            underTest.setIncomingTreeDepth(1, handle)
            verifyNoMoreInteractions(getVerifiedIncomingSharesUseCase)
        }

    @Test
    fun `test that getIncomingSharesNode executes when set incoming tree depth to greater than 0`() =
        runTest {
            val handle = 123456789L
            whenever(getNodeByHandle(any())).thenReturn(null)
            whenever(getParentNodeUseCase(NodeId(any()))).thenReturn(null)
            underTest.setIncomingTreeDepth(1, handle)
            verify(getIncomingSharesChildrenNode).invoke(handle)
        }

    @Test
    fun `test that getIncomingSharesNode does not execute when set incoming tree depth to 0`() =
        runTest {
            val handle = 123456789L
            underTest.setIncomingTreeDepth(0, handle)
            verifyNoInteractions(getIncomingSharesChildrenNode)
        }

    @Test
    fun `test that getVerifiedIncomingSharesUseCase executes when resetIncomingTreeDepth`() =
        runTest {
            underTest.resetIncomingTreeDepth()
            verify(getVerifiedIncomingSharesUseCase).invoke(underTest.state.value.sortOrder)
        }

    @Test
    fun `test that getUnverifiedIncomingShares executes when refresh and incoming tree depth is set to 0`() =
        runTest {
            val handle = 123456789L
            // initialization
            verify(getUnverifiedIncomingShares).invoke(underTest.state.value.sortOrder)
            underTest.setIncomingTreeDepth(0, handle)
            verify(getUnverifiedIncomingShares).invoke(underTest.state.value.sortOrder)
        }

    @Test
    fun `test that getUnverifiedIncomingShares does not execute when refresh and incoming tree depth is set to greater than 0`() =
        runTest {
            val handle = 123456789L
            // initialization
            verify(getUnverifiedIncomingShares).invoke(underTest.state.value.sortOrder)
            underTest.setIncomingTreeDepth(1, handle)
            verifyNoMoreInteractions(getUnverifiedIncomingShares)
        }

    @Test
    fun `test that getIncomingSharesNode executes when refresh`() =
        runTest {
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())
            whenever(getUnverifiedIncomingShares(any())).thenReturn(emptyList())
            val handle = 123456789L
            val job = underTest.increaseIncomingTreeDepth(handle)
            job.invokeOnCompletion {
                assertThat(underTest.state.value.incomingHandle).isEqualTo(handle)
                underTest.refreshIncomingSharesNode()
            }
            // increaseIncomingTreeDepth call + refreshIncomingSharesNode call
            verify(getIncomingSharesChildrenNode, times(2)).invoke(handle)
        }

    @Test
    fun `test that nodes is set with result of getIncomingSharesChildrenNode if not null`() =
        runTest {
            val node1 = mock<MegaNode> {
                on { this.handle }.thenReturn(1234L)
            }
            val node2 = mock<MegaNode> {
                on { this.handle }.thenReturn(5678L)
            }
            val expected = listOf(Pair(node1, null), Pair(node2, null))

            whenever(getIncomingSharesChildrenNode(any())).thenReturn(expected.map { it.first })

            underTest.state.map { it.nodes }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEmpty()
                    underTest.increaseIncomingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that nodes is empty if result of getIncomingSharesChildrenNode null`() =
        runTest {
            val node1 = mock<MegaNode> {
                on { this.handle }.thenReturn(1234L)
            }
            val node2 = mock<MegaNode> {
                on { this.handle }.thenReturn(5678L)
            }
            val expected = listOf(Pair(node1, null), Pair(node2, null))

            whenever(getIncomingSharesChildrenNode(123456789L)).thenReturn(expected.map { it.first })
            whenever(getIncomingSharesChildrenNode(987654321L)).thenReturn(null)

            underTest.state.map { it.nodes }.distinctUntilChanged()
                .test {
                    underTest.increaseIncomingTreeDepth(123456789L).invokeOnCompletion {
                        underTest.increaseIncomingTreeDepth(987654321L)
                    }
                    assertThat(awaitItem()).isEmpty()
                    assertThat(awaitItem()).isEqualTo(expected)
                    assertThat(awaitItem()).isEmpty()
                }
        }

    @Test
    fun `test that getParentNodeUseCase is called when setIncomingTreeDepth`() =
        runTest {
            val handle = 123456789L
            underTest.increaseIncomingTreeDepth(handle)
            verify(getParentNodeUseCase).invoke(NodeId(handle))
        }

    @Test
    fun `test that parent handle is set with result of getParentNodeUseCase`() =
        runTest {
            val expected = 111111111L
            val parentNode = mock<TypedFileNode> {
                on { this.id }.thenReturn(NodeId(expected))
            }
            whenever(getParentNodeUseCase(NodeId(any()))).thenReturn(parentNode)
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.incomingParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(null)
                    underTest.increaseIncomingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that parent handle is set to null when refreshNodes fails`() =
        runTest {
            val parentNode = mock<TypedFileNode> {
                on { this.id }.thenReturn(NodeId(111111111L))
            }
            whenever(getParentNodeUseCase(NodeId(any()))).thenReturn(parentNode)
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(listOf())
            whenever(getNodeByHandle(any())).thenReturn(mock())

            underTest.state.map { it.incomingParentHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(null)
                    underTest.increaseIncomingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(111111111L)
                    whenever(getIncomingSharesChildrenNode(any())).thenReturn(null)
                    underTest.increaseIncomingTreeDepth(123456789L)
                    assertThat(awaitItem()).isEqualTo(null)
                }
        }

    @Test
    fun `test that sort order is set with result of getOthersSortOrder if depth is equals to 0 when call setIncomingTreeDepth`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(mock())
            whenever(getOtherSortOrder()).thenReturn(expected)
            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.setIncomingTreeDepth(0, any())
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that sort order is set with result of getCloudSortOrder if depth is different than 0 when call setIncomingTreeDepth`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())
            whenever(getCloudSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.setIncomingTreeDepth(1, any())
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that sort order is set with result of getOtherSortOrder when refreshNodes fails`() =
        runTest {
            val default = SortOrder.ORDER_NONE
            val expected = SortOrder.ORDER_CREATION_ASC
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(null)
            whenever(getOtherSortOrder()).thenReturn(expected)

            underTest.state.map { it.sortOrder }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(default)
                    underTest.setIncomingTreeDepth(1, any())
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that if monitor node update returns the current node and node is not retrieved, redirect to root of incoming shares`() =
        runTest {
            val handle = 123456789L
            val node = mock<Node> {
                on { this.id }.thenReturn(NodeId(handle))
                on { this.isIncomingShare }.thenReturn(true)
            }
            val map = mapOf(node to emptyList<NodeChanges>())
            whenever(getNodeByHandle(any())).thenReturn(null)
            whenever(authorizeNode(any())).thenReturn(null)
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.setIncomingTreeDepth(any(), handle)
                    assertThat(awaitItem()).isEqualTo(handle)
                    monitorNodeUpdatesFakeFlow.emit(NodeUpdate(map))
                    assertThat(awaitItem()).isEqualTo(-1L)
                }
        }

    @Test
    fun `test that if monitor node update does not returns the current node, do not redirect to root of incoming shares`() =
        runTest {
            val handle = 123456789L
            whenever(getIncomingSharesChildrenNode(any())).thenReturn(emptyList())

            underTest.state.map { it.incomingHandle }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(-1L)
                    underTest.setIncomingTreeDepth(any(), handle)
                    assertThat(awaitItem()).isEqualTo(handle)
                    monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
                }
        }

    @Test
    fun `test that refresh nodes is called when receiving a node update`() = runTest {
        monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
        // initialization call + receiving a node update call
        verify(
            getVerifiedIncomingSharesUseCase,
            times(5)
        ).invoke(underTest.state.value.sortOrder)
    }

    @Test
    fun `test that the list view type is set when updating the current view type`() =
        testSetCurrentViewType(ViewType.LIST)

    @Test
    fun `test that the grid view type is set when updating the current view type`() =
        testSetCurrentViewType(ViewType.GRID)

    private fun testSetCurrentViewType(expectedValue: ViewType) = runTest {
        underTest.setCurrentViewType(expectedValue)

        underTest.state.test {
            assertThat(awaitItem().currentViewType).isEqualTo(expectedValue)
        }
    }
}
