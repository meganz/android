package mega.privacy.android.app.presentation.recentactions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.recentactions.RecentActionsComposeViewModel
import mega.privacy.android.app.presentation.recentactions.mapper.RecentActionBucketUiEntityMapper
import mega.privacy.android.app.presentation.recentactions.model.RecentActionBucketUiEntity
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
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
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val recentActionBucketUiEntityMapper = mock<RecentActionBucketUiEntityMapper>()

    private val megaRecentActionBucket = mock<RecentActionBucket>()
    private val megaRecentActionBucket2 = mock<RecentActionBucket>()
    private val megaRecentActionBucket3 = mock<RecentActionBucket>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase> {
        onBlocking { invoke(any()) }.thenReturn(false)
    }
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase> {
        on { invoke() }.thenReturn(flowOf(false))
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getRecentActionsUseCase,
            setHideRecentActivityUseCase,
            monitorHideRecentActivityUseCase,
            monitorNodeUpdatesUseCase,
            recentActionBucketUiEntityMapper,
            monitorConnectivityUseCase
        )
        runBlocking {
            stubCommon()
        }
        underTest = RecentActionsComposeViewModel(
            getRecentActionsUseCase = getRecentActionsUseCase,
            setHideRecentActivityUseCase = setHideRecentActivityUseCase,
            monitorHideRecentActivityUseCase = monitorHideRecentActivityUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            recentActionBucketUiEntityMapper = recentActionBucketUiEntityMapper,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorAccountDetailUseCase = mock(),
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
        )
    }

    private suspend fun stubCommon() {
        whenever(getRecentActionsUseCase()).thenReturn(listOf(megaRecentActionBucket2))
        whenever(monitorHideRecentActivityUseCase()).thenReturn(flow {
            emit(true)
            emit(false)
        })
        whenever(monitorNodeUpdatesUseCase()).thenReturn(monitorNodeUpdatesFakeFlow)
        whenever(monitorConnectivityUseCase()).thenReturn(emptyFlow())

        val recentActionBucketUiEntity1 = mock<RecentActionBucketUiEntity> {
            on { this.date }.thenReturn("Today")
            on { this.bucket }.thenReturn(megaRecentActionBucket)
        }
        val recentActionBucketUiEntity2 = mock<RecentActionBucketUiEntity> {
            on { this.date }.thenReturn("Today")
            on { this.bucket }.thenReturn(megaRecentActionBucket2)
        }
        val recentActionBucketUiEntity3 = mock<RecentActionBucketUiEntity> {
            on { this.date }.thenReturn("14 March 2024")
            on { this.bucket }.thenReturn(megaRecentActionBucket3)
        }
        whenever(recentActionBucketUiEntityMapper(megaRecentActionBucket)).thenReturn(
            recentActionBucketUiEntity1
        )
        whenever(recentActionBucketUiEntityMapper(megaRecentActionBucket2)).thenReturn(
            recentActionBucketUiEntity2
        )
        whenever(recentActionBucketUiEntityMapper(megaRecentActionBucket3)).thenReturn(
            recentActionBucketUiEntity3
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.hideRecentActivity).isFalse()
            assertThat(initial.isConnected).isFalse()
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
                    assertThat(result.keys.first()).isEqualTo("Today")
                    assertThat(result.keys.last()).isEqualTo("14 March 2024")
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

    private fun Map<String, List<Any>>.totalSize() = values.sumOf { it.size }
}
