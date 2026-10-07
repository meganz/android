package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.file.CreateTextFileWithContentUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
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

    private suspend fun ReceiveTurbine<ShareTextToMegaUiState>.awaitSettledDataState(): ShareTextToMegaUiState.Data {
        var item = awaitDataState()
        while (item.isCreatingFile) {
            item = awaitDataState()
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
    fun `test that uiState initially exposes no text file and no failure`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
        val underTest = buildViewModel()

        underTest.uiState.test {
            val data = awaitDataState()
            assertThat(data.textFile).isInstanceOf(StateEventWithContentConsumed::class.java)
            assertThat(data.isCreatingFile).isFalse()
            assertThat(data.createFileFailedEvent).isEqualTo(consumed)
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
    fun `test that createTextFile triggers textFile event with the created file`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
        val cacheFile = File("/tmp/shared.txt")
        stubCacheFile("shared.txt", cacheFile)
        val underTest = buildViewModel()

        underTest.uiState.test {
            awaitDataState()
            underTest.createTextFile("shared.txt", "hello")
            val updated = awaitSettledDataState()
            assertThat(updated.isCreatingFile).isFalse()
            val triggered = updated.textFile as StateEventWithContentTriggered
            assertThat(triggered.content).isEqualTo(
                DocumentEntity(
                    name = "shared.txt",
                    size = cacheFile.length(),
                    lastModified = cacheFile.lastModified(),
                    uri = UriPath.fromFile(cacheFile),
                )
            )
        }
    }

    @Test
    fun `test that createTextFile sets isCreatingFile while the file is being written`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            stubCacheFile("shared.txt", File("/tmp/shared.txt"))
            val gate = CompletableDeferred<Unit>()
            whenever { fileSystemRepository.writeTextToPath(any(), any()) } doSuspendableAnswer {
                gate.await()
            }
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitDataState()
                underTest.createTextFile("shared.txt", "hello")
                assertThat(awaitDataState().isCreatingFile).isTrue()

                gate.complete(Unit)
                assertThat(awaitDataState().isCreatingFile).isFalse()
            }
        }

    @Test
    fun `test that createTextFile triggers createFileFailedEvent when cache file cannot be created`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            whenever(cacheRepository.getCacheFolderNameForTransfer(false)).thenReturn(CACHE_FOLDER)
            whenever(getCacheFileUseCase(CACHE_FOLDER, "shared.txt")).thenReturn(null)
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitDataState()
                underTest.createTextFile("shared.txt", "hello")
                val updated = awaitSettledDataState()
                assertThat(updated.createFileFailedEvent).isEqualTo(triggered)
                assertThat(updated.textFile).isInstanceOf(StateEventWithContentConsumed::class.java)
                assertThat(updated.isCreatingFile).isFalse()
            }

            verify(fileSystemRepository, never()).writeTextToPath(any(), any())
        }

    @Test
    fun `test that createTextFile triggers createFileFailedEvent when write to file throws`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            stubCacheFile("shared.txt", File("/tmp/shared.txt"))
            whenever(fileSystemRepository.writeTextToPath(any(), any()))
                .thenThrow(RuntimeException("boom"))
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitDataState()
                underTest.createTextFile("shared.txt", "hello")
                val updated = awaitSettledDataState()
                assertThat(updated.createFileFailedEvent).isEqualTo(triggered)
                assertThat(updated.textFile).isInstanceOf(StateEventWithContentConsumed::class.java)
                assertThat(updated.isCreatingFile).isFalse()
            }
        }

    @Test
    fun `test that onTextFileConsumed transitions textFile back to consumed after a triggered event`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            stubCacheFile("shared.txt", File("/tmp/shared.txt"))
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitDataState()
                underTest.createTextFile("shared.txt", "hello")
                assertThat(awaitSettledDataState().textFile)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)

                underTest.onTextFileConsumed()
                assertThat(awaitDataState().textFile)
                    .isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    fun `test that onCreateFileFailedEventConsumed transitions the failure event back to consumed`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(NodeId(1L))
            whenever(cacheRepository.getCacheFolderNameForTransfer(false)).thenReturn(CACHE_FOLDER)
            whenever(getCacheFileUseCase(CACHE_FOLDER, "shared.txt")).thenReturn(null)
            val underTest = buildViewModel()

            underTest.uiState.test {
                awaitDataState()
                underTest.createTextFile("shared.txt", "hello")
                assertThat(awaitSettledDataState().createFileFailedEvent).isEqualTo(triggered)

                underTest.onCreateFileFailedEventConsumed()
                assertThat(awaitDataState().createFileFailedEvent).isEqualTo(consumed)
            }
        }

    companion object {
        private const val CACHE_FOLDER = "share-cache"
    }
}
