package mega.privacy.android.app.presentation.transfers.model.image

import androidx.core.net.toUri
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.model.image.InProgressTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.TransferImageUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.icon.pack.R
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
class InProgressTransferImageViewModelTest {

    private lateinit var underTest: InProgressTransferImageViewModel

    private val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()

    private val extension = "txt"
    private val name = "name.$extension"
    private val localPath = "localPath/$name"
    private val tag = 1
    private val nodeHandle = 1L
    private val fileTypeResId = R.drawable.ic_text_medium_solid

    @BeforeEach
    fun resetMocks() {
        reset(getThumbnailUseCase, fileTypeIconMapper)
    }

    private fun initTestClass() {
        underTest = InProgressTransferImageViewModel(
            getThumbnailUseCase = getThumbnailUseCase,
            fileTypeIconMapper = fileTypeIconMapper,
        )
    }

    @Test
    fun `test that getUiStateFlow initialises correctly the state when it is called for the first time`() =
        runTest {
            initTestClass()

            assertThat(underTest.getUiStateFlow(1).value).isEqualTo(TransferImageUiState())
        }

    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and GetThumbnailUseCase when ads a new in progress download`() =
        runTest {
            initTestClass()

            underTest.addTransfer(
                InProgressTransfer.Download(
                    tag = tag,
                    totalBytes = 100,
                    isPaused = false,
                    fileName = name,
                    speed = 100,
                    state = TransferState.STATE_ACTIVE,
                    priority = 1.toBigInteger(),
                    progress = Progress(0.5f),
                    nodeId = NodeId(nodeHandle),
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verify(getThumbnailUseCase).invoke(nodeHandle, true)
        }

    @Test
    fun `test that addTransfer adds a new in progress download transfer to the UI state`() =
        runTest {
            val file = File("file")

            whenever(fileTypeIconMapper(extension)).thenReturn(fileTypeResId)
            whenever(getThumbnailUseCase(nodeHandle, true)).thenReturn(file)

            initTestClass()

            underTest.addTransfer(
                InProgressTransfer.Download(
                    tag = tag,
                    totalBytes = 100,
                    isPaused = false,
                    fileName = name,
                    speed = 100,
                    state = TransferState.STATE_ACTIVE,
                    priority = 1.toBigInteger(),
                    progress = Progress(0.5f),
                    nodeId = NodeId(nodeHandle),
                )
            )
            testScheduler.advanceUntilIdle()

            underTest.getUiStateFlow(tag).test {
                (this.cancelAndConsumeRemainingEvents().last() as Event.Item).value.also {
                    assertThat(it.fileTypeResId).isEqualTo(fileTypeResId)
                    assertThat(it.previewUri).isEqualTo(file.toUri())
                }
            }
        }

    @Test
    fun `test that addTransfer invokes FileTypeIconMapper and does not invoke GetThumbnailUseCase when adds a new in progress upload`() =
        runTest {
            initTestClass()

            underTest.addTransfer(
                InProgressTransfer.Upload(
                    tag = tag,
                    totalBytes = 100,
                    isPaused = false,
                    fileName = name,
                    speed = 100,
                    state = TransferState.STATE_ACTIVE,
                    priority = 1.toBigInteger(),
                    progress = Progress(0.5f),
                    localPath = localPath,
                )
            )

            verify(fileTypeIconMapper).invoke(extension)
            verifyNoInteractions(getThumbnailUseCase)
        }

    @Test
    fun `test that addTransfer adds a new in progress upload transfer to the UI state`() =
        runTest {
            whenever(fileTypeIconMapper(extension)).thenReturn(fileTypeResId)

            initTestClass()

            underTest.addTransfer(
                InProgressTransfer.Upload(
                    tag = tag,
                    totalBytes = 100,
                    isPaused = false,
                    fileName = name,
                    speed = 100,
                    state = TransferState.STATE_ACTIVE,
                    priority = 1.toBigInteger(),
                    progress = Progress(0.5f),
                    localPath = localPath,
                )
            )
            advanceUntilIdle()

            underTest.getUiStateFlow(tag).test {
                (this.cancelAndConsumeRemainingEvents().last() as Event.Item).value.also {
                    assertThat(it.fileTypeResId).isEqualTo(fileTypeResId)
                    assertThat(it.previewUri).isEqualTo(localPath.toUri())
                }
            }
        }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}