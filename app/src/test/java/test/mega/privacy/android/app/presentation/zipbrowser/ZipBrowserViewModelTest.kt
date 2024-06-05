package test.mega.privacy.android.app.presentation.zipbrowser

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
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.entity.zipbrowser.ZipTreeNode
import mega.privacy.android.domain.usecase.zipbrowser.GetZipTreeMapUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZipBrowserViewModelTest {
    private lateinit var underTest: ZipBrowserViewModel

    private val getZipTreeMapUseCase = mock<GetZipTreeMapUseCase>()
    private val zipInfoUiEntityMapper = mock<ZipInfoUiEntityMapper>()
    private val savedStateHandle = mock<SavedStateHandle>()

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

    private val testZipFolderEntity = mock<ZipInfoUiEntity>()
    private val testZipFileEntity = mock<ZipInfoUiEntity>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = ZipBrowserViewModel(
            getZipTreeMapUseCase = getZipTreeMapUseCase,
            zipInfoUiEntityMapper = zipInfoUiEntityMapper,
            savedStateHandle = savedStateHandle
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getZipTreeMapUseCase,
            zipInfoUiEntityMapper,
            savedStateHandle
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
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
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
    fun `test that state is updated correctly when openFolder is invoked`() = runTest {
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
        whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
        whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testZipFileEntity)

        initUnderTest()
        underTest.openFolder(folderPath)

        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.items).isNotEmpty()
            assertThat(actual.items.size).isEqualTo(2)
            assertThat(actual.folderDepth).isEqualTo(1)
            assertThat(actual.parentFolderName).isEqualTo(folderPath)
            assertThat(actual.currentZipTreeNode).isEqualTo(testZipTreeNode)
            assertThat(actual.items[0]).isEqualTo(testZipFileEntity)
            assertThat(actual.items[1]).isEqualTo(testZipFolderEntity)
        }
    }

    @Test
    fun `test that state is updated correctly when handleOnBackPressed is invoked`() = runTest {
        whenever(savedStateHandle.get<String>(EXTRA_PATH_ZIP)).thenReturn(testZipFullPath)
        whenever(getZipTreeMapUseCase(anyOrNull())).thenReturn(testZipNodeTree)
        whenever(zipInfoUiEntityMapper(testZipTreeNode)).thenReturn(mock())
        whenever(zipInfoUiEntityMapper(testSubFolderNode)).thenReturn(testZipFolderEntity)
        whenever(zipInfoUiEntityMapper(testSubFileNode)).thenReturn(testZipFileEntity)

        initUnderTest()
        underTest.openFolder(folderPath)
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
}