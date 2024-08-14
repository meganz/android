package mega.privacy.android.app.presentation.node

import app.cash.turbine.test
import com.google.common.truth.Truth
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.chat.mapper.ChatRequestMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeContentUriIntentMapper
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.presentation.versions.mapper.VersionHistoryRemoveMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.ChatRequestResult
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.time.Duration

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeActionsViewModelTest {
    private lateinit var viewModel: NodeActionsViewModel

    private val checkNodesNameCollisionUseCase = mock<CheckNodesNameCollisionUseCase>()
    private val moveNodesUseCase = mock<MoveNodesUseCase>()
    private val copyNodesUseCase = mock<CopyNodesUseCase>()
    private val setCopyLatestTargetPathUseCase = mock<SetCopyLatestTargetPathUseCase>()
    private val setMoveLatestTargetPathUseCase = mock<SetMoveLatestTargetPathUseCase>()
    private val deleteNodeVersionsUseCase = mock<DeleteNodeVersionsUseCase>()
    private val moveRequestMessageMapper = mock<MoveRequestMessageMapper>()
    private val versionHistoryRemoveMessageMapper = mock<VersionHistoryRemoveMessageMapper>()
    private val snackBarHandler = mock<SnackBarHandler>()
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase = mock()
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase = mock()
    private val chatRequestMessageMapper: ChatRequestMessageMapper = mock()
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper = mock()
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper = mock()
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase = mock()
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase = mock()
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase = mock()
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase = mock()
    private val sampleNode = mock<TypedFileNode>().stub {
        on { id } doReturn NodeId(123)
    }
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

    private fun initViewModel() {
        viewModel = NodeActionsViewModel(
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            deleteNodeVersionsUseCase = deleteNodeVersionsUseCase,
            moveRequestMessageMapper = moveRequestMessageMapper,
            versionHistoryRemoveMessageMapper = versionHistoryRemoveMessageMapper,
            snackBarHandler = snackBarHandler,
            checkBackupNodeTypeByHandleUseCase = checkBackupNodeTypeByHandleUseCase,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            chatRequestMessageMapper = chatRequestMessageMapper,
            listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
            getNodeContentUriUseCase = getNodeContentUriUseCase,
            nodeContentUriIntentMapper = nodeContentUriIntentMapper,
            getPathFromNodeContentUseCase = getPathFromNodeContentUseCase,
            getNodePreviewFileUseCase = getNodePreviewFileUseCase,
            applicationScope = applicationScope,
            updateNodeSensitiveUseCase = updateNodeSensitiveUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        )
    }

    @Test
    fun `test that moveNodesUseCase is called when move node method is invoked`() =
        runTest {
            whenever(moveNodesUseCase(emptyMap())).thenThrow(ForeignNodeException())
            initViewModel()
            viewModel.moveNodes(emptyMap())
            verify(moveNodesUseCase).invoke(emptyMap())
            viewModel.state.test {
                val state = awaitItem()
                Truth.assertThat(state.showForeignNodeDialog)
                    .isInstanceOf(StateEvent.Triggered::class.java)
            }
        }

    @Test
    fun `test that node name collision results are updated properly in state`() = runTest {
        whenever(
            checkNodesNameCollisionUseCase(
                nodes = listOf(element = 1).associate { Pair(1, sampleNode.id.longValue) },
                type = NodeNameCollisionType.MOVE,
            ),
        ).thenReturn(
            NodeNameCollisionsResult(
                noConflictNodes = emptyMap(),
                conflictNodes = emptyMap(),
                type = NodeNameCollisionType.MOVE
            )
        )
        initViewModel()
        viewModel.checkNodesNameCollision(
            nodes = listOf(element = 1),
            targetNode = sampleNode.id.longValue,
            type = NodeNameCollisionType.MOVE
        )
        viewModel.state.test {
            val stateOne = awaitItem()
            Truth.assertThat(stateOne.nodeNameCollisionsResult).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )
        }
        viewModel.markHandleNodeNameCollisionResult()
        viewModel.state.test {
            val stateTwo = awaitItem()
            Truth.assertThat(stateTwo.nodeNameCollisionsResult).isInstanceOf(
                StateEventWithContentConsumed::class.java
            )
        }
    }

    @Test
    fun `test that setMoveTargetPath is called when move node is success`() = runTest {
        whenever(moveNodesUseCase(mapOf(sampleNode.id.longValue to sampleNode.id.longValue)))
            .thenReturn(MoveRequestResult.GeneralMovement(0, 0))
        initViewModel()
        viewModel.moveNodes(mapOf(sampleNode.id.longValue to sampleNode.id.longValue))
        verify(setMoveLatestTargetPathUseCase).invoke(sampleNode.id.longValue)
    }

    @Test
    fun `test that deleteNodeVersionsUseCase is triggered when delete node history is called`() =
        runTest {
            whenever(deleteNodeVersionsUseCase(sampleNode.id)).thenReturn(Unit)
            whenever(versionHistoryRemoveMessageMapper(anyOrNull())).thenReturn("")
            initViewModel()
            viewModel.deleteVersionHistory(sampleNode.id.longValue)
            verify(deleteNodeVersionsUseCase).invoke(sampleNode.id)
            verify(versionHistoryRemoveMessageMapper).invoke(anyOrNull())
            verify(snackBarHandler).postSnackbarMessage("")
        }

    @Test
    fun `test that copyNodesUseCase is called when copy node method is invoked`() =
        runTest {
            whenever(copyNodesUseCase(emptyMap())).thenThrow(ForeignNodeException())
            initViewModel()
            viewModel.copyNodes(emptyMap())
            verify(copyNodesUseCase).invoke(emptyMap())
            viewModel.state.test {
                val state = awaitItem()
                Truth.assertThat(state.showForeignNodeDialog)
                    .isInstanceOf(StateEvent.Triggered::class.java)
            }
        }

    @Test
    fun `test that setCopyTargetPath is called when copy node is success`() = runTest {
        whenever(copyNodesUseCase(mapOf(sampleNode.id.longValue to sampleNode.id.longValue)))
            .thenReturn(MoveRequestResult.GeneralMovement(0, 0))
        initViewModel()
        viewModel.copyNodes(mapOf(sampleNode.id.longValue to sampleNode.id.longValue))
        verify(setCopyLatestTargetPathUseCase).invoke(sampleNode.id.longValue)
    }

    @Test
    fun `test that contactSelectedForShareFolder is called when contact list is selected`() =
        runTest {
            initViewModel()
            viewModel.contactSelectedForShareFolder(
                listOf("sample@mega.co.nz", "test@mega.co.nz"),
                listOf(1234L, 346L)
            )
            viewModel.state.test {
                val state = awaitItem()
                Truth.assertThat(state.contactsData)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    fun `test that chatRequestMessageMapper is called when chatIds is selected`() =
        runTest {
            initViewModel()
            val chatIds = longArrayOf(1234L)
            val nodeIds = longArrayOf(sampleNode.id.longValue)
            val request = ChatRequestResult.ChatRequestAttachNode(
                count = 1,
                errorCount = 0
            )

            whenever(
                attachMultipleNodesUseCase(
                    listOf(sampleNode.id),
                    listOf(1234L)
                )
            ).thenReturn(request)
            whenever(chatRequestMessageMapper(request)).thenReturn("Some value")

            viewModel.attachNodeToChats(
                nodeHandles = nodeIds,
                chatIds = chatIds,
                userHandles = longArrayOf()
            )

            verify(attachMultipleNodesUseCase).invoke(
                listOf(sampleNode.id),
                listOf(1234L)
            )
            verify(chatRequestMessageMapper).invoke(request)
            verify(snackBarHandler).postSnackbarMessage("Some value")
        }

    @ParameterizedTest(name = "File type is {0}")
    @MethodSource("provideNodeType")
    fun `test that invoke is called when node is provided with different file types`(
        node: TypedFileNode,
        expected: FileNodeContent,
    ) =
        runTest {
            val content = NodeContentUri.LocalContentUri(File("path"))
            whenever(getNodeContentUriUseCase(node)).thenReturn(
                content
            )
            whenever(getNodePreviewFileUseCase(any())).thenReturn(File("path"))

            initViewModel()
            val actual = viewModel.handleFileNodeClicked(node)
            when (node.type) {
                is ImageFileTypeInfo -> {
                    verifyNoMoreInteractions(getNodeContentUriUseCase)
                }

                is TextFileTypeInfo -> {
                    verifyNoMoreInteractions(getNodeContentUriUseCase)
                }

                is UrlFileTypeInfo -> {
                    verify(getNodeContentUriUseCase).invoke(node)
                    verify(getPathFromNodeContentUseCase).invoke(content)
                }

                is VideoFileTypeInfo,
                is PdfFileTypeInfo,
                is AudioFileTypeInfo,
                -> {
                    verify(getNodeContentUriUseCase).invoke(node)
                }

                else -> {
                    verify(getNodePreviewFileUseCase).invoke(node)
                }
            }
            Truth.assertThat(actual).isInstanceOf(expected::class.java)
        }

    @Test
    fun `test that isOnboarding should return true when isPaid is true`() = runTest {
        val accountType = mock<AccountType> {
            on { isPaid } doReturn true
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `test that isOnboarding should return false when accountType isPaid is false`() = runTest {
        val accountType = mock<AccountType> {
            on { isPaid } doReturn false
        }
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `test that isOnboarding should return false when accountType is null`() = runTest {
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn null
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `test that isOnboarding should return false when levelDetail is null`() = runTest {
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn null
        }
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        initViewModel()
        val result = viewModel.isOnboarding()
        Truth.assertThat(result).isFalse()
    }

    private fun provideNodeType() = Stream.of(
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn PdfFileTypeInfo
            },
            mock<FileNodeContent.Pdf>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn VideoFileTypeInfo(
                    extension = "mp4",
                    mimeType = "video",
                    duration = Duration.INFINITE
                )
            },
            mock<FileNodeContent.AudioOrVideo>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn AudioFileTypeInfo(
                    extension = "mp3",
                    mimeType = "audio",
                    duration = Duration.INFINITE
                )
            },
            mock<FileNodeContent.AudioOrVideo>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                on { type } doReturn StaticImageFileTypeInfo(
                    extension = "jpeg",
                    mimeType = "image",
                )
            },
            mock<FileNodeContent.ImageForNode>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    TextFileTypeInfo(
                        mimeType = "text/plain",
                        extension = "txt"
                    )
                )
            },
            mock<FileNodeContent.TextContent>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    ZipFileTypeInfo(
                        mimeType = "zip",
                        extension = "zip"
                    )
                )
            }, mock<FileNodeContent.Other>()
        ), Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    UnknownFileTypeInfo(
                        mimeType = "abc",
                        extension = "abc"
                    )
                )
            }, mock<FileNodeContent.Other>()
        ),
        Arguments.of(
            mock<TypedFileNode>().stub {
                whenever(it.type).thenReturn(
                    UrlFileTypeInfo
                )
            },
            mock<FileNodeContent.UrlContent>()
        )
    )
}

