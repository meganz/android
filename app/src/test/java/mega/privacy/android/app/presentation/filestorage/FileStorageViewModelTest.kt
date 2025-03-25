package mega.privacy.android.app.presentation.filestorage

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.filestorage.model.FileStorageUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.GetDocumentEntityUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.domain.usecase.file.GetFileStorageTypeNameUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.file.GetFilesInDocumentFolderUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsUriPathInCacheUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileStorageViewModelTest {

    private lateinit var underTest: FileStorageViewModel
    private val getFileStorageTypeNameUseCase = mock<GetFileStorageTypeNameUseCase>()
    private val getPathByDocumentContentUriUseCase = mock<GetPathByDocumentContentUriUseCase>()
    private val getFilesInDocumentFolderUseCase = mock<GetFilesInDocumentFolderUseCase>()
    private val isUriPathInCacheUseCase = mock<IsUriPathInCacheUseCase>()
    private val getDocumentEntityUseCase = mock<GetDocumentEntityUseCase>()
    private val getExternalPathByContentUriUseCase = mock<GetExternalPathByContentUriUseCase>()
    private val getFileUriUseCase = mock<GetFileUriUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = FileStorageViewModel(
            getFileStorageTypeNameUseCase,
            getPathByDocumentContentUriUseCase,
            getFilesInDocumentFolderUseCase,
            isUriPathInCacheUseCase,
            getDocumentEntityUseCase,
            getExternalPathByContentUriUseCase,
            getFileUriUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getFileStorageTypeNameUseCase,
            getPathByDocumentContentUriUseCase,
            getFilesInDocumentFolderUseCase,
            isUriPathInCacheUseCase,
            getDocumentEntityUseCase,
            getFileUriUseCase,
        )
    }

    @Test
    fun `test that storageType is updated correctly when current folder is valid`() = runTest {
        val uriPath = UriPath("foo")
        val storageType = FileStorageType.SdCard
        commonStub(uriPath, storageType = storageType)
        underTest.setRootPath(uriPath, true)
        underTest.uiState.test {
            val state = awaitItem() as? FileStorageUiState.Loaded
            assertThat(state?.storageType).isEqualTo(storageType)
        }
    }

    @Test
    fun `test that storageType is not updated when storage type name is null`() = runTest {
        val uriPath = UriPath("foo")
        commonStub(uriPath)
        underTest.setRootPath(uriPath, true)
        underTest.uiState.test {
            val state = awaitItem() as? FileStorageUiState.Loaded
            assertThat(state?.storageType).isEqualTo(FileStorageType.Unknown)
        }
    }

    @Test
    fun `test that storageType is not updated when update title is false`() = runTest {
        val uriPath = UriPath("foo")
        commonStub(uriPath)
        underTest.setRootPath(uriPath, false)
        underTest.uiState.test {
            val state = awaitItem() as? FileStorageUiState.Loaded
            assertThat(state?.storageType).isEqualTo(FileStorageType.Unknown)
        }
        verifyNoInteractions(getFileStorageTypeNameUseCase)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true])
    fun `test that is in cache folder is updated when root path is set`(
        expected: Boolean,
    ) = runTest {
        val uriPath = UriPath("foo")
        commonStub(uriPath, isInCache = expected)

        underTest.setRootPath(uriPath)

        underTest.uiState.test {
            val state = awaitItem() as? FileStorageUiState.Loaded
            assertThat(state?.isInCacheDirectory).isEqualTo(expected)
        }
    }

    @Test
    fun `test that highlight position is correct`() = runTest {
        val uriPath = UriPath("foo")
        val expected = 6
        val documentEntities = (0..9).map { index ->
            mock<DocumentEntity> {
                on { uri } doReturn UriPath("child$index")
                on { name } doReturn "name$index"
            }
        }

        commonStub(uriPath, children = documentEntities)
        underTest.setRootPath(
            uriPath,
            highlightFileName = documentEntities.getOrNull(expected)?.name
        )
        underTest.uiState.test {
            val state = awaitItem() as? FileStorageUiState.Loaded
            assertThat(state?.getHighlightFilePosition()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that current path is updated when root path is set`() = runTest {
        val uriPath = UriPath("foo")
        commonStub(uriPath)
        underTest.setRootPath(uriPath)
        underTest.uiState.test {
            val state = awaitItem() as? FileStorageUiState.Loaded
            assertThat(state?.currentFolder?.uriPath).isEqualTo(uriPath)
        }
    }

    @Test
    fun `test that current path is updated when root path is set and go to child is invoked`() =
        runTest {
            val root = UriPath("foo")
            val child = UriPath("child")
            commonStub(root)
            commonStub(child)
            underTest.setRootPath(root)
            underTest.goToChild(child)
            underTest.uiState.test {
                val state = awaitItem() as? FileStorageUiState.Loaded
                assertThat(state?.currentFolder?.uriPath).isEqualTo(child)
                assertThat(state?.currentFolder?.parent?.uriPath).isEqualTo(root)
            }
        }

    @Test
    fun `test that current path is updated when root path is set and go to parent is invoked`() =
        runTest {
            val child = UriPath("child")
            val parent = UriPath("parent")
            commonStub(parent)
            commonStub(child)
            underTest.setRootPath(parent)
            underTest.goToChild(child)
            underTest.goToParent()
            underTest.uiState.test {
                val state = awaitItem() as? FileStorageUiState.Loaded
                assertThat(state?.currentFolder?.uriPath).isEqualTo(parent)
            }
        }

    @Test
    fun `test that current folder path string is updated when root path is set`() =
        runTest {
            val uriPath = UriPath("foo")
            val expected = "folder path"
            commonStub(uriPath, pathString = expected)
            underTest.setRootPath(uriPath)

            underTest.uiState.test {
                val state = awaitItem() as? FileStorageUiState.Loaded
                assertThat(state?.currentFolderPath).isEqualTo(expected)
            }
        }

    @Test
    fun `test that children are set`() =
        runTest {
            val uriPath = UriPath("foo")
            val child = "childName.txt"
            val documentEntity = listOf(mock<DocumentEntity> {
                on { uri } doReturn UriPath("child")
                on { name } doReturn child
            })
            commonStub(uriPath, children = documentEntity)
            underTest.setRootPath(uriPath)

            underTest.uiState.test {
                val state = awaitItem() as? FileStorageUiState.Loaded
                assertThat(state?.children?.singleOrNull()?.name).isEqualTo(child)
            }
        }

    @Test
    fun `test that folder picked event with correct path is triggered when folder picked is invoked`() =
        runTest {
            val uriString = "content://"
            val path = "/path"
            whenever(getExternalPathByContentUriUseCase(uriString)) doReturn path

            underTest.folderPicked(uriString)

            underTest.uiState.test {
                val state = awaitItem() as? FileStorageUiState.Loaded
                assertThat(state?.folderPickedEvent).isEqualTo(triggered(UriPath(path)))
            }
        }

    @Test
    fun `test that folder picked event is consumed`() =
        runTest {
            val uriString = "content://"
            whenever(getExternalPathByContentUriUseCase(uriString)) doReturn "/path"

            underTest.folderPicked(uriString)
            underTest.consumeFolderPickedEvent()

            underTest.uiState.test {
                val state = awaitItem() as? FileStorageUiState.Loaded
                assertThat(state?.folderPickedEvent).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that document clicked event is consumed`() =
        runTest {
            val uriString = "content://"
            whenever(getExternalPathByContentUriUseCase(uriString)) doReturn "/path"

            underTest.folderPicked(uriString)
            underTest.consumeFolderPickedEvent()

            underTest.uiState.test {
                val state = awaitItem() as? FileStorageUiState.Loaded
                assertThat(state?.folderPickedEvent).isEqualTo(consumed())
            }
        }

    private suspend fun commonStub(
        uriPath: UriPath,
        children: List<DocumentEntity> = emptyList(),
        isInCache: Boolean = false,
        storageType: FileStorageType? = null,
        pathString: String = uriPath.value,
    ) {
        whenever(getFilesInDocumentFolderUseCase(uriPath)) doReturn DocumentFolder(children)
        whenever(getPathByDocumentContentUriUseCase(uriPath.value)) doReturn pathString
        whenever(isUriPathInCacheUseCase(uriPath)) doReturn isInCache
        whenever(getFileStorageTypeNameUseCase.invoke(uriPath)).thenReturn(storageType)
        whenever(getDocumentEntityUseCase(uriPath)) doReturn DocumentEntity(
            "name",
            4654L,
            0L,
            uriPath,
        )
    }
}