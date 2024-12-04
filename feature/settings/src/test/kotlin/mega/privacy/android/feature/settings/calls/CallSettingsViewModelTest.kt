package mega.privacy.android.feature.settings.calls

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallSettingsViewModelTest {
    private lateinit var underTest: CallSettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        underTest = CallSettingsViewModel()
    }

    @BeforeEach
    fun cleanUp() {
        //reset mocks
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that ui state is updated on start`() = runTest {
        underTest.uiState.test {
            //should invoke the mock
            assertThat(awaitItem().isSoundNotificationActive).isTrue()
        }
    }

    @Test
    fun `test that ui state is updated when setSoundNotification is invoked`() = runTest {
        //use cases should be tested here once created
        underTest.uiState.test {
            awaitItem()//initial value
            underTest.setSoundNotification(false)
            assertThat(awaitItem().isSoundNotificationActive).isFalse()
            underTest.setSoundNotification(true)
            assertThat(awaitItem().isSoundNotificationActive).isTrue()
        }
    }
}