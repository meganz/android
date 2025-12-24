package mega.privacy.android.app.presentation.mediaplayer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.mediaplayer.trackinfo.TrackInfoViewModel
import mega.privacy.android.app.nav.NodeDestinationMapper
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLocationUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.reset
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
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
    private val getNodeLocationInfoUseCase = mock<GetNodeLocationInfo>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getNodeLocationUseCase = mock<GetNodeLocationUseCase>()
    private val nodeDestinationMapper = mock<NodeDestinationMapper>()

    @BeforeEach
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = TrackInfoViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            getAudioNodeByHandleUseCase = getAudioNodeByHandleUseCase,
            fileSizeStringMapper = fileSizeStringMapper,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            removeOfflineNodeUseCase = removeOfflineNodeUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            getNodeLocationInfoUseCase = getNodeLocationInfoUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getNodeLocationUseCase = getNodeLocationUseCase,
            nodeDestinationMapper = nodeDestinationMapper,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
            getAudioNodeByHandleUseCase,
            fileSizeStringMapper,
            isAvailableOfflineUseCase,
            fileSizeStringMapper,
            removeOfflineNodeUseCase,
            durationInSecondsTextMapper,
            getNodeLocationInfoUseCase,
            getFeatureFlagValueUseCase,
            getNodeLocationUseCase,
            nodeDestinationMapper,
        )

        wheneverBlocking { getFeatureFlagValueUseCase(AppFeatures.SingleActivity) } doReturn false
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
    fun `test that getNodeLocationByIdUseCase is not invoked and nodeDestination is not updated if single activity flag is disabled`() =
        runTest {
            val handle = 123L
            val nodeId = NodeId(handle)
            val node = mock<TypedAudioNode> {
                on { id } doReturn nodeId
            }

            whenever(getAudioNodeByHandleUseCase(handle, false)) doReturn node
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) doReturn false

            initUnderTest()
            underTest.getNodeDestination(node)
            advanceUntilIdle()

            underTest.state.map { it.nodeDestination }.test {
                assertThat(awaitItem()).isNull()
            }
            verifyNoInteractions(getNodeLocationUseCase)

        }

    @Test
    fun `test that nodeDestination is updated if single activity flag is enabled`() = runTest {
        val handle = 100L
        val nodeId = NodeId(handle)
        val node = mock<TypedAudioNode> {
            on { id } doReturn nodeId
        }
        val nodeLocation = mock<NodeLocation> {
            on { this.node } doReturn node
            on { ancestorIds } doReturn emptyList()
            on { nodeSourceType } doReturn NodeSourceType.CLOUD_DRIVE
        }
        val expected = listOf(HomeScreensNavKey(DriveSyncNavKey(highlightedNodeHandle = handle)))

        whenever(getAudioNodeByHandleUseCase(handle, false)) doReturn node
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) doReturn true
        whenever(getNodeLocationUseCase(node)) doReturn nodeLocation
        whenever(nodeDestinationMapper(nodeLocation)) doReturn expected

        initUnderTest()
        underTest.getNodeDestination(node)
        advanceUntilIdle()

        underTest.state.map { it.nodeDestination }.test {
            val item = awaitItem()
            assertThat(item).isNotEmpty()
            val navKey = item?.first()
            assertThat(navKey).isInstanceOf(HomeScreensNavKey::class.java)
            assertThat((navKey as HomeScreensNavKey).root).isInstanceOf(DriveSyncNavKey::class.java)
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
                    .isEqualTo(
                        triggered(
                            TransferTriggerEvent.StartDownloadForOffline(
                                node = testAudioNode,
                                withStartMessage = true
                            )
                        )
                    )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test getStorageState is returned correctly`() {
        val exampleStorageStateEvent = StorageStateEvent(
            handle = 1L,
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