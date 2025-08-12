package mega.privacy.android.feature.clouddrive.presentation.upload

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadFileViewModelTest {
    private lateinit var viewModel: UploadFileViewModel

    private val filePrepareUseCase = mock<FilePrepareUseCase>()
    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase>()
    private val checkFileNameCollisionsUseCase = mock<CheckFileNameCollisionsUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()

    @BeforeEach
    fun setUp() {
        viewModel = UploadFileViewModel(
            filePrepareUseCase = filePrepareUseCase,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            checkFileNameCollisionsUseCase = checkFileNameCollisionsUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
        )
    }

    @Test
    fun `test that proceedUris triggers overQuotaEvent when storage state is PayWall`() = runTest {
        val uris = listOf(
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/test.txt"
            }
        )
        val parentNodeId = NodeId(123L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.PayWall))

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)

        viewModel.proceedUris(uris, parentNodeId)

        val uiState = viewModel.uiState.value
        assertThat(uiState.overQuotaEvent).isInstanceOf(StateEvent.Triggered::class.java)
    }

    @Test
    fun `test that proceedUris uses parentNodeId when it is not -1L`() = runTest {
        val uris = listOf(
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/test.txt"
            }
        )
        val parentNodeId = NodeId(456L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(mock<DocumentEntity>())
        val collisions = emptyList<FileNameCollision>()

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(uris, parentNodeId)

        verify(checkFileNameCollisionsUseCase).invoke(entities, parentNodeId)
    }

    @Test
    fun `test that proceedUris uses root node when parentNodeId is -1L`() = runTest {
        val uris = listOf(
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/test.txt"
            }
        )
        val parentNodeId = NodeId(-1L)
        val rootNodeId = NodeId(789L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(mock<DocumentEntity>())
        val collisions = emptyList<FileNameCollision>()
        val rootNode = mock<mega.privacy.android.domain.entity.node.TypedNode>().stub {
            on { id } doReturn rootNodeId
        }

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(getRootNodeUseCase()).thenReturn(rootNode)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(uris, parentNodeId)

        verify(checkFileNameCollisionsUseCase).invoke(entities, rootNodeId)
    }

    @Test
    fun `test that proceedUris uses NodeId(-1L) when root node is null`() = runTest {
        val uris = listOf(
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/test.txt"
            }
        )
        val parentNodeId = NodeId(-1L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(mock<DocumentEntity>())
        val collisions = emptyList<FileNameCollision>()

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(getRootNodeUseCase()).thenReturn(null)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(uris, parentNodeId)

        verify(checkFileNameCollisionsUseCase).invoke(entities, NodeId(-1L))
    }

    @Test
    fun `test that proceedUris triggers nameCollisionEvent when collisions exist`() = runTest {
        val uris = listOf(
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/test.txt"
            }
        )
        val parentNodeId = NodeId(123L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(mock<DocumentEntity>())
        val collisions = listOf(
            FileNameCollision(
                collisionHandle = 1L,
                name = "test.txt",
                size = 1024L,
                childFolderCount = 0,
                childFileCount = 0,
                lastModified = 1234567890L,
                parentHandle = 123L,
                isFile = true,
                renameName = null,
                path = UriPath("/test.txt")
            )
        )

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(uris, parentNodeId)

        val uiState = viewModel.uiState.value
        assertThat(uiState.nameCollisionEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
        assertThat((uiState.nameCollisionEvent as StateEventWithContentTriggered).content).isEqualTo(
            collisions
        )
    }

    @Test
    fun `test that proceedUris triggers startUploadEvent for files without collisions`() = runTest {
        val uris = listOf(
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/test1.txt"
            },
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/test2.txt"
            }
        )
        val parentNodeId = NodeId(123L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(
            mock<DocumentEntity>().stub { on { uri } doReturn UriPath("/test1.txt") },
            mock<DocumentEntity>().stub { on { uri } doReturn UriPath("/test2.txt") }
        )
        val collisions = listOf(
            FileNameCollision(
                collisionHandle = 1L,
                name = "test1.txt",
                size = 1024L,
                childFolderCount = 0,
                childFileCount = 0,
                lastModified = 1234567890L,
                parentHandle = 123L,
                isFile = true,
                renameName = null,
                path = UriPath("/test1.txt")
            )
        )

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(uris, parentNodeId)

        val uiState = viewModel.uiState.value
        assertThat(uiState.startUploadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)

        val startUploadEvent =
            (uiState.startUploadEvent as StateEventWithContentTriggered).content
        assertThat(startUploadEvent).isInstanceOf(TransferTriggerEvent.StartUpload.Files::class.java)

        val filesEvent = startUploadEvent as TransferTriggerEvent.StartUpload.Files
        assertThat(filesEvent.pathsAndNames).containsKey("/test2.txt")
        assertThat(filesEvent.pathsAndNames).doesNotContainKey("/test1.txt")
        assertThat(filesEvent.destinationId).isEqualTo(parentNodeId)
    }

    @Test
    fun `test that proceedUris triggers startUploadEvent for all files when no collisions`() =
        runTest {
            val uris = listOf(
                mock<Uri> {
                    on { toString() } doReturn "content://com.example.provider/test1.txt"
                },
                mock<Uri> {
                    on { toString() } doReturn "content://com.example.provider/test2.txt"
                }
            )
            val parentNodeId = NodeId(123L)
            val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
            val entities = listOf(
                mock<DocumentEntity>().stub { on { uri } doReturn UriPath("/test1.txt") },
                mock<DocumentEntity>().stub { on { uri } doReturn UriPath("/test2.txt") }
            )
            val collisions = emptyList<FileNameCollision>()

            whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
            whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
            whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

            viewModel.proceedUris(uris, parentNodeId)

            val uiState = viewModel.uiState.value
            assertThat(uiState.startUploadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)

            val startUploadEvent =
                (uiState.startUploadEvent as StateEventWithContentTriggered).content
            assertThat(startUploadEvent).isInstanceOf(TransferTriggerEvent.StartUpload.Files::class.java)

            val filesEvent = startUploadEvent as TransferTriggerEvent.StartUpload.Files
            assertThat(filesEvent.pathsAndNames).containsKey("/test1.txt")
            assertThat(filesEvent.pathsAndNames).containsKey("/test2.txt")
            assertThat(filesEvent.destinationId).isEqualTo(parentNodeId)
        }

    @Test
    fun `test that proceedUris triggers uploadErrorEvent when checkFileNameCollisionsUseCase fails`() =
        runTest {
            val uris = listOf(
                mock<Uri> {
                    on { toString() } doReturn "content://com.example.provider/test.txt"
                }
            )
            val parentNodeId = NodeId(123L)
            val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
            val entities = listOf(mock<DocumentEntity>())
            val exception = RuntimeException("Collision check failed")

            whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
            whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
            whenever(checkFileNameCollisionsUseCase(any(), any())).thenThrow(exception)

            viewModel.proceedUris(uris, parentNodeId)

            val uiState = viewModel.uiState.value
            assertThat(uiState.uploadErrorEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((uiState.uploadErrorEvent as StateEventWithContentTriggered).content).isEqualTo(
                exception
            )
        }

    @Test
    fun `test that onConsumeOverQuotaEvent consumes the overQuotaEvent`() {
        // First trigger the event
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.PayWall))
        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)

        viewModel.proceedUris(
            listOf(
                mock<Uri> {
                    on { toString() } doReturn "content://com.example.provider/test.txt"
                }
            ),
            NodeId(123L)
        )

        // Verify event is triggered
        assertThat(viewModel.uiState.value.overQuotaEvent).isInstanceOf(StateEvent.Triggered::class.java)

        // Now consume it
        viewModel.onConsumeOverQuotaEvent()

        // Verify event is consumed
        assertThat(viewModel.uiState.value.overQuotaEvent).isInstanceOf(StateEvent.Consumed::class.java)
    }

    @Test
    fun `test that onConsumeNameCollisionEvent consumes the nameCollisionEvent`() = runTest {
        // First trigger the event
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(mock<DocumentEntity>())
        val collisions = listOf(
            FileNameCollision(
                collisionHandle = 1L,
                name = "test.txt",
                size = 1024L,
                childFolderCount = 0,
                childFileCount = 0,
                lastModified = 1234567890L,
                parentHandle = 123L,
                isFile = true,
                renameName = null,
                path = UriPath("/test.txt")
            )
        )

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(
            uris = listOf(
                mock<Uri> {
                    on { toString() } doReturn "content://com.example.provider/test.txt"
                }
            ),
            parentNodeId = NodeId(123L)
        )

        // Verify event is triggered
        assertThat(viewModel.uiState.value.nameCollisionEvent).isInstanceOf(
            StateEventWithContentTriggered::class.java
        )

        // Now consume it
        viewModel.onConsumeNameCollisionEvent()

        // Verify event is consumed
        assertThat(viewModel.uiState.value.nameCollisionEvent).isInstanceOf(
            StateEventWithContentConsumed::class.java
        )
    }

    @Test
    fun `test that onConsumeStartUploadEvent consumes the startUploadEvent`() = runTest {
        // First trigger the event
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(mock<DocumentEntity>())
        val collisions = emptyList<FileNameCollision>()

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(
            listOf(
                mock<Uri> {
                    on { toString() } doReturn "content://com.example.provider/test.txt"
                }
            ),
            NodeId(123L)
        )

        // Verify event is triggered
        assertThat(viewModel.uiState.value.startUploadEvent).isInstanceOf(
            StateEventWithContentTriggered::class.java
        )

        // Now consume it
        viewModel.onConsumeStartUploadEvent()

        // Verify event is consumed
        assertThat(viewModel.uiState.value.startUploadEvent).isInstanceOf(
            StateEventWithContentConsumed::class.java
        )
    }

    @Test
    fun `test that proceedUris handles empty Uri list gracefully`() = runTest {
        val uris = emptyList<Uri>()
        val parentNodeId = NodeId(123L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = emptyList<DocumentEntity>()
        val collisions = emptyList<FileNameCollision>()

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(uris, parentNodeId)

        verify(filePrepareUseCase).invoke(emptyList())
        verify(checkFileNameCollisionsUseCase).invoke(emptyList(), parentNodeId)
    }

    @Test
    fun `test that proceedUris handles single Uri correctly`() = runTest {
        val uris = listOf(
            mock<Uri> {
                on { toString() } doReturn "content://com.example.provider/single.txt"
            }
        )
        val parentNodeId = NodeId(123L)
        val storageStateFlow = MutableStateFlow(StorageStateEvent(1L, StorageState.Green))
        val entities = listOf(mock<DocumentEntity>())
        val collisions = emptyList<FileNameCollision>()

        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)
        whenever(filePrepareUseCase(any<List<UriPath>>())).thenReturn(entities)
        whenever(checkFileNameCollisionsUseCase(any(), any())).thenReturn(collisions)

        viewModel.proceedUris(uris, parentNodeId)

        verify(filePrepareUseCase).invoke(listOf(UriPath("content://com.example.provider/single.txt")))
        verify(checkFileNameCollisionsUseCase).invoke(entities, parentNodeId)
    }
}