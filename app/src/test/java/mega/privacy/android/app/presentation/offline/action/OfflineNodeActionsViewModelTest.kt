package mega.privacy.android.app.presentation.offline.action


import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.usecase.node.ExportNodesUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFilesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
class OfflineNodeActionsViewModelTest {
    private val getOfflineFilesUseCase: GetOfflineFilesUseCase = mock()
    private val exportNodesUseCase: ExportNodesUseCase = mock()
    private val snackBarHandler: SnackBarHandler = mock()
    private lateinit var underTest: OfflineNodeActionsViewModel

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        initViewModel()
    }

    private fun initViewModel() {
        underTest = OfflineNodeActionsViewModel(
            getOfflineFilesUseCase = getOfflineFilesUseCase,
            exportNodesUseCase = exportNodesUseCase,
            snackBarHandler = snackBarHandler
        )
    }

    @Test
    fun `test that handleShareOfflineNodes emits shareFilesEvent when all nodes are files`() =
        runTest {
            val file = mock<File>()
            val offlineFileInformation: OfflineFileInformation = mock()
            whenever(offlineFileInformation.isFolder).thenReturn(false)
            whenever(getOfflineFilesUseCase(listOf(offlineFileInformation))).thenReturn(listOf(file))

            underTest.handleShareOfflineNodes(listOf(offlineFileInformation), true)
            verify(getOfflineFilesUseCase).invoke(any())

            underTest.uiState.test {
                val res = awaitItem()
                assertThat(res.shareFilesEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.shareFilesEvent as StateEventWithContentTriggered).content)
                    .containsExactly(file)
            }
        }

    @Test
    fun `test that handleShareOfflineNodes calls exportNodesUseCase when a node is folder and online`() =
        runTest {
            val offlineFileInformation: OfflineFileInformation = mock()
            whenever(offlineFileInformation.isFolder).thenReturn(true)
            whenever(offlineFileInformation.handle).thenReturn("123")
            underTest.handleShareOfflineNodes(listOf(offlineFileInformation), true)
            verify(exportNodesUseCase).invoke(any())
        }

    @Test
    fun `test that sharesNodeLinksEvent is sent with correct title and formatted links when node is folder`() =
        runTest {
            val offlineFileInformation = mock<OfflineFileInformation> {
                on { name } doReturn "name1"
                on { isFolder } doReturn true
                on { handle } doReturn "123"
            }
            val offlineFileInformation2: OfflineFileInformation = mock<OfflineFileInformation> {
                on { name } doReturn "name2"
                on { isFolder } doReturn true
                on { handle } doReturn "456"
            }
            whenever(exportNodesUseCase(listOf(123, 456))).thenReturn(
                mapOf(
                    123L to "link1",
                    456L to "link2"
                )
            )
            underTest.handleShareOfflineNodes(
                listOf(
                    offlineFileInformation,
                    offlineFileInformation2
                ), true
            )

            underTest.uiState.test {
                val res = awaitItem()
                assertThat(res.sharesNodeLinksEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((res.sharesNodeLinksEvent as StateEventWithContentTriggered).content)
                    .isEqualTo("name1" to "link1\n\nlink2")
            }
        }

    @Test
    fun `test that handleShareOfflineNodes post snackbar error message when offline and node is a folder`() =
        runTest {
            val offlineFileInformation: OfflineFileInformation = mock()
            whenever(offlineFileInformation.isFolder).thenReturn(true)
            whenever(offlineFileInformation.handle).thenReturn("123")
            underTest.handleShareOfflineNodes(listOf(offlineFileInformation), false)
            verify(snackBarHandler).postSnackbarMessage(R.string.error_server_connection_problem)
        }

    private suspend fun stubCommon() {
        whenever(getOfflineFilesUseCase(any())).thenReturn(listOf())
        whenever(exportNodesUseCase(any())).thenReturn(mapOf())
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getOfflineFilesUseCase,
            exportNodesUseCase,
            snackBarHandler
        )
    }
}