package test.mega.privacy.android.app.presentation.offline

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.offline.offlinev2.OfflineViewModelV2
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.LoadOfflineNodesUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class OfflineViewModelV2Test {
    private lateinit var underTest: OfflineViewModelV2
    private val loadOfflineNodesUseCase = mock<LoadOfflineNodesUseCase>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = OfflineViewModelV2(
            loadOfflineNodesUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that isLoading should be false when get fetching the offline nodes is successful `() =
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
