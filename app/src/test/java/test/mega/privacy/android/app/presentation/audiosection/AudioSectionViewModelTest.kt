package test.mega.privacy.android.app.presentation.audiosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.presentation.audiosection.AudioSectionViewModel
import mega.privacy.android.app.presentation.audiosection.mapper.UIAudioMapper
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.audiosection.GetAllAudioUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioSectionViewModelTest {
    private lateinit var underTest: AudioSectionViewModel

    private val getAllAudioUseCase = mock<GetAllAudioUseCase>()
    private val uiAudioMapper = mock<UIAudioMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        underTest = AudioSectionViewModel(
            getAllAudioUseCase = getAllAudioUseCase,
            uiAudioMapper = uiAudioMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllAudioUseCase,
            uiAudioMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.allAudios).isEmpty()
            assertThat(initial.isPendingRefresh).isFalse()
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(initial.progressBarShowing).isEqualTo(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the audios are retrieved when the nodes are refreshed`() = runTest {
        val expectedAudio: UIAudio = mock {
            on { name }.thenReturn("audio name")
        }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

        whenever(getAllAudioUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(uiAudioMapper(any())).thenReturn(expectedAudio)

        underTest.refreshNodes()

        underTest.state.drop(1).test {
            val actual = awaitItem()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allAudios.size).isEqualTo(2)
            assertThat(actual.progressBarShowing).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the audios are retrieved when the monitorOfflineNodeUpdatesUseCase is triggered`() =
        runTest {
            val expectedAudio: UIAudio = mock {
                on { name }.thenReturn("audio name")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(getAllAudioUseCase()).thenReturn(listOf(mock(), mock()))
            whenever(uiAudioMapper(any())).thenReturn(expectedAudio)
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allAudios.size).isEqualTo(2)
                assertThat(actual.progressBarShowing).isEqualTo(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the audios are retrieved when the monitorOffline is triggered`() =
        runTest {
            val expectedAudio: UIAudio = mock {
                on { name }.thenReturn("audio name")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

            whenever(getAllAudioUseCase()).thenReturn(
                listOf(mock(), mock())
            )
            whenever(uiAudioMapper(any())).thenReturn(expectedAudio)
            whenever(monitorNodeUpdatesUseCase()).thenReturn(flowOf(NodeUpdate(emptyMap())))

            underTest.state.drop(1).test {
                val actual = awaitItem()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allAudios.size).isEqualTo(2)
                assertThat(actual.progressBarShowing).isEqualTo(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the sortOrder is updated when order is changed`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

        whenever(getAllAudioUseCase()).thenReturn(emptyList())
        underTest.refreshWhenOrderChanged()
        underTest.state.drop(1).test {
            assertThat(awaitItem().sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            cancelAndIgnoreRemainingEvents()
        }
    }
}