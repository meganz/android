package test.mega.privacy.android.app.presentation.offline

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.offline.offlinecompose.OfflineComposeViewModel
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.LoadOfflineNodesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineComposeViewModelTest {
    private lateinit var underTest: OfflineComposeViewModel
    private val loadOfflineNodesUseCase = mock<LoadOfflineNodesUseCase>()
    private val setOfflineWarningMessageVisibilityUseCase =
        mock<SetOfflineWarningMessageVisibilityUseCase>()
    private val monitorOfflineWarningMessageVisibilityUseCase =
        mock<MonitorOfflineWarningMessageVisibilityUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = OfflineComposeViewModel(
            loadOfflineNodesUseCase,
            setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineWarningMessageVisibilityUseCase
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            loadOfflineNodesUseCase,
            setOfflineWarningMessageVisibilityUseCase,
            monitorOfflineWarningMessageVisibilityUseCase
        )
    }

    @Test
    fun `test that dismissOfflineWarning will invoke setOfflineWarningMessageVisibilityUseCase`() {
        runTest {
            val expectedResult = false
            underTest.dismissOfflineWarning()
            verify(setOfflineWarningMessageVisibilityUseCase).invoke(expectedResult)
        }
    }

    @Test
    fun `test that showOfflineWarning equals to the same value that monitorOfflineWarningMessageVisibilityUseCase returns`() =
        runTest {
            val expected = false
            whenever(monitorOfflineWarningMessageVisibilityUseCase()).thenReturn(flowOf(expected))
            underTest.monitorOfflineWarningMessage()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.showOfflineWarning).isEqualTo(expected)
            }
        }

    @Test
    fun `test that isLoading should be false when get fetching the offline nodes is successful`() =
        runTest {
            val path = ""
            val searchQuery = ""
            whenever(loadOfflineNodesUseCase(path, searchQuery)).thenReturn(emptyList())
            underTest.loadOfflineNodes()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.isLoading).isEqualTo(false)
            }
        }

    @Test
    fun `test that isLoading should be false when get fetching the offline nodes returns error`() =
        runTest {
            val path = ""
            val searchQuery = ""
            val fakeErrorCode = Random.nextInt()
            whenever(loadOfflineNodesUseCase(path, searchQuery)).thenAnswer {
                throw MegaException(errorCode = fakeErrorCode, errorString = "")
            }
            underTest.loadOfflineNodes()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.isLoading).isEqualTo(false)
            }
        }

    @Test
    fun `test that offlineNodes List should be null when get fetching the offline nodes returns error`() =
        runTest {
            val path = ""
            val searchQuery = ""
            val fakeErrorCode = Random.nextInt()
            whenever(loadOfflineNodesUseCase(path, searchQuery)).thenAnswer {
                throw MegaException(errorCode = fakeErrorCode, errorString = "")
            }
            underTest.loadOfflineNodes()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.offlineNodes).isEmpty()
            }
        }

    @Test
    fun `test that offlineNodes List should NOT be null when get fetching the offline nodes is successful`() =
        runTest {
            val path = ""
            val searchQuery = ""
            whenever(loadOfflineNodesUseCase(path, searchQuery)).thenReturn(emptyList())
            underTest.loadOfflineNodes()
            underTest.uiState.test {
                val state = awaitItem()
                Truth.assertThat(state.offlineNodes).isNotNull()
            }
        }
}
