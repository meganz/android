package mega.privacy.android.app.presentation.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoViewModel
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodeByHandleUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.reset
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackInfoViewModelTest {
    private lateinit var underTest: TrackInfoViewModel

    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()
    private val getAudioNodeByHandleUseCase = mock<GetAudioNodeByHandleUseCase>()
    private val fileSizeStringMapper = mock<FileSizeStringMapper>()
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()
    private val removeOfflineNodeUseCase = mock<RemoveOfflineNodeUseCase>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val getOfflinePathForNodeUseCase = mock<GetOfflinePathForNodeUseCase>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getNodeLocationInfoUseCase = mock<GetNodeLocationInfo>()

    @BeforeEach
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = TrackInfoViewModel(
            getOfflinePathForNodeUseCase = getOfflinePathForNodeUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            getAudioNodeByHandleUseCase = getAudioNodeByHandleUseCase,
            fileSizeStringMapper = fileSizeStringMapper,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            removeOfflineNodeUseCase = removeOfflineNodeUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            getNodeByHandle = getNodeByHandle,
            getNodeLocationInfoUseCase = getNodeLocationInfoUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getOfflinePathForNodeUseCase,
            monitorStorageStateEventUseCase,
            getAudioNodeByHandleUseCase,
            fileSizeStringMapper,
            isAvailableOfflineUseCase,
            fileSizeStringMapper,
            removeOfflineNodeUseCase,
            durationInSecondsTextMapper,
            getNodeByHandle,
            getNodeLocationInfoUseCase
        )
    }

    @Test
    fun `test the initial state is correctly`() = runTest {
        initUnderTest()

        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual.thumbnail).isNull()
            assertThat(actual.availableOffline).isFalse()
            assertThat(actual.size).isEmpty()
            assertThat(actual.location).isNull()
            assertThat(actual.added).isEqualTo(0)
            assertThat(actual.lastModified).isEqualTo(0)
            assertThat(actual.durationString).isEmpty()
            assertThat(actual.offlineRemovedEvent).isEqualTo(consumed)
            assertThat(actual.transferTriggerEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test the state is updated correctly after loadNodeInfo is invoked`() = runTest {
        val testSize = "100 KB"
        val testDuration = "1:40"
        val testCreationTime = 100L
        val testModificationTime = 1000L
        val testAudioNode = mock<TypedAudioNode> {
            on { isAvailableOffline }.thenReturn(false)
            on { size }.thenReturn(100)
            on { creationTime }.thenReturn(testCreationTime)
            on { modificationTime }.thenReturn(testModificationTime)
            on { duration }.thenReturn(100.seconds)
        }

        whenever(getNodeLocationInfoUseCase(anyOrNull())).thenReturn(null)

        whenever(getAudioNodeByHandleUseCase(anyOrNull(), anyOrNull())).thenReturn(
            testAudioNode
        )
        whenever(fileSizeStringMapper(anyOrNull())).thenReturn(testSize)
        whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(testDuration)

        initUnderTest()
        underTest.loadTrackInfo(1L)

        underTest.state.drop(1).test {
            val actual = awaitItem()
            assertThat(actual.thumbnail).isNull()
            assertThat(actual.availableOffline).isFalse()
            assertThat(actual.size).isEqualTo(testSize)
            assertThat(actual.location).isNull()
            assertThat(actual.added).isEqualTo(testCreationTime)
            assertThat(actual.lastModified).isEqualTo(testModificationTime)
            assertThat(actual.durationString).isEqualTo(testDuration)
        }
    }

    @Test
    fun `test that the state is updated correctly after makeAvailableOffline is invoked if isAvailableOffline is true`() =
        runTest {
            val testNodeId = NodeId(1L)
            val testAudioNode = mock<TypedAudioNode> {
                on { id }.thenReturn(testNodeId)
            }
            whenever(getAudioNodeByHandleUseCase(anyOrNull(), anyOrNull())).thenReturn(
                testAudioNode
            )
            whenever(isAvailableOfflineUseCase(anyOrNull())).thenReturn(true)
            whenever(fileSizeStringMapper(anyOrNull())).thenReturn("")
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn("")

            initUnderTest()
            underTest.makeAvailableOffline(testNodeId.longValue)

            underTest.state.drop(1).test {
                assertThat(awaitItem().offlineRemovedEvent).isEqualTo(triggered)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the state is updated correctly after makeAvailableOffline is invoked if isAvailableOffline is false`() =
        runTest {
            val testNodeId = NodeId(1L)
            val testAudioNode = mock<TypedAudioNode> {
                on { id }.thenReturn(testNodeId)
            }
            whenever(getAudioNodeByHandleUseCase(anyOrNull(), anyOrNull()))
                .thenReturn(testAudioNode)
            whenever(isAvailableOfflineUseCase(anyOrNull())).thenReturn(false)
            whenever(fileSizeStringMapper(anyOrNull())).thenReturn("")
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn("")

            initUnderTest()
            underTest.makeAvailableOffline(testNodeId.longValue)

            underTest.state.drop(1).test {
                assertThat(awaitItem().transferTriggerEvent)
                    .isEqualTo(triggered(TransferTriggerEvent.StartDownloadForOffline(testAudioNode)))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test getStorageState is returned correctly`() {
        val exampleStorageStateEvent = StorageStateEvent(
            handle = 1L,
            eventString = "eventString",
            number = 0L,
            text = "text",
            type = EventType.Storage,
            storageState = StorageState.PayWall
        )
        val storageFlow: MutableStateFlow<StorageStateEvent> =
            MutableStateFlow(exampleStorageStateEvent)
        whenever(monitorStorageStateEventUseCase()).thenReturn(storageFlow)

        underTest.getStorageState()
        assertThat(underTest.getStorageState()).isEqualTo(StorageState.PayWall)
    }


    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}