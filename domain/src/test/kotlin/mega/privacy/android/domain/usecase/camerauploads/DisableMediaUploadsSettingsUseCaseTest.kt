package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [DisableMediaUploadsSettingsUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DisableMediaUploadsSettingsUseCaseTest {
    private lateinit var underTest: DisableMediaUploadsSettingsUseCase

    private val setupMediaUploadsSettingUseCase = mock<SetupMediaUploadsSettingUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DisableMediaUploadsSettingsUseCase(setupMediaUploadsSettingUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(setupMediaUploadsSettingUseCase)
    }

    @Test
    fun `test that media uploads is disabled`() = runTest {
        underTest()

        verify(setupMediaUploadsSettingUseCase).invoke(isEnabled = false)
    }
}