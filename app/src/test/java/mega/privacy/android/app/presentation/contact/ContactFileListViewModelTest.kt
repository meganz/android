package mega.privacy.android.app.presentation.contact

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactFileListViewModelTest {
    private lateinit var underTest: ContactFileListViewModel
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase = mock()
    private val moveNodesUseCase: MoveNodesUseCase = mock()
    private val copyNodesUseCase: CopyNodesUseCase = mock()
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase = mock()
    private val filePrepareUseCase: FilePrepareUseCase = mock()
    private val scannerHandler: ScannerHandler = mock()
    private val nodeNameCollision = mock<NodeNameCollision.Default> {
        on { nodeHandle }.thenReturn(2L)
        on { collisionHandle }.thenReturn(123L)
        on { name }.thenReturn("node")
        on { size }.thenReturn(199L)
        on { childFileCount }.thenReturn(5)
        on { childFolderCount }.thenReturn(10)
        on { lastModified }.thenReturn(234L)
        on { parentHandle }.thenReturn(345L)
        on { isFile }.thenReturn(true)
    }
    private val nodes = listOf(1L, 2L)
    private val targetNode = 100L

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase,
            moveNodesToRubbishUseCase,
            checkNodesNameCollisionUseCase,
            moveNodesUseCase,
            copyNodesUseCase,
            getNodeContentUriByHandleUseCase,
            filePrepareUseCase,
            scannerHandler,
        )
    }

    private fun initTestClass() {
        underTest = ContactFileListViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            getNodeContentUriByHandleUseCase = getNodeContentUriByHandleUseCase,
            filePrepareUseCase = filePrepareUseCase,
            scannerHandler = scannerHandler,
        )
    }

    @ParameterizedTest(name = "test that snack bar message is updated when internet connectivity is not available and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that snack bar message is updated when internet connectivity is not available`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes = nodes, targetNode = targetNode, type = type)
                val state1 = awaitItem()
                assertThat(state1.snackBarMessage).isNotNull()
                underTest.onConsumeSnackBarMessageEvent()
                assertThat(awaitItem().snackBarMessage).isNull()
            }
        }

    @ParameterizedTest(name = "test that copyMoveAlertTextId is updated when checkNameCollision failed and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that copyMoveAlertTextId is updated when checkNameCollision failed`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(
                checkNodesNameCollisionUseCase(nodes = mapOf(1L to 100L, 2L to 100L), type = type)
            ).thenThrow(RuntimeException::class.java)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes = nodes, targetNode = targetNode, type = type)
                val copyStartedState = awaitItem()
                assertThat(copyStartedState.copyMoveAlertTextId).isNotNull()
                assertThat(awaitItem().copyMoveAlertTextId).isNull()
            }
        }

    @ParameterizedTest(name = "test that moveRequestResult updated correctly when calling copyOrMoveNodes failed and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that moveRequestResult updated correctly when calling copyOrMoveNodes failed`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            val nameCollisionResult = NodeNameCollisionsResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenThrow(RuntimeException::class.java)
            whenever(
                copyNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenThrow(RuntimeException::class.java)
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isFailure).isTrue()
            }
        }


    @ParameterizedTest(name = "test that moveRequestResult updated correctly when calling copyOrMoveNodes successfully and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that moveRequestResult updated correctly when calling copyOrMoveNodes successfully`(
        type: NodeNameCollisionType,
    ) =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.GeneralMovement>()
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            whenever(
                copyNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            val nameCollisionResult = NodeNameCollisionsResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
            }
        }

    @ParameterizedTest(name = "test that nodeNameCollisionResult updated when calling checkNodesNameCollisionUseCase successfully and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that nodeNameCollisionResult updated when calling checkNodesNameCollisionUseCase successfully`(
        type: NodeNameCollisionType,
    ) =
        runTest {
            initTestClass()
            val nameCollisionResult = NodeNameCollisionsResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            val result = mock<MoveRequestResult.GeneralMovement>()
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.nodeNameCollisionResult).isEmpty()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                assertThat(awaitItem().nodeNameCollisionResult).isNotEmpty()
                underTest.markHandleNodeNameCollisionResult()
                assertThat(awaitItem().nodeNameCollisionResult).isEmpty()
            }
        }

    @ParameterizedTest(name = "test that moveRequestResult updated when calling markHandleMoveRequestResult and transfer type is {0}")
    @EnumSource(NodeNameCollisionType::class, names = ["RESTORE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that moveRequestResult updated when calling markHandleMoveRequestResult`(type: NodeNameCollisionType) =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.GeneralMovement>()
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            val nameCollisionResult = NodeNameCollisionsResult(
                noConflictNodes = mapOf(pair = Pair(1L, 100L)),
                conflictNodes = mapOf(Pair(2L, nodeNameCollision)),
                type = type
            )
            whenever(
                checkNodesNameCollisionUseCase(
                    nodes = mapOf(1L to 100L, 2L to 100L),
                    type = type
                )
            ).thenReturn(nameCollisionResult)
            whenever(
                moveNodesUseCase(mapOf(Pair(1L, 100L)))
            ).thenReturn(result)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.copyOrMoveNodes(nodes, targetNode, type)
                assertThat(awaitItem().copyMoveAlertTextId).isNotNull()
                val movementComplete = awaitItem()
                assertThat(movementComplete.nodeNameCollisionResult).isNotEmpty()
                assertThat(movementComplete.moveRequestResult).isNotNull()
                assertThat(movementComplete.moveRequestResult?.isSuccess).isTrue()
                underTest.markHandleMoveRequestResult()
                assertThat(awaitItem().moveRequestResult).isNull()
            }
        }

    @Test
    fun `test that StorageState is returned when getStorageState is called`() = runTest {
        initTestClass()
        whenever(monitorStorageStateEventUseCase()).thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 1L,
                    storageState = StorageState.Red
                )
            )
        )
        val state = underTest.getStorageState()
        assertThat(state).isEqualTo(StorageState.Red)
    }

    @Test
    fun `test that moveRequestResult is updated correctly when calling moveNodesToRubbish failed`() =
        runTest {
            initTestClass()
            whenever(moveNodesToRubbishUseCase(nodes)).thenThrow(RuntimeException::class.java)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.moveNodesToRubbish(nodes)
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isFailure).isTrue()
            }
        }

    @Test
    fun `test that moveRequestResult is updated correctly when calling moveNodesToRubbish completed successfully`() =
        runTest {
            initTestClass()
            val result = mock<MoveRequestResult.RubbishMovement>()
            whenever(moveNodesToRubbishUseCase(nodes)).thenReturn(result)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.moveNodesToRubbish(nodes)
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
            }
        }

    @Test
    fun `test that state is updated correctly if a File is uploaded`() = runTest {
        val file = File("path")
        val parentHandle = 123L
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(file.absolutePath to null),
                NodeId(parentHandle)
            )
        )

        underTest.uploadFile(file, parentHandle)
        underTest.state.map { it.uploadEvent }.test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that getNodeContentUriByHandleUseCase is invoked and returns as expected`() =
        runTest {
            val paramHandle = 1L
            val expectedContentUri = NodeContentUri.RemoteContentUri("", false)
            whenever(getNodeContentUriByHandleUseCase(paramHandle)).thenReturn(expectedContentUri)
            val actual = underTest.getNodeContentUri(paramHandle)
            assertThat(actual).isEqualTo(expectedContentUri)
            verify(getNodeContentUriByHandleUseCase).invoke(paramHandle)
        }

    @Test
    fun `test that prepare file is invoked and returns as expected`() = runTest {
        val uri = mock<Uri> {
            on { toString() } doReturn "uri"
        }
        val entity = mock<DocumentEntity>()
        whenever(filePrepareUseCase(listOf(uri).map { UriPath(it.toString()) }))
            .thenReturn(listOf(entity))
        val actual = underTest.prepareFiles(listOf(uri))
        assertThat(actual).isEqualTo(listOf(entity))
    }

    @Test
    fun `test that the ML Kit Document Scanner is initialized and ready to scan documents`() =
        runTest {
            val gmsDocumentScanner = mock<GmsDocumentScanner>()
            whenever(scannerHandler.prepareDocumentScanner()).thenReturn(gmsDocumentScanner)

            underTest.prepareDocumentScanner()

            underTest.state.test {
                assertThat(awaitItem().gmsDocumentScanner).isEqualTo(gmsDocumentScanner)
            }
        }

    @Test
    fun `test that an insufficient RAM to launch error is returned when initializing the ML Kit Document Scanner with low device RAM`() =
        runTest {
            whenever(scannerHandler.prepareDocumentScanner()).thenAnswer {
                throw InsufficientRAMToLaunchDocumentScanner()
            }

            assertDoesNotThrow { underTest.prepareDocumentScanner() }

            underTest.state.test {
                assertThat(awaitItem().documentScanningError).isEqualTo(DocumentScanningError.InsufficientRAM)
            }
        }

    @Test
    fun `test that a generic error is returned when initializing the ML Kit Document Scanner results in an error`() =
        runTest {
            whenever(scannerHandler.prepareDocumentScanner()).thenThrow(RuntimeException())

            assertDoesNotThrow { underTest.prepareDocumentScanner() }

            underTest.state.test {
                assertThat(awaitItem().documentScanningError).isEqualTo(DocumentScanningError.GenericError)
            }
        }

    @Test
    fun `test that a generic error is returned when opening the ML Kit Document Scanner results in an error`() =
        runTest {
            underTest.onDocumentScannerFailedToOpen()

            underTest.state.test {
                assertThat(awaitItem().documentScanningError).isEqualTo(DocumentScanningError.GenericError)
            }
        }

    @Test
    fun `test that the gms document scanner is reset`() = runTest {
        underTest.onGmsDocumentScannerConsumed()

        underTest.state.test {
            assertThat(awaitItem().gmsDocumentScanner).isNull()
        }
    }

    @Test
    fun `test that the document scanning error is reset`() = runTest {
        underTest.onDocumentScanningErrorConsumed()

        underTest.state.test {
            assertThat(awaitItem().documentScanningError).isNull()
        }
    }

    @Test
    fun `test that the leave folder ids are set when setLeaveFolderNodeIds is called`() = runTest {
        val ids = listOf(1L, 2L)
        underTest.setLeaveFolderNodeIds(ids)

        underTest.state.test {
            assertThat(awaitItem().leaveFolderNodeIds).isEqualTo(ids)
        }
    }

    @Test
    fun `test that the leave folder ids are reset when clearLeaveFolderNodeIds is called`() =
        runTest {
            val ids = listOf(1L, 2L)
            underTest.setLeaveFolderNodeIds(ids)

            underTest.state.test {
                assertThat(awaitItem().leaveFolderNodeIds).isEqualTo(ids)
                underTest.clearLeaveFolderNodeIds()
                assertThat(awaitItem().leaveFolderNodeIds).isNull()
            }
        }
}
