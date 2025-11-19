package mega.privacy.android.app.presentation.audiosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TimberJUnit5Extension
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.audiosection.mapper.AudioUiEntityMapper
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.AudioHiddenNodeActionModifierItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.audiosection.GetAllAudioUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.rubbishbin.GetRubbishBinFolderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration

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
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private var accountDetailFlow = MutableSharedFlow<AccountDetail>()
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()

    private val firstDefaultId = NodeId(123L)
    private val firstDefaultAudioFileNode = mock<FileNode> {
        on { id } doReturn firstDefaultId
        on { isIncomingShare } doReturn false
    }
    private val firstDefaultTypedAudioNode = TypedAudioNode(
        fileNode = firstDefaultAudioFileNode,
        duration = Duration.INFINITE
    )
    private val expectedFirstAudioUiEntity = newAudioUiEntity(
        id = firstDefaultId
    )
    private val secondDefaultId = NodeId(312L)
    private val secondDefaultAudioFileNode = mock<FileNode> {
        on { id } doReturn secondDefaultId
        on { isIncomingShare } doReturn false
    }
    private val secondDefaultTypedAudioNode = TypedAudioNode(
        fileNode = secondDefaultAudioFileNode,
        duration = Duration.INFINITE
    )
    private val expectedSecondAudioUiEntity = newAudioUiEntity(
        id = secondDefaultId
    )
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val checkNodeCanBeMovedToTargetNode = mock<CheckNodeCanBeMovedToTargetNode>()
    private val getRubbishBinFolderUseCase = mock<GetRubbishBinFolderUseCase>()
    private val getNodeAccessUseCase = mock<GetNodeAccessUseCase>()
    private var showHiddenItemsFlow = MutableSharedFlow<Boolean>()

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorViewType() }.thenReturn(fakeMonitorViewTypeFlow)
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(accountDetailFlow)
        wheneverBlocking { monitorShowHiddenItemsUseCase() }.thenReturn(showHiddenItemsFlow)
        wheneverBlocking { isHiddenNodesOnboardedUseCase() }.thenReturn(false)
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
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            checkNodeCanBeMovedToTargetNode = checkNodeCanBeMovedToTargetNode,
            getRubbishBinFolderUseCase = getRubbishBinFolderUseCase,
            getNodeAccessUseCase = getNodeAccessUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        accountDetailFlow = MutableSharedFlow()
        showHiddenItemsFlow = MutableSharedFlow()
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
            updateNodeSensitiveUseCase,
            getNodeContentUriUseCase,
            monitorAccountDetailUseCase,
            checkNodeCanBeMovedToTargetNode,
            getRubbishBinFolderUseCase,
            getNodeAccessUseCase,
            isHiddenNodesOnboardedUseCase,
            monitorShowHiddenItemsUseCase,
            getBusinessStatusUseCase
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
            assertThat(initial.scrollToTop).isFalse()
            assertThat(initial.selectedNodes).isEmpty()
            assertThat(initial.isInSelection).isFalse()
            assertThat(initial.accountType).isNull()
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
        whenever(getRubbishBinFolderUseCase()) doReturn null
        whenever(getAllAudioUseCase()).thenReturn(
            listOf(
                firstDefaultTypedAudioNode,
                secondDefaultTypedAudioNode
            )
        )
        whenever(
            audioUIEntityMapper(
                eq(firstDefaultTypedAudioNode),
                any(),
                any()
            )
        ).thenReturn(expectedFirstAudioUiEntity)
        whenever(
            audioUIEntityMapper(
                eq(secondDefaultTypedAudioNode),
                any(),
                any()
            )
        ).thenReturn(expectedSecondAudioUiEntity)
        whenever(
            getNodeAccessUseCase(nodeId = firstDefaultId)
        ) doReturn AccessPermission.UNKNOWN
        whenever(
            getNodeAccessUseCase(nodeId = secondDefaultId)
        ) doReturn AccessPermission.UNKNOWN
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

                underTest.onItemLongClicked(expectedFirstAudioUiEntity, 0)
                assertThat(awaitItem().selectedNodes.size).isEqualTo(1)
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

                underTest.onItemLongClicked(expectedFirstAudioUiEntity, 0)
                assertThat(awaitItem().selectedNodes.size).isEqualTo(1)

                underTest.onItemClicked(expectedFirstAudioUiEntity, 1)
                assertThat(awaitItem().selectedNodes.size).isEqualTo(2)
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
                    assertThat(state.selectedNodes.size).isEqualTo(state.allAudios.size)
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
        whenever(
            getNodeAccessUseCase(nodeId = typedNodes[0].id)
        ) doReturn AccessPermission.UNKNOWN
        whenever(
            getNodeAccessUseCase(nodeId = typedNodes[1].id)
        ) doReturn AccessPermission.UNKNOWN
        whenever(
            getNodeAccessUseCase(nodeId = typedNodes[2].id)
        ) doReturn AccessPermission.UNKNOWN
        whenever(
            getNodeAccessUseCase(nodeId = typedNodes[3].id)
        ) doReturn AccessPermission.UNKNOWN

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
            initAudiosReturned()

            underTest.refreshNodes()
            advanceUntilIdle()
            underTest.onItemClicked(expectedFirstAudioUiEntity, 1)

            underTest.state.test {
                assertThat(expectMostRecentItem().clickedItem).isEqualTo(firstDefaultTypedAudioNode)
            }
        }

    @ParameterizedTest
    @EnumSource(AccessPermission::class)
    fun `test that the correct access permission for an account is set`(
        accessPermission: AccessPermission,
    ) = runTest {
        whenever(getCloudSortOrder()) doReturn SortOrder.ORDER_MODIFICATION_DESC
        whenever(getAllAudioUseCase()) doReturn listOf(firstDefaultTypedAudioNode)
        whenever(
            audioUIEntityMapper(
                any(),
                any(),
                any()
            )
        ) doReturn expectedFirstAudioUiEntity.copy(accessPermission = accessPermission)
        whenever(getRubbishBinFolderUseCase()) doReturn null
        whenever(
            getNodeAccessUseCase(nodeId = firstDefaultId)
        ) doReturn accessPermission

        underTest.refreshNodes()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(
                expectMostRecentItem().allAudios.first().accessPermission
            ).isEqualTo(accessPermission)
        }
    }

    private fun provideNodeAccessPermissions() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
        Arguments.of(true, false),
        Arguments.of(false, true),
    )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct value is set indicating whether the node can be moved to the rubbish bin`(
        canBeMovedToRubbishBin: Boolean,
    ) = runTest {
        whenever(getCloudSortOrder()) doReturn SortOrder.ORDER_MODIFICATION_DESC
        whenever(getAllAudioUseCase()) doReturn listOf(firstDefaultTypedAudioNode)
        val accessPermission = AccessPermission.UNKNOWN
        whenever(getNodeAccessUseCase(nodeId = firstDefaultId)) doReturn accessPermission
        whenever(
            audioUIEntityMapper(
                firstDefaultTypedAudioNode,
                accessPermission,
                canBeMovedToRubbishBin
            )
        ) doReturn expectedFirstAudioUiEntity.copy(canBeMovedToRubbishBin = canBeMovedToRubbishBin)
        val rubbishBinNodeId = NodeId(4321L)
        val rubbishBinNode = if (canBeMovedToRubbishBin) {
            mock<FileNode> {
                on { id } doReturn rubbishBinNodeId
            }
        } else null
        whenever(getRubbishBinFolderUseCase()) doReturn rubbishBinNode
        whenever(
            checkNodeCanBeMovedToTargetNode(
                nodeId = firstDefaultId,
                targetNodeId = rubbishBinNodeId
            )
        ) doReturn canBeMovedToRubbishBin

        underTest.refreshNodes()
        advanceUntilIdle()

        underTest.state.test {
            assertThat(
                expectMostRecentItem().allAudios.first().canBeMovedToRubbishBin
            ).isEqualTo(canBeMovedToRubbishBin)
        }
    }

    @Test
    fun `test that the hidden node toolbar item can be hidden when a selection is made for free account`() =
        runTest {
            val accountDetail = AccountDetail(
                levelDetail = AccountLevelDetail(
                    accountType = AccountType.FREE,
                    subscriptionStatus = null,
                    subscriptionRenewTime = 0L,
                    accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                    proExpirationTime = 0L,
                    accountPlanDetail = null,
                    accountSubscriptionDetailList = listOf(),
                )
            )
            accountDetailFlow.emit(accountDetail)
            initAudiosReturned()
            whenever(getRubbishBinFolderUseCase()) doReturn null

            underTest.refreshNodes()
            advanceUntilIdle()
            underTest.onItemLongClicked(expectedFirstAudioUiEntity, 0)
            advanceUntilIdle()

            underTest.state.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    AudioHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = true
                    )
                )
            }
        }

    @Test
    fun `test that the hidden node toolbar item can be hidden when a selection is made for an expired business account`() =
        runTest {
            val accountDetail = AccountDetail(
                levelDetail = AccountLevelDetail(
                    accountType = AccountType.BUSINESS,
                    subscriptionStatus = null,
                    subscriptionRenewTime = 0L,
                    accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                    proExpirationTime = 0L,
                    accountPlanDetail = null,
                    accountSubscriptionDetailList = listOf(),
                )
            )
            whenever(getBusinessStatusUseCase()) doReturn BusinessAccountStatus.Expired
            accountDetailFlow.emit(accountDetail)
            initAudiosReturned()
            whenever(getRubbishBinFolderUseCase()) doReturn null

            underTest.refreshNodes()
            underTest.onItemLongClicked(expectedSecondAudioUiEntity, 1)
            advanceUntilIdle()
            underTest.onItemClicked(expectedFirstAudioUiEntity, 0)

            underTest.state.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    AudioHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = true
                    )
                )
            }
        }

    @Test
    fun `test that the hidden node toolbar item can be hidden when a selection is made for a not sensitive node`() =
        runTest {
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(getRubbishBinFolderUseCase()) doReturn null
            val nodeId = NodeId(123L)
            val audioFileNode = mock<FileNode> {
                on { id } doReturn nodeId
                on { isSensitiveInherited } doReturn false
                on { isMarkedSensitive } doReturn false
            }
            val audioNode = TypedAudioNode(
                fileNode = audioFileNode,
                duration = Duration.INFINITE
            )
            whenever(getAllAudioUseCase()) doReturn listOf(audioNode)
            val uiEntity = newAudioUiEntity()
            whenever(
                audioUIEntityMapper(
                    eq(audioNode),
                    any(),
                    any()
                )
            ) doReturn uiEntity
            whenever(
                getNodeAccessUseCase(nodeId = nodeId)
            ) doReturn AccessPermission.UNKNOWN

            underTest.refreshNodes()
            underTest.onItemLongClicked(uiEntity, 0)
            advanceUntilIdle()

            underTest.state.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    AudioHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = true
                    )
                )
            }
        }

    @Test
    fun `test that the hidden node toolbar item cannot be hidden when a selection is made with non-hidden criteria`() =
        runTest {
            val accountDetail = AccountDetail(
                levelDetail = AccountLevelDetail(
                    accountType = AccountType.BUSINESS,
                    subscriptionStatus = null,
                    subscriptionRenewTime = 0L,
                    accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
                    proExpirationTime = 0L,
                    accountPlanDetail = null,
                    accountSubscriptionDetailList = listOf(),
                )
            )
            whenever(monitorNodeUpdatesUseCase()) doReturn flowOf(NodeUpdate(emptyMap()))
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
            whenever(getBusinessStatusUseCase()) doReturn BusinessAccountStatus.Active
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
            whenever(isHiddenNodesOnboardedUseCase()) doReturn false
            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(getRubbishBinFolderUseCase()) doReturn null
            val nodeId = NodeId(123L)
            val audioFileNode = mock<FileNode> {
                on { id } doReturn nodeId
                on { isSensitiveInherited } doReturn false
                on { isMarkedSensitive } doReturn true
            }
            val audioNode = TypedAudioNode(
                fileNode = audioFileNode,
                duration = Duration.INFINITE
            )
            whenever(getAllAudioUseCase()) doReturn listOf(audioNode)
            val uiEntity = newAudioUiEntity(
                isMarkedSensitive = true,
                isSensitiveInherited = false
            )
            whenever(
                audioUIEntityMapper(
                    eq(audioNode),
                    any(),
                    any()
                )
            ) doReturn uiEntity
            whenever(
                getNodeAccessUseCase(nodeId = nodeId)
            ) doReturn AccessPermission.UNKNOWN

            initUnderTest()
            advanceUntilIdle()
            underTest.refreshNodes()
            advanceUntilIdle()
            underTest.onItemLongClicked(uiEntity, 0)
            advanceUntilIdle()

            underTest.state.test {
                assertThat(
                    expectMostRecentItem().toolbarActionsModifierItem!!.item.hiddenNodeItem
                ).isEqualTo(
                    AudioHiddenNodeActionModifierItem(
                        isEnabled = true,
                        canBeHidden = false
                    )
                )
            }
        }

    private fun newAudioUiEntity(
        id: NodeId = NodeId(1),
        name: String = "",
        size: Long = 0L,
        duration: String? = null,
        thumbnail: File? = null,
        fileTypeInfo: FileTypeInfo = UnknownFileTypeInfo(mimeType = "", extension = ""),
        isFavourite: Boolean = false,
        isExported: Boolean = false,
        isTakenDown: Boolean = false,
        hasVersions: Boolean = false,
        modificationTime: Long = 0L,
        label: Int = 0,
        nodeAvailableOffline: Boolean = false,
        isSelected: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        isIncomingShare: Boolean = false,
        accessPermission: AccessPermission = AccessPermission.UNKNOWN,
        canBeMovedToRubbishBin: Boolean = false,
    ) = AudioUiEntity(
        id = id,
        name = name,
        size = size,
        duration = duration,
        thumbnail = thumbnail,
        fileTypeInfo = fileTypeInfo,
        isFavourite = isFavourite,
        isExported = isExported,
        isTakenDown = isTakenDown,
        hasVersions = hasVersions,
        modificationTime = modificationTime,
        label = label,
        nodeAvailableOffline = nodeAvailableOffline,
        isSelected = isSelected,
        isMarkedSensitive = isMarkedSensitive,
        isSensitiveInherited = isSensitiveInherited,
        isIncomingShare = isIncomingShare,
        accessPermission = accessPermission,
        canBeMovedToRubbishBin = canBeMovedToRubbishBin
    )

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
