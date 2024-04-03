package test.mega.privacy.android.app.presentation.recentactions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.recentactions.RecentActionsComposeViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentActionsComposeViewModelTest {
    private lateinit var underTest: RecentActionsComposeViewModel

    private val getRecentActionsUseCase = mock<GetRecentActionsUseCase>()
    private val setHideRecentActivityUseCase = mock<SetHideRecentActivityUseCase>()
    private val monitorHideRecentActivityUseCase = mock<MonitorHideRecentActivityUseCase>()
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()

    private val node: TypedFileNode = mock {
        on { id }.thenReturn(NodeId(123))
        on { isNodeKeyDecrypted }.thenReturn(false)
    }

    private val megaRecentActionBucket = mock<RecentActionBucket> {
        on { this.nodes }.thenReturn(listOf(node))
        on { this.parentNodeId }.thenReturn(NodeId(321))
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(1711960000)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
        on { this.isKeyVerified }.thenReturn(true)
    }

    private val megaRecentActionBucket2 = mock<RecentActionBucket> {
        on { this.nodes }.thenReturn(listOf(node))
        on { this.parentNodeId }.thenReturn(NodeId(111))
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(1711961075)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }

    private val megaRecentActionBucket3 = mock<RecentActionBucket> {
        on { this.nodes }.thenReturn(listOf(node))
        on { this.parentNodeId }.thenReturn(NodeId(111))
        on { this.isMedia }.thenReturn(false)
        on { this.timestamp }.thenReturn(1711961075)
        on { this.userEmail }.thenReturn("aaa@aaa.com")
        on { this.isUpdate }.thenReturn(false)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getRecentActionsUseCase,
            setHideRecentActivityUseCase,
            monitorHideRecentActivityUseCase,
            monitorNodeUpdatesUseCase,
        )
        runBlocking {
            stubCommon()
        }
        underTest = RecentActionsComposeViewModel(
            getRecentActionsUseCase = getRecentActionsUseCase,
            setHideRecentActivityUseCase = setHideRecentActivityUseCase,
            monitorHideRecentActivityUseCase = monitorHideRecentActivityUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
        )
    }

    private suspend fun stubCommon() {
        whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket2))
        whenever(monitorHideRecentActivityUseCase()).thenReturn(flow {
            emit(true)
            emit(false)
        })
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.hideRecentActivity).isEqualTo(false)
        }
    }

    @Test
    fun `test that recent action items is updated at initialization`() =
        runTest {
            underTest.uiState.map { it.groupedRecentActionItems }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem().totalSize()).isEqualTo(1)
                }
        }

    @Test
    fun `test that recent action items is updated when receiving a node update`() =
        runTest {
            underTest.uiState.map { it.groupedRecentActionItems }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem().totalSize()).isEqualTo(1)
                    advanceUntilIdle()
                    monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
                    whenever(getRecentActionsUseCase()).thenReturn(
                        listOf(megaRecentActionBucket, megaRecentActionBucket3)
                    )
                    assertThat(awaitItem().totalSize()).isEqualTo(2)
                }
        }

    @Test
    fun `test that recent action items are grouped by timestamp`() =
        runTest {
            underTest.uiState.map { it.groupedRecentActionItems }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem().totalSize()).isEqualTo(1)
                    advanceUntilIdle()
                    monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
                    whenever(getRecentActionsUseCase()).thenReturn(
                        listOf(
                            megaRecentActionBucket,
                            megaRecentActionBucket2,
                            megaRecentActionBucket3
                        )
                    )
                    val result = awaitItem()
                    assertThat(result.totalSize()).isEqualTo(3)
                    assertThat(result.size).isEqualTo(2)
                    assertThat(result.keys.first()).isEqualTo(1711960000)
                    assertThat(result.keys.last()).isEqualTo(1711961075)
                }
        }

    @Test
    fun `test that hide recent activity is set with value of monitor hide recent activity`() =
        runTest {
            underTest.uiState.map { it.hideRecentActivity }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(false)
                    assertThat(awaitItem()).isEqualTo(true)
                    assertThat(awaitItem()).isEqualTo(false)
                }
        }

    @Test
    fun `test that disable hide recent activity calls setHideRecentActivity use case with value true`() =
        runTest {
            underTest.disableHideRecentActivitySetting()
            advanceUntilIdle()
            verify(setHideRecentActivityUseCase).invoke(false)
        }

    @Test
    fun `test that list of recent buckets is returned when getAllRecentBuckets is invoked`() =
        runTest {
            underTest.uiState.map { it.groupedRecentActionItems }.distinctUntilChanged().test {
                awaitItem()
                advanceUntilIdle()
                monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
                whenever(getRecentActionsUseCase()).thenReturn(
                    listOf(
                        megaRecentActionBucket,
                        megaRecentActionBucket2,
                        megaRecentActionBucket3
                    )
                )
                awaitItem()
                val result = underTest.getAllRecentBuckets()
                assertThat(result.size).isEqualTo(3)
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }

    private fun Map<Long, List<Any>>.totalSize() = values.sumOf { it.size }
}
