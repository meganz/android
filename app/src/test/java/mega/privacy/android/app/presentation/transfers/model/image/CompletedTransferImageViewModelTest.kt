package mega.privacy.android.app.presentation.transfers.model.image

import androidx.core.net.toUri
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.model.image.CompletedTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.TransferImageUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.data.mapper.transfer.TransferStateMapper
import mega.privacy.android.data.mapper.transfer.TransferTypeMapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.icon.pack.R
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompletedTransferImageViewModelTest {

    private lateinit var underTest: CompletedTransferImageViewModel

    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val transferStateMapper = mock<TransferStateMapper>()
    private val transferTypeMapper = mock<TransferTypeMapper>()

    private val id = 1
    private val extension = "txt"
    private val name = "name.$extension"
    private val localPath = "localPath/$name"
    private val nodeHandle = 1L
    private val fileTypeResId = R.drawable.ic_text_medium_solid
    private val size = "size"

    @BeforeEach
    fun resetMocks() {
        reset(getThumbnailUseCase, fileTypeIconMapper, transferStateMapper, transferTypeMapper)
    }

    private fun initTestClass() {
        underTest = CompletedTransferImageViewModel(
            getThumbnailUseCase = getThumbnailUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
            transferStateMapper = transferStateMapper,
            transferTypeMapper = transferTypeMapper,
        )
    }

    @Test
    fun `test that getUiStateFlow initialises correctly the state when it is called for the first time`() =
        runTest {
            initTestClass()

            assertThat(underTest.getUiStateFlow(1).value).isEqualTo(TransferImageUiState())
        }

    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and GetThumbnailUseCase when ads a new completed download`() =
        runTest {
            val state = MegaTransfer.STATE_COMPLETED
            val type = MegaTransfer.TYPE_DOWNLOAD

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_COMPLETED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.DOWNLOAD)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verify(getThumbnailUseCase).invoke(nodeHandle, true)
        }

    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and GetThumbnailUseCase when ads a new cancelled download`() =
        runTest {
            val state = MegaTransfer.STATE_CANCELLED
            val type = MegaTransfer.TYPE_DOWNLOAD

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_COMPLETED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.DOWNLOAD)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verify(getThumbnailUseCase).invoke(nodeHandle, true)
        }


    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and GetThumbnailUseCase when ads a new failed download`() =
        runTest {
            val state = MegaTransfer.STATE_FAILED
            val type = MegaTransfer.TYPE_DOWNLOAD

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_COMPLETED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.DOWNLOAD)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verify(getThumbnailUseCase).invoke(nodeHandle, true)
        }

    @Test
    fun `test that addTransfer adds a new completed download transfer to the UI state`() =
        runTest {
            val state = MegaTransfer.STATE_COMPLETED
            val type = MegaTransfer.TYPE_DOWNLOAD
            val file = File("file")

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_COMPLETED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.DOWNLOAD)
            whenever(fileTypeIconMapper(extension)).thenReturn(fileTypeResId)
            whenever(getThumbnailUseCase(nodeHandle, true)).thenReturn(file)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )
            testScheduler.advanceUntilIdle()

            underTest.getUiStateFlow(id).test {
                (this.cancelAndConsumeRemainingEvents().last() as Event.Item).value.also {
                    assertThat(it.fileTypeResId).isEqualTo(fileTypeResId)
                    assertThat(it.previewUri).isEqualTo(file.toUri())
                }
            }
        }

    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and GetThumbnailUseCase when adds a new completed upload`() =
        runTest {
            val state = MegaTransfer.STATE_COMPLETED
            val type = MegaTransfer.TYPE_UPLOAD

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_COMPLETED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.GENERAL_UPLOAD)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verify(getThumbnailUseCase).invoke(nodeHandle, true)
        }

    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and does not invoke GetThumbnailUseCase when adds a new cancelled upload`() =
        runTest {
            val state = MegaTransfer.STATE_CANCELLED
            val type = MegaTransfer.TYPE_UPLOAD

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_CANCELLED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.GENERAL_UPLOAD)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verifyNoInteractions(getThumbnailUseCase)
        }

    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and does not invoke GetThumbnailUseCase when adds a new failed upload`() =
        runTest {
            val state = MegaTransfer.STATE_FAILED
            val type = MegaTransfer.TYPE_UPLOAD

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_FAILED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.GENERAL_UPLOAD)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verifyNoInteractions(getThumbnailUseCase)
        }

    @Test
    fun `test that addTransfer adds a new completed upload transfer to the UI state`() =
        runTest {
            val state = MegaTransfer.STATE_COMPLETED
            val type = MegaTransfer.TYPE_UPLOAD

            whenever(transferStateMapper(state)).thenReturn(TransferState.STATE_COMPLETED)
            whenever(transferTypeMapper(type, emptyList())).thenReturn(TransferType.GENERAL_UPLOAD)
            whenever(fileTypeIconMapper(extension)).thenReturn(fileTypeResId)

            initTestClass()

            underTest.addTransfer(
                getCompletedTransfer(
                    type = type,
                    state = state,
                )
            )
            advanceUntilIdle()

            underTest.getUiStateFlow(id).test {
                (this.cancelAndConsumeRemainingEvents().last() as Event.Item).value.also {
                    assertThat(it.fileTypeResId).isEqualTo(fileTypeResId)
                    assertThat(it.previewUri).isEqualTo(localPath.toUri())
                }
            }
        }

    private fun getCompletedTransfer(type: Int, state: Int) = CompletedTransfer(
        id = id,
        fileName = name,
        type = type,
        state = state,
        size = size,
        handle = nodeHandle,
        path = "",
        isOffline = false,
        timestamp = 0,
        error = "",
        originalPath = localPath,
        parentHandle = -1,
        appData = null,
    )

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}