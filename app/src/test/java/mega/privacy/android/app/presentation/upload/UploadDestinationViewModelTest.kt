package mega.privacy.android.app.presentation.upload

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromContentUri
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadDestinationViewModelTest {


    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getFileNameFromContentUri = mock<GetFileNameFromContentUri>()
    private val importFilesErrorMessageMapper = mock<ImportFilesErrorMessageMapper>()
    private val importFileErrorMessageMapper = mock<ImportFileErrorMessageMapper>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private lateinit var viewModel: UploadDestinationViewModel

    @BeforeEach
    fun setup() {
        reset(
            getFeatureFlagValueUseCase,
            getFileNameFromContentUri,
            importFilesErrorMessageMapper,
            importFileErrorMessageMapper,
            fileTypeIconMapper,
        )
        viewModel = UploadDestinationViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getFileNameFromContentUri = getFileNameFromContentUri,
            importFilesErrorMessageMapper = importFilesErrorMessageMapper,
            importFileErrorMessageMapper = importFileErrorMessageMapper,
            fileTypeIconMapper = fileTypeIconMapper,
        )
    }

    @Test
    fun `test that uiState is initialized with default values`() = runTest {
        val uiState = viewModel.uiState.value
        assertTrue(uiState.importUiItems.isEmpty())
    }

    @Test
    fun `test that updateUri updates the list of Uri of the files to upload`() = runTest {
        val uri1 = mock<Uri> {
            on { toString() } doReturn "path1"
        }
        val uri2 = mock<Uri> {
            on { toString() } doReturn "path2"
        }
        whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
            .thenReturn(true)
        val fileUriList = listOf(uri1, uri2)
        whenever(getFileNameFromContentUri.invoke("path1")).thenReturn("file1")
        whenever(getFileNameFromContentUri.invoke("path2")).thenReturn("file2")
        viewModel.updateUri(fileUriList)
        val uiState = viewModel.uiState.value
        assertThat(uiState.importUiItems.size).isEqualTo(2)
    }

    @Test
    fun `test that isValidNameForUpload updates the uiState with the importUiItems`() = runTest {
        val uri1 = mock<Uri> {
            on { toString() } doReturn "path1"
        }
        val uri2 = mock<Uri> {
            on { toString() } doReturn "path2"
        }
        whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
            .thenReturn(true)
        whenever(getFileNameFromContentUri.invoke("path1")).thenReturn("file1")
        whenever(getFileNameFromContentUri.invoke("path2")).thenReturn("file2")
        whenever(importFileErrorMessageMapper("file1")).thenReturn("")
        whenever(importFileErrorMessageMapper("file2")).thenReturn("")
        val fileUriList = listOf(uri1, uri2)
        viewModel.updateUri(fileUriList)
        viewModel.isValidNameForUpload()
        val uiState = viewModel.uiState.value
        assertThat(uiState.nameValidationError).isInstanceOf(StateEventWithContentConsumed::class.java)
    }

    @Test
    fun `test that isValidNameForUpload updates the uiState with the nameValidationError when the file name is empty`() =
        runTest {
            val uri1 = mock<Uri> {
                on { toString() } doReturn ""
            }
            val uri2 = mock<Uri> {
                on { toString() } doReturn "file2"
            }
            whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
                .thenReturn(true)
            whenever(importFileErrorMessageMapper("")).thenReturn("Invalid name")
            whenever(importFileErrorMessageMapper("file2")).thenReturn("")
            val fileUriList = listOf(uri1, uri2)
            viewModel.updateUri(fileUriList)
            viewModel.isValidNameForUpload()
            val uiState = viewModel.uiState.value
            assertThat(uiState.nameValidationError)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
        }

    @Test
    fun `test that isValidNameForUpload updates the uiState with the nameValidationError when the file name is invalid`() =
        runTest {
            val uri1 = mock<Uri> {
                on { toString() } doReturn "ab/cs"
            }
            val uri2 = mock<Uri> {
                on { toString() } doReturn "file2"
            }
            whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
                .thenReturn(true)
            whenever(getFileNameFromContentUri.invoke("ab/cs")).thenReturn("ab/cs")
            whenever(importFileErrorMessageMapper("")).thenReturn("Invalid characters")
            whenever(importFileErrorMessageMapper("file2")).thenReturn("")
            val fileUriList = listOf(uri1, uri2)
            viewModel.updateUri(fileUriList)
            viewModel.isValidNameForUpload()
            assertThat(viewModel.uiState.value.nameValidationError)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
            viewModel.consumeNameValidationError()
            assertThat(viewModel.uiState.value.nameValidationError)
                .isInstanceOf(StateEventWithContentConsumed::class.java)
        }

    @Test
    fun `test that ui state is updated with latest import ui item when edit file name is called`() =
        runTest {
            assertThat(viewModel.uiState.value.editableFile).isNull()
            viewModel.editFileName(
                ImportUiItem(
                    originalFileName = "file1",
                    filePath = "path1",
                    fileName = "file1"
                )
            )
            assertThat(viewModel.uiState.value.editableFile?.originalFileName).isEqualTo("file1")
        }

    @Test
    fun `test that ui state is updated with latest import ui item when update file name is called`() =
        runTest {
            val uri1 = mock<Uri> {
                on { toString() } doReturn "path1"
            }
            val uri2 = mock<Uri> {
                on { toString() } doReturn "path2"
            }
            whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
                .thenReturn(true)
            whenever(getFileNameFromContentUri.invoke("path1")).thenReturn("file1")
            whenever(importFileErrorMessageMapper("")).thenReturn("Invalid characters")
            whenever(importFileErrorMessageMapper("file2")).thenReturn("")
            val fileUriList = listOf(uri1, uri2)
            viewModel.updateUri(fileUriList)
            val importUiItem = ImportUiItem(
                originalFileName = "file1",
                filePath = "path1",
                fileIcon = 0,
                fileName = "file1"
            )
            viewModel.editFileName(importUiItem)
            viewModel.updateFileName("fileNew")
            assertThat(viewModel.uiState.value.importUiItems.first().fileName).isEqualTo("fileNew")
        }

}