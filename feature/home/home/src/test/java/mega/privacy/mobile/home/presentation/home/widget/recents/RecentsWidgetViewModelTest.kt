package mega.privacy.mobile.home.presentation.home.widget.recents

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.milliseconds
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.mobile.home.presentation.home.widget.recents.mapper.RecentActionUiItemMapper
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsTimestampText
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsUiItem
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsWidgetUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsWidgetViewModelTest {

    private lateinit var underTest: RecentsWidgetViewModel

    private val getRecentActionsUseCase = mock<GetRecentActionsUseCase>()
    private val recentActionUiItemMapper = mock<RecentActionUiItemMapper>()
    private val setHideRecentActivityUseCase = mock<SetHideRecentActivityUseCase>()
    private val monitorHideRecentActivityUseCase = mock<MonitorHideRecentActivityUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()

    private val bucket1 = createMockRecentActionBucket(
        timestamp = 1000L,
        identifier = "bucket1"
    )
    private val bucket2 = createMockRecentActionBucket(
        timestamp = 2000L,
        identifier = "bucket2"
    )
    private val bucket3 = createMockRecentActionBucket(
        timestamp = 1500L,
        identifier = "bucket3"
    )

    private val uiItem1 = createMockRecentsUiItem(
        timestamp = 1000L,
        bucket = bucket1
    )
    private val uiItem2 = createMockRecentsUiItem(
        timestamp = 2000L,
        bucket = bucket2
    )
    private val uiItem3 = createMockRecentsUiItem(
        timestamp = 1500L,
        bucket = bucket3
    )

    @BeforeEach
    fun setUp() {
        reset(
            getRecentActionsUseCase,
            recentActionUiItemMapper,
            setHideRecentActivityUseCase,
            monitorHideRecentActivityUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase
        )
        whenever(monitorHideRecentActivityUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(monitorNodeUpdatesUseCase()).thenReturn(emptyFlow())
    }

    private fun initViewModel() {
        underTest = RecentsWidgetViewModel(
            getRecentActionsUseCase = getRecentActionsUseCase,
            recentActionUiItemMapper = recentActionUiItemMapper,
            setHideRecentActivityUseCase = setHideRecentActivityUseCase,
            monitorHideRecentActivityUseCase = monitorHideRecentActivityUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
        )
    }

    @Test
    fun `test that empty list results in isLoading false and empty items`() = runTest {
        whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isNodesLoading).isFalse()
            assertThat(state.recentActionItems).isEmpty()
        }
    }

    @Test
    fun `test that recent actions are loaded and mapped correctly`() = runTest {
        whenever(getRecentActionsUseCase(excludeSensitives = false, maxBucketCount = 4)).thenReturn(
            listOf(bucket1, bucket2, bucket3)
        )
        whenever(recentActionUiItemMapper(bucket1)).thenReturn(uiItem1)
        whenever(recentActionUiItemMapper(bucket2)).thenReturn(uiItem2)
        whenever(recentActionUiItemMapper(bucket3)).thenReturn(uiItem3)

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isNodesLoading).isFalse()
            assertThat(state.recentActionItems).hasSize(3)
        }
    }

    @Test
    fun `test that isLoading remains true when loading fails`() = runTest {
        whenever(getRecentActionsUseCase(any(), any())).thenThrow(
            RuntimeException("Test error")
        )

        initViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            // When loading fails, isNodesLoading remains true (not updated to false)
            // but isHiddenNodeSettingsLoading should be false after first emission
            assertThat(state.isNodesLoading).isTrue()
            assertThat(state.isHiddenNodeSettingsLoading).isFalse()
            assertThat(state.recentActionItems).isEmpty()
        }
    }

    @Test
    fun `test that isHideRecentsEnabled is updated when monitor emits true`() = runTest {
        whenever(monitorHideRecentActivityUseCase()).thenReturn(flowOf(true))

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isHideRecentsEnabled).isTrue()
        }
    }

    @Test
    fun `test that isHideRecentsEnabled is updated when monitor emits false`() = runTest {
        whenever(monitorHideRecentActivityUseCase()).thenReturn(flowOf(false))

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isHideRecentsEnabled).isFalse()
        }
    }

    @Test
    fun `test that hideRecentActivity calls setHideRecentActivityUseCase with true`() = runTest {
        whenever(setHideRecentActivityUseCase(any())).thenReturn(Unit)

        initViewModel()

        underTest.hideRecentActivity()

        verify(setHideRecentActivityUseCase).invoke(true)
    }

    @Test
    fun `test that showRecentActivity calls setHideRecentActivityUseCase with false`() = runTest {
        whenever(setHideRecentActivityUseCase(any())).thenReturn(Unit)

        initViewModel()

        underTest.showRecentActivity()

        verify(setHideRecentActivityUseCase).invoke(false)
    }

    @Test
    fun `test that excludeSensitives is false when hidden nodes disabled`() = runTest {
        whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

        initViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.excludeSensitives).isFalse()
            assertThat(state.isHiddenNodesEnabled).isFalse()
            assertThat(state.showHiddenNodes).isFalse()
        }
    }

    @Test
    fun `test that excludeSensitives is true when hidden nodes enabled and showHiddenNodes is false`() =
        runTest {
            whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

            initViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.excludeSensitives).isTrue()
                assertThat(state.isHiddenNodesEnabled).isTrue()
                assertThat(state.showHiddenNodes).isFalse()
            }
        }

    @Test
    fun `test that excludeSensitives is false when hidden nodes enabled and showHiddenNodes is true`() =
        runTest {
            whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))

            initViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.excludeSensitives).isFalse()
                assertThat(state.isHiddenNodesEnabled).isTrue()
                assertThat(state.showHiddenNodes).isTrue()
            }
        }

    @Test
    fun `test that loadRecents is called with correct excludeSensitives value`() = runTest {
        whenever(getRecentActionsUseCase(excludeSensitives = true, maxBucketCount = 4)).thenReturn(
            emptyList()
        )
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

        initViewModel()
        advanceUntilIdle()

        verify(getRecentActionsUseCase).invoke(excludeSensitives = true, maxBucketCount = 4)
    }

    @Test
    fun `test that recents are reloaded when excludeSensitives changes from false to true`() =
        runTest {
            whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(
                flowOf(false, true) // First false, then true
            )
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

            initViewModel()
            advanceUntilIdle()

            // Should be called twice: once on init, once when excludeSensitives changes
            val inOrder = inOrder(getRecentActionsUseCase)
            inOrder.verify(getRecentActionsUseCase)
                .invoke(excludeSensitives = false, maxBucketCount = 4)
            inOrder.verify(getRecentActionsUseCase)
                .invoke(excludeSensitives = true, maxBucketCount = 4)
        }

    @Test
    fun `test that recents are reloaded when showHiddenNodes changes from false to true`() =
        runTest {
            whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(
                flowOf(false, true) // First false, then true
            )

            initViewModel()
            advanceUntilIdle()

            // Should be called twice: once on init with excludeSensitives=true, once when it changes to false
            val inOrder = inOrder(getRecentActionsUseCase)
            inOrder.verify(getRecentActionsUseCase)
                .invoke(excludeSensitives = true, maxBucketCount = 4)
            inOrder.verify(getRecentActionsUseCase)
                .invoke(excludeSensitives = false, maxBucketCount = 4)
        }

    @Test
    fun `test that isHiddenNodeSettingsLoading is set to false after first emission`() = runTest {
        whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))

        initViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isHiddenNodeSettingsLoading).isFalse()
        }
    }

    @Test
    fun `test that isLoading is true when isNodesLoading is true`() = runTest {
        val state = RecentsWidgetUiState(
            isNodesLoading = true,
            isHiddenNodeSettingsLoading = false
        )
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `test that isLoading is true when isHiddenNodeSettingsLoading is true`() = runTest {
        val state = RecentsWidgetUiState(
            isNodesLoading = false,
            isHiddenNodeSettingsLoading = true
        )
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `test that isLoading is true when both loading flags are true`() = runTest {
        val state = RecentsWidgetUiState(
            isNodesLoading = true,
            isHiddenNodeSettingsLoading = true
        )
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `test that isLoading is false when both loading flags are false`() = runTest {
        val state = RecentsWidgetUiState(
            isNodesLoading = false,
            isHiddenNodeSettingsLoading = false
        )
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `test that monitorNodeUpdatesUseCase starts collecting only after isLoading becomes false`() =
        runTest {
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)
            whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())

            initViewModel()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()

            nodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
            advanceUntilIdle()
            advanceTimeBy(600.milliseconds)

            verify(getRecentActionsUseCase, times(2))
                .invoke(excludeSensitives = false, maxBucketCount = 4)
        }

    @Test
    fun `test that loadRecents is called when node updates are emitted after loading completes`() =
        runTest {
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)
            whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())

            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()
            advanceTimeBy(100.milliseconds)

            nodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
            advanceUntilIdle()
            advanceTimeBy(600.milliseconds)

            verify(getRecentActionsUseCase, times(2))
                .invoke(excludeSensitives = false, maxBucketCount = 4)
        }

    @Test
    fun `test that monitorNodeUpdatesUseCase does not trigger loadRecents when isLoading is true`() =
        runTest {
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)
            whenever(getRecentActionsUseCase(any(), any())).thenThrow(
                RuntimeException("Test error")
            )

            initViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isTrue()
                cancelAndIgnoreRemainingEvents()
            }

            nodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
            advanceUntilIdle()
            advanceTimeBy(600.milliseconds)

            verify(getRecentActionsUseCase, times(1))
                .invoke(excludeSensitives = false, maxBucketCount = 4)
        }

    @Test
    fun `test that multiple node updates trigger loadRecents multiple times after loading completes`() =
        runTest {
            val nodeUpdatesFlow = MutableSharedFlow<NodeUpdate>()
            whenever(monitorNodeUpdatesUseCase()).thenReturn(nodeUpdatesFlow)
            whenever(getRecentActionsUseCase(any(), any())).thenReturn(emptyList())

            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }

            advanceUntilIdle()

            nodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
            advanceUntilIdle()
            advanceTimeBy(600.milliseconds)

            nodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
            advanceUntilIdle()
            advanceTimeBy(600.milliseconds)

            nodeUpdatesFlow.emit(NodeUpdate(emptyMap()))
            advanceUntilIdle()
            advanceTimeBy(600.milliseconds)

            verify(getRecentActionsUseCase, times(4))
                .invoke(excludeSensitives = false, maxBucketCount = 4)
        }

    private fun createMockRecentActionBucket(
        timestamp: Long = 1234567890L,
        identifier: String = "test_bucket",
        nodes: List<TypedFileNode> = listOf(createMockFileNode()),
    ): RecentActionBucket = mock {
        on { it.timestamp }.thenReturn(timestamp)
        on { it.nodes }.thenReturn(nodes)
        on { it.identifier }.thenReturn(identifier)
    }

    private fun createMockFileNode(
        name: String = "testFile.txt",
    ): TypedFileNode = mock {
        on { it.name }.thenReturn(name)
        val fileTypeInfo = TextFileTypeInfo("text/plain", "txt")
        on { it.type }.thenReturn(fileTypeInfo)
    }

    private fun createMockRecentsUiItem(
        timestamp: Long = 1234567890L,
        bucket: RecentActionBucket = createMockRecentActionBucket(),
    ): RecentsUiItem = mock {
        on { it.timestampText }.thenReturn(RecentsTimestampText(timestamp))
        on { it.bucket }.thenReturn(bucket)
    }
}