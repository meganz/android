package test.mega.privacy.android.app.presentation.audiosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.audiosection.AudioSectionViewModel
import mega.privacy.android.app.presentation.audiosection.mapper.AudioUiEntityMapper
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.audiosection.GetAllAudioUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import test.mega.privacy.android.app.TimberJUnit5Extension

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TimberJUnit5Extension::class)
class AudioSectionViewModelTest {
    private lateinit var underTest: AudioSectionViewModel

    private val getAllAudioUseCase = mock<GetAllAudioUseCase>()
    private val audioUIEntityMapper = mock<AudioUiEntityMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val setViewType = mock<SetViewType>()
    private val fakeMonitorViewTypeFlow = MutableSharedFlow<ViewType>()
    private val monitorViewType = mock<MonitorViewType>()
    private val updateNodeSensitiveUseCase = mock<UpdateNodeSensitiveUseCase>()
    private val getNodeContentUriUseCase = mock<GetNodeContentUriUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        on {
            runBlocking { invoke() }
        }.thenReturn(false)
    }
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase> {
        on {
            runBlocking { invoke() }
        }.thenReturn(flowOf(false))
    }
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    private val expectedId = NodeId(1)
    private val expectedAudio: AudioUiEntity = mock {
        on { id }.thenReturn(expectedId)
        on { name }.thenReturn("audio name")
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorViewType() }.thenReturn(fakeMonitorViewTypeFlow)
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(false)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = AudioSectionViewModel(
            getAllAudioUseCase = getAllAudioUseCase,
            audioUIEntityMapper = audioUIEntityMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            getNodeByHandle = getNodeByHandle,
            getNodeByIdUseCase = getNodeByIdUseCase,
            setViewType = setViewType,
            monitorViewType = monitorViewType,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            defaultDispatcher = UnconfinedTestDispatcher(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getNodeContentUriUseCase = getNodeContentUriUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllAudioUseCase,
            audioUIEntityMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            getNodeByHandle,
            getNodeByIdUseCase,
            setViewType,
            monitorViewType,
            getNodeContentUriUseCase
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.allAudios).isEmpty()
            assertThat(initial.isPendingRefresh).isFalse()
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(initial.progressBarShowing).isTrue()
            assertThat(initial.searchMode).isFalse()
            assertThat(initial.scrollToTop).isFalse()
            assertThat(initial.selectedAudioHandles).isEmpty()
            assertThat(initial.isInSelection).isFalse()
            assertThat(initial.accountDetail).isNull()
            assertThat(initial.isHiddenNodesOnboarded).isFalse()
            assertThat(initial.clickedItem).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the audios are retrieved when the nodes are refreshed`() = runTest {
        initAudiosReturned()

        underTest.refreshNodes()

        underTest.state.drop(1).test {
            val actual = awaitItem()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allAudios.size).isEqualTo(2)
            assertThat(actual.progressBarShowing).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun initAudiosReturned() {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllAudioUseCase()).thenReturn(listOf(mock(), mock()))
        whenever(audioUIEntityMapper(any())).thenReturn(expectedAudio)
    }

    @Test
    fun `test that isPendingRefresh is correctly updated when monitorOfflineNodeUpdatesUseCase is triggered`() =
        runTest {
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(flowOf(emptyList()))

            underTest.state.test {
                underTest.markHandledPendingRefresh()
                assertThat(awaitItem().isPendingRefresh).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isPendingRefresh is correctly updated when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            whenever(monitorNodeUpdatesUseCase()).thenReturn(flowOf(NodeUpdate(emptyMap())))

            underTest.state.test {
                underTest.markHandledPendingRefresh()
                assertThat(awaitItem().isPendingRefresh).isFalse()
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

    @Test
    fun `test that the result returned correctly when search query is not empty`() = runTest {
        val expectedTypedAudioNode = mock<TypedAudioNode> { on { name }.thenReturn("audio name") }
        val audioNode = mock<TypedAudioNode> { on { name }.thenReturn("name") }
        val expectedAudio = mock<AudioUiEntity> { on { name }.thenReturn("audio name") }
        val audio = mock<AudioUiEntity> { on { name }.thenReturn("name") }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllAudioUseCase()).thenReturn(listOf(expectedTypedAudioNode, audioNode))
        whenever(audioUIEntityMapper(audioNode)).thenReturn(audio)
        whenever(audioUIEntityMapper(expectedTypedAudioNode)).thenReturn(expectedAudio)

        underTest.refreshNodes()

        underTest.state.drop(1).test {
            assertThat(awaitItem().allAudios.size).isEqualTo(2)

            underTest.searchQuery("audio")
            assertThat(awaitItem().allAudios.size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the searchMode is correctly updated`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
        whenever(getAllAudioUseCase()).thenReturn(emptyList())

        underTest.state.drop(1).test {
            underTest.searchReady()
            assertThat(awaitItem().searchMode).isTrue()

            underTest.exitSearch()
            assertThat(awaitItem().searchMode).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the currentViewType is correctly updated when monitorViewType is triggered`() =
        runTest {
            underTest.state.drop(1).test {
                fakeMonitorViewTypeFlow.emit(ViewType.GRID)
                assertThat(awaitItem().currentViewType).isEqualTo(ViewType.GRID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the selected item is updated by 1 when long clicked`() =
        runTest {
            initAudiosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allAudios.size).isEqualTo(2)

                underTest.onItemLongClicked(expectedAudio, 0)
                assertThat(awaitItem().selectedAudioHandles.size).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the checked index is incremented by 1 when the selected item gets clicked`() =
        runTest {
            initAudiosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allAudios.size).isEqualTo(2)

                underTest.onItemLongClicked(expectedAudio, 0)
                assertThat(awaitItem().selectedAudioHandles.size).isEqualTo(1)

                underTest.onItemClicked(expectedAudio, 1)
                assertThat(awaitItem().selectedAudioHandles.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that after selected all audios, size of audio items equals to size of selected audios`() =
        runTest {
            initAudiosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allAudios.size).isEqualTo(2)

                underTest.selectAllNodes()
                awaitItem().let { state ->
                    assertThat(state.selectedAudioHandles.size).isEqualTo(state.allAudios.size)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that isInSelection is correctly updated when selecting and clearing all nodes`() =
        runTest {
            initAudiosReturned()

            underTest.state.drop(1).test {
                underTest.refreshNodes()

                underTest.selectAllNodes()
                assertThat(awaitItem().isInSelection).isTrue()

                underTest.clearAllSelectedAudios()
                assertThat(awaitItem().isInSelection).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that getTypedAudioNodeById function returns the correct node`() = runTest {
        val typedNodes = (0..3).map { handle ->
            mock<TypedAudioNode> { on { id }.thenReturn(NodeId(handle.toLong())) }
        }
        initAudiosReturned()
        whenever(getAllAudioUseCase()).thenReturn(typedNodes)

        underTest.refreshNodes()
        delay(100)
        val result = underTest.getTypedAudioNodeById(typedNodes[1].id)
        assertThat(result).isEqualTo(typedNodes[1])
    }

    @Test
    fun `test that getNodeContentUri function returns the correct uri`() = runTest {
        val url = "url"
        val uri = NodeContentUri.RemoteContentUri(url, true)
        initAudiosReturned()
        whenever(getNodeContentUriUseCase(anyOrNull())).thenReturn(uri)

        val result = underTest.getNodeContentUri(mock())
        assertThat(result).isEqualTo(uri)
    }

    @Test
    fun `test that getNodeContentUri function returns null when the exception is thrown`() =
        runTest {
            initAudiosReturned()
            whenever(getNodeContentUriUseCase(anyOrNull())).thenThrow(NullPointerException())

            val result = underTest.getNodeContentUri(mock())
            assertThat(result).isNull()
        }

    @Test
    fun `test that clickedItem is updated correctly`() = runTest {
        val expectedAudioNode = mock<TypedAudioNode>()
        initUnderTest()

        underTest.state.test {
            assertThat(awaitItem().clickedItem).isNull()
            underTest.updateClickedItem(expectedAudioNode)
            assertThat(awaitItem().clickedItem).isEqualTo(expectedAudioNode)
            underTest.updateClickedItem(null)
            assertThat(awaitItem().clickedItem).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that clickedItem is updated correctly when onItemClicked is invoked`() =
        runTest {
            val expectedAudioNode = mock<TypedAudioNode>() {
                on { id }.thenReturn(expectedId)
            }
            initAudiosReturned()
            whenever(getAllAudioUseCase()).thenReturn(listOf(mock(), expectedAudioNode))

            underTest.state.drop(1).test {
                underTest.refreshNodes()
                assertThat(awaitItem().allAudios.size).isEqualTo(2)

                underTest.onItemClicked(expectedAudio, 1)
                assertThat(awaitItem().clickedItem).isEqualTo(expectedAudioNode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
