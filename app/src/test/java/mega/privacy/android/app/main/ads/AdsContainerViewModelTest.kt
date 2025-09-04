package mega.privacy.android.app.main.ads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.advertisements.SetAdsClosingTimestampUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdsContainerViewModelTest {

    private lateinit var viewModel: AdsContainerViewModel
    private val setAdsClosingTimestampUseCase: SetAdsClosingTimestampUseCase = mock()

    @BeforeEach
    fun reset() {
        reset(setAdsClosingTimestampUseCase)
    }

    private fun initViewModel() {
        viewModel = AdsContainerViewModel(setAdsClosingTimestampUseCase)
    }

    @Test
    fun `initial state of isAdsLoaded should be null`() = runTest {
        initViewModel()

        viewModel.isAdsLoaded.test {
            val item = awaitItem()
            assertThat(item).isNull()
        }
    }

    @Test
    fun `setAdsLoaded with true should set isAdsLoaded to true`() = runTest {
        initViewModel()

        viewModel.setAdsLoaded(true)

        viewModel.isAdsLoaded.test {
            val item = awaitItem()
            assertThat(item).isTrue()
        }
    }

    @Test
    fun `setAdsLoaded with false when initial state is null should set isAdsLoaded to false`() =
        runTest {
            initViewModel()

            viewModel.setAdsLoaded(false)

            viewModel.isAdsLoaded.test {
                val item = awaitItem()
                assertThat(item).isFalse()
            }
        }

    @Test
    fun `setAdsLoaded with false when isAdsLoaded is already true should keep it true`() = runTest {
        initViewModel()

        // First set to true
        viewModel.setAdsLoaded(true)
        // Then try to set to false
        viewModel.setAdsLoaded(false)

        viewModel.isAdsLoaded.test {
            val item = awaitItem()
            assertThat(item).isTrue()
        }
    }

    @Test
    fun `isAdsLoaded function should return true when state is true`() = runTest {
        initViewModel()

        viewModel.setAdsLoaded(true)

        assertThat(viewModel.isAdsLoaded()).isTrue()
    }

    @Test
    fun `isAdsLoaded function should return false when state is false`() = runTest {
        initViewModel()

        viewModel.setAdsLoaded(false)

        assertThat(viewModel.isAdsLoaded()).isFalse()
    }

    @Test
    fun `isAdsLoaded function should return false when state is null`() = runTest {
        initViewModel()

        assertThat(viewModel.isAdsLoaded()).isFalse()
    }

    @Test
    fun `handleAdsClosed should call setAdsClosingTimestampUseCase with current timestamp`() =
        runTest {
            initViewModel()

            viewModel.handleAdsClosed()

            verify(setAdsClosingTimestampUseCase).invoke(any())
        }

    @Test
    fun `handleAdsClosed should handle exceptions gracefully`() = runTest {
        val exception = RuntimeException("Error setting timestamp")
        whenever(setAdsClosingTimestampUseCase.invoke(any())).thenThrow(exception)
        initViewModel()

        viewModel.handleAdsClosed()

        verify(setAdsClosingTimestampUseCase).invoke(any())
    }
}