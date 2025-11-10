@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.myAccount

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Test class for [MyAccountUsageComposeViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MyAccountUsageComposeViewModelTest {

    private lateinit var underTest: MyAccountUsageComposeViewModel

    @BeforeEach
    fun setUp() {
        initializeViewModel()
    }

    private fun initializeViewModel() {
        underTest = MyAccountUsageComposeViewModel()
    }

    @Test
    fun `test that initial state has isLoading set to false`() = runTest {
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that initial state matches default MyAccountUsageComposeUiState`() = runTest {
        val expectedState = MyAccountUsageComposeUiState()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(expectedState)
        }
    }
}

