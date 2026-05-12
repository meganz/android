package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.file.CreateTextFileWithContentUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaUiState
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text.ShareTextToMegaViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class ShareTextToMegaViewModelTest {
    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val cacheRepository = mock<CacheRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val createTextFileWithContentUseCase = CreateTextFileWithContentUseCase(
        getCacheFileUseCase = getCacheFileUseCase,
        cacheRepository = cacheRepository,
        fileSystemRepository = fileSystemRepository,
    )

    @AfterEach
    fun resetMocks() {
        reset(
            getRootNodeIdUseCase,
            getCacheFileUseCase,
            cacheRepository,
            fileSystemRepository,
        )
    }

    private fun buildViewModel(): ShareTextToMegaViewModel = ShareTextToMegaViewModel(
        getRootNodeIdUseCase = getRootNodeIdUseCase,
        createTextFileWithContentUseCase = createTextFileWithContentUseCase,
    )

    private suspend fun ReceiveTurbine<ShareTextToMegaUiState>.awaitDataState(): ShareTextToMegaUiState.Data {
        var item = awaitItem()
        while (item !is ShareTextToMegaUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private fun stubCacheFile(fileName: String, file: File) {
        whenever(cacheRepository.getCacheFolderNameForTransfer(false)).thenReturn(CACHE_FOLDER)
        whenever(getCacheFileUseCase(CACHE_FOLDER, fileName)).thenReturn(file)
    }

    @Test
    fun `test that uiState exposes root node id when use case returns a value`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(42L))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitDataState()
            assertThat(data.rootNodeId).isEqualTo(NodeId(42L))
        }
    }

    @Test
    fun `test that uiState initially exposes a consumed fileUri event`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitDataState()
            assertThat(data.fileUri).isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that uiState falls back to invalid root node id when use case returns null`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(null)
            val underTest = buildViewModel()

            underTest.uiState.test {
                val data = awaitDataState()
                assertThat(data.rootNodeId).isEqualTo(NodeId(-1))
            }
        }

    @Test
    fun `test that uiState falls back to invalid root node id when use case throws`() = runTest {
        whenever(getRootNodeIdUseCase()).thenThrow(RuntimeException("boom"))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitDataState()
            assertThat(data.rootNodeId).isEqualTo(NodeId(-1))
        }
    }

    @Test
    fun `test that createTextFile writes content to cache file with the given file name`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            val cacheFile = File("/tmp/shared.txt")
            stubCacheFile("shared.txt", cacheFile)
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitDataState()
                underTest.createTextFile("shared.txt", "hello world")
                awaitDataState()
            }

            verify(fileSystemRepository).writeTextToPath(cacheFile.absolutePath, "hello world")
        }

    @Test
    fun `test that createTextFile triggers fileUri event with the cache file uri`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
        val cacheFile = File("/tmp/shared.txt")
        stubCacheFile("shared.txt", cacheFile)
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitDataState()
            underTest.createTextFile("shared.txt", "hello")
            val updated = awaitDataState()
            assertThat(updated.fileUri).isInstanceOf(StateEventWithContentTriggered::class.java)
            val triggered = updated.fileUri as StateEventWithContentTriggered
            assertThat(triggered.content).isEqualTo(UriPath.fromFile(cacheFile))
        }
    }

    @Test
    fun `test that createTextFile keeps fileUri consumed when cache file cannot be created`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            whenever(cacheRepository.getCacheFolderNameForTransfer(false)).thenReturn(CACHE_FOLDER)
            whenever(getCacheFileUseCase(CACHE_FOLDER, "shared.txt")).thenReturn(null)
            val underTest = buildViewModel()

            underTest.uiState.test {
                val initial = awaitDataState()
                assertThat(initial.fileUri).isInstanceOf(StateEventWithContentConsumed::class.java)
                underTest.createTextFile("shared.txt", "hello")
                expectNoEvents()
            }

            verify(fileSystemRepository, never()).writeTextToPath(any(), any())
        }

    @Test
    fun `test that createTextFile keeps fileUri consumed when write to file throws`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
        val cacheFile = File("/tmp/shared.txt")
        stubCacheFile("shared.txt", cacheFile)
        whenever(fileSystemRepository.writeTextToPath(any(), any()))
            .thenThrow(RuntimeException("boom"))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val initial = awaitDataState()
            assertThat(initial.fileUri).isInstanceOf(StateEventWithContentConsumed::class.java)
            underTest.createTextFile("shared.txt", "hello")
            expectNoEvents()
        }
    }

    @Test
    fun `test that onFileUriConsumed transitions fileUri back to consumed after a triggered event`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            val cacheFile = File("/tmp/shared.txt")
            stubCacheFile("shared.txt", cacheFile)
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitDataState()
                underTest.createTextFile("shared.txt", "hello")
                val triggeredState = awaitDataState()
                assertThat(triggeredState.fileUri)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)

                underTest.onFileUriConsumed()
                val consumedState = awaitDataState()
                assertThat(consumedState.fileUri)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    companion object {
        private const val CACHE_FOLDER = "share-cache"
    }
}
