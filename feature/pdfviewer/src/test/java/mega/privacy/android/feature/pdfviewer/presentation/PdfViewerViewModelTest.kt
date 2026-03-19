package mega.privacy.android.feature.pdfviewer.presentation

import android.content.Context
import android.content.res.Resources
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.shockwave.pdfium.PdfTextMatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.pdf.GetLastPageViewedInPdfUseCase
import mega.privacy.android.domain.usecase.pdf.SetOrUpdateLastPageViewedInPdfUseCase
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource
import mega.privacy.android.feature.pdfviewer.search.FakePdfSearchEngine
import mega.privacy.android.feature.pdfviewer.search.PdfSearchEngineFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PdfViewerViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }

    private val testDispatcher: TestDispatcher get() = extension.testDispatcher

    private lateinit var underTest: PdfViewerViewModel

    private val getLastPageViewedInPdfUseCase = mock<GetLastPageViewedInPdfUseCase>()
    private val setOrUpdateLastPageViewedInPdfUseCase =
        mock<SetOrUpdateLastPageViewedInPdfUseCase>()
    private val getDataBytesFromUrlUseCase = mock<GetDataBytesFromUrlUseCase>()
    private val context = mock<Context>()

    private val defaultArgs = PdfViewerViewModel.Args(
        nodeHandle = 12345L,
        contentUri = "https://example.com/test/document.pdf",
        isLocalContent = false,
        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        mimeType = "application/pdf",
        title = "Test Document.pdf",
        chatId = null,
        messageId = null,
        shouldStopHttpServer = false,
    )

    @BeforeEach
    fun setUp() = runTest {
        // Reset use case mocks to prevent test pollution
        reset(
            getLastPageViewedInPdfUseCase,
            setOrUpdateLastPageViewedInPdfUseCase,
            getDataBytesFromUrlUseCase
        )

        // Mock ContentResolver so PdfSearchEngine doesn't crash when opening URIs in tests.
        whenever(context.contentResolver).thenReturn(mock())
        // Mock Resources to prevent NPE in PdfiumCore initialization
        val resources = mock<Resources>()
        val displayMetrics = android.util.DisplayMetrics()
        whenever(resources.displayMetrics).thenReturn(displayMetrics)
        whenever(context.resources).thenReturn(resources)
        whenever(getLastPageViewedInPdfUseCase(12345L)).thenReturn(1)
    }

    private fun initViewModel(
        args: PdfViewerViewModel.Args = defaultArgs,
        pdfSearchEngineFactory: PdfSearchEngineFactory = object : PdfSearchEngineFactory {
            override fun create(context: Context) = FakePdfSearchEngine()
        },
    ): PdfViewerViewModel {
        return PdfViewerViewModel(
            args = args,
            context = context,
            pdfSearchEngineFactory = pdfSearchEngineFactory,
            getLastPageViewedInPdfUseCase = getLastPageViewedInPdfUseCase,
            setOrUpdateLastPageViewedInPdfUseCase = setOrUpdateLastPageViewedInPdfUseCase,
            getDataBytesFromUrlUseCase = getDataBytesFromUrlUseCase,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `test that initial state has correct title from args`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.title).isEqualTo("Test Document.pdf")
        }
    }

    @Test
    fun `test that initial state has correct nodeHandle from args`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.nodeHandle).isEqualTo(12345L)
        }
    }

    @Test
    fun `test that toggleToolbarVisibility toggles isToolbarVisible`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.isToolbarVisible).isTrue()

            underTest.toggleToolbarVisibility()
            assertThat(awaitItem().isToolbarVisible).isFalse()

            underTest.toggleToolbarVisibility()
            assertThat(awaitItem().isToolbarVisible).isTrue()
        }
    }

    @Test
    fun `test that hideToolbar sets isToolbarVisible to false`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            // Consume initial state
            awaitItem()

            underTest.hideToolbar()
            assertThat(awaitItem().isToolbarVisible).isFalse()
        }
    }

    @Test
    fun `test that onPageChanged updates currentPage and totalPages`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            // Consume initial state
            awaitItem()

            underTest.onPageChanged(5, 20)
            val state = awaitItem()
            assertThat(state.currentPage).isEqualTo(5)
            assertThat(state.totalPages).isEqualTo(20)
        }
    }

    @Test
    fun `test that onLoadComplete sets loading to false and updates totalPages`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            // Consume initial state
            awaitItem()

            underTest.onLoadComplete(25)
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.totalPages).isEqualTo(25)
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `test that initial state has nodeSourceType from args`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
        }
    }

    @Test
    fun `test that initial state has nodeSourceType FOLDER_LINK when args have FOLDER_LINK`() =
        runTest {
            val folderLinkArgs = defaultArgs.copy(nodeSourceType = NodeSourceType.FOLDER_LINK)
            underTest = initViewModel(args = folderLinkArgs)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.nodeSourceType).isEqualTo(NodeSourceType.FOLDER_LINK)
            }
        }

    @Test
    fun `test that dismissPasswordDialog hides dialog and clears error`() = runTest {
        underTest = initViewModel()
        underTest.dismissPasswordDialog()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.showPasswordDialog).isFalse()
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `test that submitPassword updates currentPassword and hides password dialog`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            // Consume initial state
            awaitItem()

            underTest.submitPassword("test123")
            val state = awaitItem()
            assertThat(state.currentPassword).isEqualTo("test123")
            assertThat(state.showPasswordDialog).isFalse()
        }
    }

    @Test
    fun `test that retryLoad sets loading to true and clears error`() = runTest {
        underTest = initViewModel()

        // Verify initial state has isLoading=true and error=null
        underTest.retryLoad()
        // Since retryLoad() sets values that are already the default, StateFlow won't emit
        // So we verify the state directly
        assertThat(underTest.state.value.isLoading).isTrue()
        assertThat(underTest.state.value.error).isNull()
    }

    @Test
    fun `test that clearError sets error to null`() = runTest {
        underTest = initViewModel()

        // Verify that after clearError(), the error is null
        underTest.clearError()
        // Since error is already null in the default state, StateFlow won't emit
        // So we verify the state directly
        assertThat(underTest.state.value.error).isNull()
    }

    @Test
    fun `test that activateSearch sets isSearchActive to true`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            // Consume initial state
            awaitItem()

            underTest.activateSearch()
            assertThat(awaitItem().searchState.isSearchActive).isTrue()
        }
    }

    @Test
    fun `test that deactivateSearch clears all search state`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            // Consume initial state
            awaitItem()

            underTest.activateSearch()
            // Consume state after activateSearch
            awaitItem()

            underTest.onSearchQueryChanged("test")
            // Consume state after query change
            awaitItem()

            underTest.deactivateSearch()
            val searchState = awaitItem().searchState
            assertThat(searchState.isSearchActive).isFalse()
            assertThat(searchState.query).isEmpty()
            assertThat(searchState.results).isEmpty()
        }
    }

    @Test
    fun `test that onSearchQueryChanged updates query in state`() = runTest {
        underTest = initViewModel()

        underTest.state.test {
            // Consume initial state
            awaitItem()

            underTest.onSearchQueryChanged("hello")
            assertThat(awaitItem().searchState.query).isEqualTo("hello")
        }
    }

    @Test
    fun `test that createSourceFromArgs returns CloudNode with RUBBISH_BIN type when nodeSourceType is RUBBISH_BIN`() =
        runTest {
            val rubbishBinArgs = defaultArgs.copy(
                nodeHandle = 98765L,
                nodeSourceType = NodeSourceType.RUBBISH_BIN,
            )
            underTest = initViewModel(args = rubbishBinArgs)

            underTest.state.test {
                val state = awaitItem()

                assertThat(state.source).isInstanceOf(PdfViewerSource.CloudNode::class.java)
                val source = state.source as PdfViewerSource.CloudNode
                assertThat(source.nodeHandle).isEqualTo(98765L)
                assertThat(source.nodeSourceType).isEqualTo(NodeSourceType.RUBBISH_BIN)
            }
        }

    @Test
    fun `test that createSourceFromArgs returns CloudNode with BACKUPS type when nodeSourceType is BACKUPS`() =
        runTest {
            val backupsArgs = defaultArgs.copy(
                nodeHandle = 54321L,
                nodeSourceType = NodeSourceType.BACKUPS,
            )
            underTest = initViewModel(args = backupsArgs)

            underTest.state.test {
                val state = awaitItem()

                assertThat(state.source).isInstanceOf(PdfViewerSource.CloudNode::class.java)
                val source = state.source as PdfViewerSource.CloudNode
                assertThat(source.nodeHandle).isEqualTo(54321L)
                assertThat(source.nodeSourceType).isEqualTo(NodeSourceType.BACKUPS)
            }
        }

    @Test
    fun `test that search pipeline updates state with results when engine returns matches`() =
        runTest {
            val fakeEngine = FakePdfSearchEngine()
            val mockMatch = mock<PdfTextMatch>()
            fakeEngine.searchResults = listOf(mockMatch)

            whenever(getDataBytesFromUrlUseCase(any())).thenReturn(ByteArray(1))

            underTest = initViewModel(
                pdfSearchEngineFactory = object : PdfSearchEngineFactory {
                    override fun create(context: Context) = fakeEngine
                },
            )

            // Wait for initialization - now that ioDispatcher is the test dispatcher, advanceUntilIdle works
            advanceUntilIdle()

            // Verify search engine is ready
            assertThat(fakeEngine.isOpen).isTrue()

            underTest.activateSearch()
            underTest.onSearchQueryChanged("abc")
            // Wait for debounce (300ms) + processing
            advanceTimeBy(400)
            runCurrent()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.searchState.results).hasSize(1)
                assertThat(state.searchState.isSearching).isFalse()
            }
        }
}
