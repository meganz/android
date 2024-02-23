package test.mega.privacy.android.app.presentation.settings.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.settings.camerauploads.SettingsCameraUploadsViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [SettingsCameraUploadsViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettingsCameraUploadsViewModelTest {

    private lateinit var underTest: SettingsCameraUploadsViewModel

    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(isCameraUploadsEnabledUseCase)
    }

    /**
     * Creates a new instance of [underTest]
     */
    private suspend fun initializeUnderTest() {
        stubMocks()
        underTest = SettingsCameraUploadsViewModel(
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
        )
    }

    /**
     * Configures some Use Cases with specific values when initializing [underTest]
     */
    private suspend fun stubMocks() {
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
    }

    /**
     * Test Group that verifies the initialization of [underTest]
     */
    @Nested
    @DisplayName("Initialization")
    internal inner class Initialization {

        @Test
        fun `test that the initial state is returned`() = runTest {
            initializeUnderTest()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isCameraUploadsEnabled).isTrue()
            }
        }
    }
}