package mega.privacy.mobile.home.presentation.home.widget.recents

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.mobile.home.presentation.home.widget.recents.mapper.RecentActionUiItemMapper
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsTimestampText
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentsUiItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
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
        reset(getRecentActionsUseCase, recentActionUiItemMapper, setHideRecentActivityUseCase, monitorHideRecentActivityUseCase)
        whenever(monitorHideRecentActivityUseCase()).thenReturn(flowOf(false))
    }

    private fun initViewModel() {
        underTest = RecentsWidgetViewModel(
            getRecentActionsUseCase = getRecentActionsUseCase,
            recentActionUiItemMapper = recentActionUiItemMapper,
            setHideRecentActivityUseCase = setHideRecentActivityUseCase,
            monitorHideRecentActivityUseCase = monitorHideRecentActivityUseCase,
        )
    }

    @Test
    fun `test that empty list results in isLoading false and empty items`() = runTest {
        whenever(getRecentActionsUseCase(any())).thenReturn(emptyList())

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.recentActionItems).isEmpty()
        }
    }

    @Test
    fun `test that recent actions are loaded and mapped correctly`() = runTest {
        whenever(getRecentActionsUseCase(excludeSensitives = false)).thenReturn(
            listOf(bucket1, bucket2, bucket3)
        )
        whenever(recentActionUiItemMapper(bucket1)).thenReturn(uiItem1)
        whenever(recentActionUiItemMapper(bucket2)).thenReturn(uiItem2)
        whenever(recentActionUiItemMapper(bucket3)).thenReturn(uiItem3)

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.recentActionItems).hasSize(3)
        }
    }

    @Test
    fun `test that isLoading is set to false when loading fails`() = runTest {
        whenever(getRecentActionsUseCase(excludeSensitives = false)).thenThrow(
            RuntimeException("Test error")
        )

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
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