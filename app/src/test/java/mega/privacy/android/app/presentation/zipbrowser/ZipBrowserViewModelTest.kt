package mega.privacy.android.app.presentation.zipbrowser

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserViewModel
import mega.privacy.android.app.presentation.zipbrowser.mapper.ZipInfoUiEntityMapper
import mega.privacy.android.app.presentation.zipbrowser.model.ZipInfoUiEntity
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.domain.usecase.zipbrowser.GetZipTreeMapUseCase
import mega.privacy.android.domain.usecase.zipbrowser.UnzipFileUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipBrowserViewModelTest {
    @TempDir
    lateinit var temporaryFolder: File

    private lateinit var underTest: ZipBrowserViewModel

    private val getZipTreeMapUseCase = mock<GetZipTreeMapUseCase>()
    private val zipInfoUiEntityMapper = mock<ZipInfoUiEntityMapper>()
    private val unzipFileUseCase = mock<UnzipFileUseCase>()
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getFileTypeInfoUseCase = mock<GetFileTypeInfoUseCase>()

    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val testZipFullPath = "/testZipFullPath.zip"

    private val folderPath = "folder"
    private val subFolderPath = "folder/subFolder"
    private val subFilePath = "folder/file.txt"
    private val testSubFolderNode = mock<ZipTreeNode> {
        on { path }.thenReturn(subFolderPath)
        on { zipEntryType }.thenReturn(ZipEntryType.Folder)
        on { parentPath }.thenReturn(folderPath)
        on { children }.thenReturn(listOf(mock(), mock()))
    }
    private val testSubFileNode = mock<ZipTreeNode> {
        on { path }.thenReturn(subFilePath)
        on { parentPath }.thenReturn(folderPath)
        on { zipEntryType }.thenReturn(ZipEntryType.File)
    }
    private val testZipTreeNode = mock<ZipTreeNode> {
        on { children }.thenReturn(listOf(testSubFileNode, testSubFolderNode))
    }
    private val testZipNodeTree: Map<String, ZipTreeNode> =
        mapOf(
            folderPath to testZipTreeNode,
            subFolderPath to testSubFolderNode,
            subFilePath to testSubFileNode
        )

    private val testZipFolderEntity = mock<ZipInfoUiEntity> {
        on { zipEntryType }.thenReturn(ZipEntryType.Folder)
        on { path }.thenReturn(folderPath)
    }
    private val testFileEntity = mock<ZipInfoUiEntity> {
        on { zipEntryType }.thenReturn(ZipEntryType.File)
        on { path }.thenReturn(subFilePath)
    }
    private val testZipFileEntity = mock<ZipInfoUiEntity> {
        on { zipEntryType }.thenReturn(ZipEntryType.Zip)
        on { name }.thenReturn(".zipFile.txt")
        on { path }.thenReturn(subFilePath)
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = ZipBrowserViewModel(
            getZipTreeMapUseCase = getZipTreeMapUseCase,
            zipInfoUiEntityMapper = zipInfoUiEntityMapper,
            unzipFileUseCase = unzipFileUseCase,
            savedStateHandle = savedStateHandle,
            getFileTypeInfoUseCase = getFileTypeInfoUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getZipTreeMapUseCase,
            zipInfoUiEntityMapper,
            unzipFileUseCase,
            savedStateHandle,
            getFileTypeInfoUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(emptyMap())
        initUnderTest()

        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.items).isEmpty()
            assertThat(initial.folderDepth).isEqualTo(0)
            assertThat(initial.parentFolderName).isEmpty()
            assertThat(initial.currentZipTreeNode).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the state is updated correctly when getting the root zip tree nodes`() =
        runTest {
            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
            whenever(zipInfoUiEntityMapper(anyOrNull())).thenReturn(mock())

            initUnderTest()

            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.items).isNotEmpty()
                assertThat(actual.items.size).isEqualTo(1)
                assertThat(actual.folderDepth).isEqualTo(0)
                assertThat(actual.parentFolderName).isEqualTo("ZIP testZipFullPath")
                assertThat(actual.currentZipTreeNode).isNull()
            }
        }

    @Test
    fun `test that the state is updated correctly when getZipTreeMapUseCase throws an exception`() =
        runTest {
            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenThrow(IllegalArgumentException())

            initUnderTest()

            underTest.uiState.test {
                assertThat(awaitItem().showAlertDialog).isTrue()
            }
        }

    @Test
    fun `test that state is updated correctly when folder item is clicked`() = runTest {
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
        whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
        whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testFileEntity)

        initUnderTest()
        underTest.itemClicked(testZipFolderEntity)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.items).isNotEmpty()
            assertThat(actual.items.size).isEqualTo(2)
            assertThat(actual.folderDepth).isEqualTo(1)
            assertThat(actual.parentFolderName).isEqualTo(folderPath)
            assertThat(actual.currentZipTreeNode).isEqualTo(testZipTreeNode)
            assertThat(actual.items[0]).isEqualTo(testFileEntity)
            assertThat(actual.items[1]).isEqualTo(testZipFolderEntity)
        }
    }

    @Test
    fun `test that shouldShowAlertDialog is true when zip file is not unpack and unzip is failed`() =
        runTest {
            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
            whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
            whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testFileEntity)
            whenever(unzipFileUseCase(anyOrNull(), anyOrNull())).thenReturn(false)

            initUnderTest()
            underTest.itemClicked(testFileEntity)

            underTest.uiState.test {
                assertThat(awaitItem().showAlertDialog).isTrue()
            }
        }

    @Test
    fun `test that shouldShowAlertDialog is true when the zip item does not exist`() =
        runTest {
            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
            whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
            whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testFileEntity)
            whenever(unzipFileUseCase(anyOrNull(), anyOrNull())).thenReturn(false)

            initUnderTest()
            underTest.getUnzipRootPath()?.let { File(it).mkdirs() }
            underTest.itemClicked(testFileEntity)

            underTest.uiState.test {
                assertThat(awaitItem().showAlertDialog).isTrue()
            }
        }

    @Test
    fun `test that shouldShowAlertDialog is true when the zip item is not available for opening`() =
        runTest {
            val zipFile = File(temporaryFolder, "zipFile.txt").apply { createNewFile() }
            File(temporaryFolder, "zipFile").apply { createNewFile() }

            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(zipFile.path)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
            whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
            whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testZipFileEntity)
            whenever(unzipFileUseCase(anyOrNull(), anyOrNull())).thenReturn(false)

            initUnderTest()
            underTest.itemClicked(testZipFileEntity)

            underTest.uiState.test {
                assertThat(awaitItem().showAlertDialog).isTrue()
            }
        }

    @Test
    fun `test that shouldShowAlertDialog is true when the unzipFileUseCase throws an exception`() =
        runTest {
            val zipFile = File(temporaryFolder, "zipFile.txt").apply { createNewFile() }
            File(temporaryFolder, "zipFile").apply { createNewFile() }

            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(zipFile.path)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
            whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
            whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testZipFileEntity)
            whenever(unzipFileUseCase(anyOrNull(), anyOrNull())).thenThrow(NullPointerException())

            initUnderTest()
            underTest.itemClicked(testZipFileEntity)

            underTest.uiState.test {
                assertThat(awaitItem().showAlertDialog).isTrue()
            }
        }

    @Test
    fun `test that state is updated correctly when the zip item is available for opening`() =
        runTest {
            val fileName = "test.txt"
            val file = File(temporaryFolder, fileName).apply { createNewFile() }
            val fileEntry = mock<ZipInfoUiEntity> {
                on { zipEntryType }.thenReturn(ZipEntryType.File)
                on { path }.thenReturn(file.name)
            }

            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(temporaryFolder.path + ".")
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
            whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
            whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(fileEntry)
            whenever(unzipFileUseCase(anyOrNull(), anyOrNull())).thenReturn(true)

            initUnderTest()
            underTest.itemClicked(fileEntry)

            underTest.uiState.test {
                assertThat(awaitItem().openedFile).isEqualTo(fileEntry)
            }
        }

    @Test
    fun `test that state is updated correctly when handleOnBackPressed is invoked`() = runTest {
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
        whenever(zipInfoUiEntityMapper(testZipTreeNode)).thenReturn(mock())
        whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
        whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testFileEntity)

        initUnderTest()
        underTest.itemClicked(testZipFolderEntity)
        underTest.handleOnBackPressed()

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.items).isNotEmpty()
            assertThat(actual.items.size).isEqualTo(1)
            assertThat(actual.folderDepth).isEqualTo(0)
            assertThat(actual.parentFolderName).isEqualTo("ZIP testZipFullPath")
            assertThat(actual.currentZipTreeNode).isNull()
        }
    }

    @Test
    fun `test that unzipRootPath is returned correctly`() = runTest {
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(emptyMap())
        initUnderTest()

        assertThat(underTest.getUnzipRootPath()).isEqualTo(
            "/testZipFullPath/"
        )
    }

    @Test
    fun `test that state is updated correctly when updateShouldShowAlertDialog is invoked`() =
        runTest {
            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(emptyMap())
            initUnderTest()

            underTest.uiState.test {
                assertThat(awaitItem().showAlertDialog).isFalse()
                underTest.updateShowAlertDialog(true)
                assertThat(awaitItem().showAlertDialog).isTrue()
                underTest.updateShowAlertDialog(false)
                assertThat(awaitItem().showAlertDialog).isFalse()
            }
        }

    @Test
    fun `test that state is updated correctly when updateShouldShowSnackBar is invoked`() =
        runTest {
            whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
            whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(emptyMap())
            initUnderTest()

            underTest.uiState.test {
                assertThat(awaitItem().showSnackBar).isFalse()
                underTest.updateShowSnackBar(true)
                assertThat(awaitItem().showSnackBar).isTrue()
                underTest.updateShowSnackBar(false)
                assertThat(awaitItem().showSnackBar).isFalse()
            }
        }

    @Test
    fun `test that getFileTypeInfoUseCase function is invoked and returns as expected`() =
        runTest {
            val mockFile = mock<File>()
            val expectedFileTypeInfo = VideoFileTypeInfo("", "", 10.seconds)
            whenever(getFileTypeInfoUseCase(mockFile)).thenReturn(expectedFileTypeInfo)
            val actual = underTest.getFileTypeInfo(mockFile)
            assertThat(actual is VideoFileTypeInfo).isTrue()
            verify(getFileTypeInfoUseCase).invoke(mockFile)
        }
}