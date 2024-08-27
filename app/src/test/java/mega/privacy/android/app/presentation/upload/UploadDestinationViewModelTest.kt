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
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UploadDestinationViewModelTest {


    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getFileForUploadUseCase = mock<GetFileForUploadUseCase>()
    private val importFilesErrorMessageMapper = mock<ImportFilesErrorMessageMapper>()
    private val importFileErrorMessageMapper = mock<ImportFileErrorMessageMapper>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private lateinit var viewModel: UploadDestinationViewModel

    @BeforeEach
    fun setup() {
        reset(
            getFileForUploadUseCase,
            getFileForUploadUseCase,
            importFilesErrorMessageMapper,
            importFileErrorMessageMapper,
            fileTypeIconMapper,
        )
        viewModel = UploadDestinationViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getFileForUploadUseCase = getFileForUploadUseCase,
            importFilesErrorMessageMapper = importFilesErrorMessageMapper,
            importFileErrorMessageMapper = importFileErrorMessageMapper,
            fileTypeIconMapper = fileTypeIconMapper,
        )
    }

    @Test
    fun `test that uiState is initialized with default values`() = runTest {
        val uiState = viewModel.uiState.value
        assertTrue(uiState.fileUriList.isEmpty())
        assertTrue(uiState.importUiItems.isEmpty())
    }

    @Test
    fun `test that updateUri updates the list of Uri of the files to upload`() = runTest {
        whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
            .thenReturn(true)
        val fileUriList = listOf(Uri.parse("file1"), Uri.parse("file2"))
        viewModel.updateUri(fileUriList)
        val uiState = viewModel.uiState.value
        assertTrue(uiState.fileUriList == fileUriList)
    }

    @Test
    fun `test that confirmImport updates the uiState with the importUiItems`() = runTest {
        val file1 = mock<File> {
            on { name } doReturn "file1"
            on { path } doReturn "path1"
        }
        val file2 = mock<File> {
            on { name } doReturn "file2"
            on { path } doReturn "path2"
        }
        val uri1 = mock<Uri> {
            on { toString() } doReturn "file1"
        }
        val uri2 = mock<Uri> {
            on { toString() } doReturn "file2"
        }
        whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
            .thenReturn(true)
        whenever(getFileForUploadUseCase.invoke("file1", false))
            .thenReturn(file1)
        whenever(importFileErrorMessageMapper("file1")).thenReturn("")
        whenever(importFileErrorMessageMapper("file2")).thenReturn("")
        whenever(getFileForUploadUseCase("file2", false)).thenReturn(file2)
        val fileUriList = listOf(uri1, uri2)
        viewModel.updateUri(fileUriList)
        viewModel.confirmImport()
        val uiState = viewModel.uiState.value
        assertThat(uiState.navigateToUpload)
            .isInstanceOf(StateEventWithContentTriggered::class.java)
    }

    @Test
    fun `test that confirmImport updates the uiState with the nameValidationError when the file name is empty`() =
        runTest {
            val file1 = mock<File> {
                on { name } doReturn ""
                on { path } doReturn "path1"
            }
            val file2 = mock<File> {
                on { name } doReturn "file2"
                on { path } doReturn "path2"
            }
            val uri1 = mock<Uri> {
                on { toString() } doReturn ""
            }
            val uri2 = mock<Uri> {
                on { toString() } doReturn "file2"
            }
            whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
                .thenReturn(true)
            whenever(getFileForUploadUseCase.invoke("", false))
                .thenReturn(file1)
            whenever(importFileErrorMessageMapper("")).thenReturn("Invalid name")
            whenever(importFileErrorMessageMapper("file2")).thenReturn("")
            whenever(getFileForUploadUseCase("file2", false)).thenReturn(file2)
            val fileUriList = listOf(uri1, uri2)
            viewModel.updateUri(fileUriList)
            viewModel.confirmImport()
            val uiState = viewModel.uiState.value
            assertThat(uiState.nameValidationError)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
        }

    @Test
    fun `test that confirmImport updates the uiState with the nameValidationError when the file name is invalid`() =
        runTest {
            val file1 = mock<File> {
                on { name } doReturn "ab/cs"
                on { path } doReturn "path1"
            }
            val file2 = mock<File> {
                on { name } doReturn "file2"
                on { path } doReturn "path2"
            }
            val uri1 = mock<Uri> {
                on { toString() } doReturn "ab/cs"
            }
            val uri2 = mock<Uri> {
                on { toString() } doReturn "file2"
            }
            whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
                .thenReturn(true)
            whenever(getFileForUploadUseCase.invoke("ab/cs", false))
                .thenReturn(file1)
            whenever(importFileErrorMessageMapper("")).thenReturn("Invalid characters")
            whenever(importFileErrorMessageMapper("file2")).thenReturn("")
            whenever(getFileForUploadUseCase("file2", false)).thenReturn(file2)
            val fileUriList = listOf(uri1, uri2)
            viewModel.updateUri(fileUriList)
            viewModel.confirmImport()
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
            viewModel.editFileName(ImportUiItem(fileName = "file1", filePath = "path1"))
            assertThat(viewModel.uiState.value.editableFile?.fileName).isEqualTo("file1")
        }

    @Test
    fun `test that ui state is updated with latest import ui item when update file name is called`() =
        runTest {
            val file1 = mock<File> {
                on { name } doReturn "file1"
                on { path } doReturn "path1"
            }
            val file2 = mock<File> {
                on { name } doReturn "file2"
                on { path } doReturn "path2"
            }
            val uri1 = mock<Uri> {
                on { toString() } doReturn "file1"
            }
            val uri2 = mock<Uri> {
                on { toString() } doReturn "file2"
            }
            whenever(getFeatureFlagValueUseCase.invoke(AppFeatures.NewUploadDestinationActivity))
                .thenReturn(true)
            whenever(getFileForUploadUseCase.invoke("file1", false))
                .thenReturn(file1)
            whenever(importFileErrorMessageMapper("")).thenReturn("Invalid characters")
            whenever(importFileErrorMessageMapper("file2")).thenReturn("")
            whenever(getFileForUploadUseCase("file2", false)).thenReturn(file2)
            val fileUriList = listOf(uri1, uri2)
            viewModel.updateUri(fileUriList)
            val importUiItem = ImportUiItem(fileName = "file1", filePath = "path1")
            viewModel.editFileName(importUiItem)
            viewModel.updateFileName("fileNew")
            assertThat(viewModel.uiState.value.importUiItems.first().fileName).isEqualTo("fileNew")
        }

}