package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset

/**
 * Test class for [DisableCameraUploadsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DisableCameraUploadsUseCaseTest {
    private lateinit var underTest: DisableCameraUploadsUseCase

    private val disableCameraUploadsSettingsUseCase = mock<DisableCameraUploadsSettingsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DisableCameraUploadsUseCase(
            disableCameraUploadsSettingsUseCase = disableCameraUploadsSettingsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            disableCameraUploadsSettingsUseCase,
        )
    }

    @Test
    fun `test that disabling camera uploads in the database are executed by use cases in a specific order`() =
        runTest {
            underTest()

            with(
                inOrder(
                    disableCameraUploadsSettingsUseCase,
                )
            ) {
                verify(disableCameraUploadsSettingsUseCase).invoke()
            }
        }
}
