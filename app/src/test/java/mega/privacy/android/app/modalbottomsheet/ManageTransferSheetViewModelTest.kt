package mega.privacy.android.app.modalbottomsheet

import android.net.Uri
import androidx.annotation.Nullable
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetCompletedTransferByIdUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadDocumentFileUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadParentDocumentFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManageTransferSheetViewModelTest {
    private lateinit var underTest: ManageTransferSheetViewModel
    private val getCompletedTransferByIdUseCase: GetCompletedTransferByIdUseCase = mock()
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase = mock()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val getDownloadParentDocumentFileUseCase = mock<GetDownloadParentDocumentFileUseCase>()
    private val getDownloadDocumentFileUseCase = mock<GetDownloadDocumentFileUseCase>()
    private val getPathByDocumentContentUriUseCase = mock<GetPathByDocumentContentUriUseCase>()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeAll
    fun setup() {
    }

    private fun initTestClass() {
        underTest = ManageTransferSheetViewModel(
            getCompletedTransferByIdUseCase = getCompletedTransferByIdUseCase,
            deleteCompletedTransferUseCase = deleteCompletedTransferUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            getDownloadParentDocumentFileUseCase = getDownloadParentDocumentFileUseCase,
            getDownloadDocumentFileUseCase = getDownloadDocumentFileUseCase,
            getPathByDocumentContentUriUseCase = getPathByDocumentContentUriUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCompletedTransferByIdUseCase,
            deleteCompletedTransferUseCase,
            getNodeAccessPermission,
            getDownloadParentDocumentFileUseCase,
            getDownloadDocumentFileUseCase,
            getPathByDocumentContentUriUseCase,
            savedStateHandle,
        )
    }

    @Test
    fun `test that ManageTransferSheetUiState update correctly`() = runTest {
        val completedTransfer = mock<CompletedTransfer>()
        whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID)).thenReturn(
            1
        )
        whenever(getCompletedTransferByIdUseCase(1)).thenReturn(completedTransfer)
        initTestClass()
        underTest.uiState.test {
            assertThat(awaitItem().transfer).isEqualTo(completedTransfer)
        }
    }

    @ParameterizedTest(name = " and use case returns {0}")
    @EnumSource(AccessPermission::class)
    @Nullable
    fun `test that when completed transfer is obtained, getNodeAccessPermission is invoked and state is updated correctly`(
        accessPermission: AccessPermission?,
    ) = runTest {
        val transferId = 1
        val handle = 234L
        val completedTransfer = mock<CompletedTransfer> {
            on { this.handle } doReturn handle
        }

        whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID))
            .thenReturn(transferId)
        whenever(getCompletedTransferByIdUseCase(transferId)).thenReturn(completedTransfer)
        whenever(getNodeAccessPermission(NodeId(handle))).thenReturn(accessPermission)

        initTestClass()

        underTest.uiState.test {
            assertThat(awaitItem().iAmNodeOwner)
                .isEqualTo(accessPermission == AccessPermission.OWNER)
        }
    }

    @Test
    fun `test that completedTransferRemoved invoke correctly`() = runTest {
        val completedTransfer = mock<CompletedTransfer>()
        underTest.completedTransferRemoved(completedTransfer, true)
        verify(deleteCompletedTransferUseCase).invoke(completedTransfer, true)
    }

    @Test
    fun `test that parentDocumentFile is updated if the completed transfer is a content uri download`() =
        runTest {
            val path = "content://some/path"
            val completedTransfer = mock<CompletedTransfer> {
                on { isContentUriDownload } doReturn true
                on { type } doReturn TransferType.NONE
                on { this.path } doReturn path
            }
            val uriPath = UriPath(path)
            val expected = mock<DocumentEntity> {
                on { uri } doReturn uriPath
            }
            mockStatic(Uri::class.java).use { uriMock ->
                val uri = mock<Uri> {
                    on { scheme } doReturn "content"
                }

                whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID)) doReturn 1
                whenever(getCompletedTransferByIdUseCase(1)) doReturn completedTransfer
                whenever(getDownloadParentDocumentFileUseCase(path)) doReturn expected
                whenever(Uri.parse(path)) doReturn uri

                initTestClass()

                underTest.uiState.test {
                    advanceUntilIdle()
                    assertThat(awaitItem().parentDocumentFile).isEqualTo(expected)
                }
            }
        }

    @Test
    fun `test that parentFilePath is updated if the completed transfer is a content uri download`() =
        runTest {
            val path = "content://some/path"
            val completedTransfer = mock<CompletedTransfer> {
                on { isContentUriDownload } doReturn true
                on { type } doReturn TransferType.NONE
                on { this.path } doReturn path
            }
            val uriPath = UriPath(path)
            val documentEntity = mock<DocumentEntity> {
                on { uri } doReturn uriPath
                on { getUriString() } doReturn uriPath.value
            }
            val expected = "some/expected/path"
            mockStatic(Uri::class.java).use { uriMock ->
                val uri = mock<Uri> {
                    on { scheme } doReturn "content"
                }

                whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID))
                    .thenReturn(1)
                whenever(getCompletedTransferByIdUseCase(1)) doReturn completedTransfer
                whenever(getDownloadParentDocumentFileUseCase(path)) doReturn documentEntity
                whenever(Uri.parse(path)) doReturn uri
                whenever(getPathByDocumentContentUriUseCase(uriPath.value)) doReturn expected

                initTestClass()

                underTest.uiState.test {
                    advanceUntilIdle()
                    assertThat(awaitItem().parentFilePath).isEqualTo(expected)
                }
            }
        }


    @Test
    fun `test that documentFile is updated if the completed transfer is a content uri download`() =
        runTest {
            val path = "content://some/path"
            val fileName = "file.txt"
            val completedTransfer = mock<CompletedTransfer> {
                on { isContentUriDownload } doReturn true
                on { type } doReturn TransferType.NONE
                on { this.path } doReturn path
                on { this.fileName } doReturn fileName
            }
            val uriPath = UriPath(path)
            val expected = mock<DocumentEntity> {
                on { uri } doReturn uriPath
            }
            mockStatic(Uri::class.java).use { uriMock ->
                val uri = mock<Uri> {
                    on { scheme } doReturn "content"
                }

                whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID)) doReturn 1
                whenever(getCompletedTransferByIdUseCase(1)) doReturn completedTransfer
                whenever(getDownloadDocumentFileUseCase(path, fileName)) doReturn expected
                whenever(Uri.parse(path)) doReturn uri

                initTestClass()

                underTest.uiState.test {
                    advanceUntilIdle()
                    assertThat(awaitItem().documentFile).isEqualTo(expected)
                }
            }
        }

    @Test
    fun `test that parentDocumentFile, documentFile, and parentFilePath are not updated if the completed transfer is not a content uri download`() =
        runTest {
            val completedTransfer = mock<CompletedTransfer> {
                on { isContentUriDownload } doReturn false
                on { type } doReturn TransferType.DOWNLOAD
            }

            whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID)) doReturn 1
            whenever(getCompletedTransferByIdUseCase(1)) doReturn completedTransfer

            initTestClass()

            underTest.uiState.test {
                advanceUntilIdle()
                val actual = awaitItem()
                assertThat(actual.parentDocumentFile).isNull()
                assertThat(actual.documentFile).isNull()
                assertThat(actual.parentFilePath).isNull()
            }

            verifyNoInteractions(getDownloadParentDocumentFileUseCase)
            verifyNoInteractions(getDownloadDocumentFileUseCase)
            verifyNoInteractions(getPathByDocumentContentUriUseCase)
        }
}