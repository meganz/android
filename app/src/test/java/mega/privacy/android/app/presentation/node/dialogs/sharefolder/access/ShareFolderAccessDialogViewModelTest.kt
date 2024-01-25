package mega.privacy.android.app.presentation.node.dialogs.sharefolder.access

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.ShareFolderUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareFolderAccessDialogViewModelTest {
    private val sharedFolderUseCase: ShareFolderUseCase = mock()
    private val moveRequestMessageMapper: MoveRequestMessageMapper = mock()
    private val snackBarHandler: SnackBarHandler = mock()
    private val applicationScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    private val underTest = ShareFolderAccessDialogViewModel(
        sharedFolderUseCase = sharedFolderUseCase,
        moveRequestMessageMapper = moveRequestMessageMapper,
        snackBarHandler = snackBarHandler,
        applicationScope = applicationScope
    )

    @Test
    fun `test that the snackbar handler posts a message and sharedFolderUseCase is invoked when share folder is called`() =
        runTest {
            val handles = listOf(1234L, 2345L)
            val contactData = listOf("sample@mega.co.nz", "test@mega.co.nz")
            val nodeIds = handles.map {
                NodeId(it)
            }
            val shareMovement = MoveRequestResult.ShareMovement(
                count = 1,
                errorCount = 0,
                nodes = listOf()
            )
            whenever(
                sharedFolderUseCase(
                    nodeIds = nodeIds,
                    contactData = contactData,
                    accessPermission = AccessPermission.READ
                )
            ).thenReturn(
                shareMovement
            )
            whenever(moveRequestMessageMapper(shareMovement)).thenReturn("Any message")

            underTest.shareFolder(
                handles = handles,
                contactData = contactData,
                accessPermission = AccessPermission.READ
            )

            verify(sharedFolderUseCase).invoke(
                nodeIds = nodeIds,
                contactData = contactData,
                accessPermission = AccessPermission.READ
            )
            verify(moveRequestMessageMapper).invoke(shareMovement)
            verify(snackBarHandler).postSnackbarMessage("Any message")
        }

}