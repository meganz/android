package mega.privacy.android.app.uploadFolder

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.namecollision.data.NameCollisionResultUiEntity
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.file.ApplySortOrderToDocumentFolderUseCase
import mega.privacy.android.domain.usecase.file.CheckFileNameCollisionsUseCase
import mega.privacy.android.domain.usecase.file.GetFilesInDocumentFolderUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExtendWith(CoroutineMainDispatcherExtension::class, InstantExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadFolderViewModelTest {
    private lateinit var underTest: UploadFolderViewModel

    private val checkFileNameCollisionsUseCase: CheckFileNameCollisionsUseCase = mock()
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()
    private val getFilesInDocumentFolderUseCase: GetFilesInDocumentFolderUseCase = mock()
    private val applySortOrderToDocumentFolderUseCase: ApplySortOrderToDocumentFolderUseCase =
        mock()

    @BeforeEach
    fun setup() {
        underTest = UploadFolderViewModel(
            getFilesInDocumentFolderUseCase = getFilesInDocumentFolderUseCase,
            applySortOrderToDocumentFolderUseCase = applySortOrderToDocumentFolderUseCase,
            documentEntityDataMapper = mock(),
            searchFilesInDocumentFolderRecursiveUseCase = mock(),
            checkFileNameCollisionsUseCase = checkFileNameCollisionsUseCase,
            getRootNodeIdUseCase = getRootNodeIdUseCase
        )
    }

    @Test
    fun `test that the state event is triggered when proceedWithUpload is invoked`() =
        runTest {
            val folderName = "myFolder"
            val folderUriString = "content://test/myFolder"
            populatePendingUpload(folderName, folderUriString)
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(12345L))
            underTest.consumeTransferTriggerEvent()
            underTest.proceedWithUpload(null)

            val actual =
                (underTest.uiState.value.transferTriggerEvent as? StateEventWithContentTriggered)?.content

            assertThat(actual).isInstanceOf(TransferTriggerEvent.StartUpload.Files::class.java)
            assertThat(actual?.waitNotificationPermissionResponseToStart).isTrue()
        }

    @Test
    fun `test that the state event is consumed when consume transfer trigger event is invoked`() =
        runTest {
            val folderName = "myFolder"
            val folderUriString = "content://test/myFolder"
            populatePendingUpload(folderName, folderUriString)
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(12345L))
            underTest.proceedWithUpload(null)
            underTest.consumeTransferTriggerEvent()

            val actual = underTest.uiState.value.transferTriggerEvent

            assertThat(actual).isInstanceOf(StateEventWithContentConsumed::class.java)
        }

    @Test
    fun `test that proceedWithUpload finishes the activity without triggering an upload when all items are resolved with don't upload`() =
        runTest {
            val folderName = "myFolder"
            val folderUriString = "content://test/myFolder"
            populatePendingUpload(folderName, folderUriString)

            underTest.consumeTransferTriggerEvent()
            val actionResultObserver = mock<Observer<String?>>()
            underTest.onActionResult().observeForever(actionResultObserver)

            underTest.proceedWithUpload(
                listOf(buildResolution(folderName, NameCollisionChoice.CANCEL))
            )

            assertThat(underTest.uiState.value.transferTriggerEvent)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
            verify(actionResultObserver).onChanged(null)
            underTest.onActionResult().removeObserver(actionResultObserver)
        }

    @Test
    fun `test that proceedWithUpload uploads the original item when resolved with replace or merge`() =
        runTest {
            val folderName = "myFolder"
            val folderUriString = "content://test/myFolder"
            populatePendingUpload(folderName, folderUriString)

            underTest.consumeTransferTriggerEvent()
            underTest.proceedWithUpload(
                listOf(buildResolution(folderName, NameCollisionChoice.REPLACE_UPDATE_MERGE))
            )

            val event = underTest.requireFilesEvent()
            assertThat(event.pathsAndNames).containsExactly(folderUriString, folderName)
        }

    @Test
    fun `test that proceedWithUpload uses the renamed name when resolved with rename`() = runTest {
        val folderName = "myFolder"
        val folderUriString = "content://test/myFolder"
        val renamedName = "myFolder (1)"
        populatePendingUpload(folderName, folderUriString)

        underTest.consumeTransferTriggerEvent()
        underTest.proceedWithUpload(
            listOf(
                buildResolution(
                    name = folderName,
                    choice = NameCollisionChoice.RENAME,
                    renameName = renamedName,
                )
            )
        )

        val event = underTest.requireFilesEvent()
        assertThat(event.pathsAndNames).containsExactly(folderUriString, renamedName)
    }

    private suspend fun populatePendingUpload(folderName: String, folderUriString: String) {
        whenever(applySortOrderToDocumentFolderUseCase(any()))
            .thenReturn(emptyList<DocumentEntity>() to emptyList())
        whenever(getFilesInDocumentFolderUseCase(any())).thenReturn(DocumentFolder(emptyList()))
        whenever(
            checkFileNameCollisionsUseCase(
                files = any(),
                parentNodeId = any(),
                pitagTrigger = any()
            )
        ).thenReturn(listOf(buildFileNameCollision(folderName)))

        val uri = mock<Uri> { on { toString() } doReturn folderUriString }
        val documentFile = mock<DocumentFile> {
            on { name } doReturn folderName
            on { isFile } doReturn false
            on { lastModified() } doReturn 0L
            on { length() } doReturn 0L
            on { this.uri } doReturn uri
        }

        underTest.retrieveFolderContent(
            documentFile = documentFile,
            parentHandle = PARENT_HANDLE,
            order = SortOrder.ORDER_DEFAULT_ASC,
            isList = true,
        )
        underTest.upload()
    }

    private fun buildResolution(
        name: String,
        choice: NameCollisionChoice,
        renameName: String? = null,
    ) = NameCollisionResultUiEntity(
        nameCollision = NameCollisionUiEntity.Upload(
            collisionHandle = 0L,
            absolutePath = "",
            name = name,
            lastModified = 0L,
            parentHandle = PARENT_HANDLE,
            isFile = false,
            renameName = renameName,
            pitagTrigger = PitagTrigger.Picker,
        ),
        choice = choice,
    )

    private fun buildFileNameCollision(name: String) = FileNameCollision(
        collisionHandle = 0L,
        name = name,
        size = 0L,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = 0L,
        parentHandle = PARENT_HANDLE,
        isFile = false,
        renameName = null,
        path = UriPath(""),
        pitagTrigger = PitagTrigger.Picker,
    )

    private fun UploadFolderViewModel.requireFilesEvent(): TransferTriggerEvent.StartUpload.Files {
        val content =
            (uiState.value.transferTriggerEvent as StateEventWithContentTriggered).content
        return content as TransferTriggerEvent.StartUpload.Files
    }

    private companion object {
        const val PARENT_HANDLE = 100L
    }
}
