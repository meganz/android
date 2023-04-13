package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Test class for [SetPrimaryFolderInSDCardUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetPrimaryFolderInSDCardUseCaseTest {

    private lateinit var underTest: SetPrimaryFolderInSDCardUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetPrimaryFolderInSDCardUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "is in SD card: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the primary folder in the SD card is handled`(isInSDCard: Boolean) = runTest {
        underTest(isInSDCard)

        verify(cameraUploadRepository, times(1)).setPrimaryFolderInSDCard(isInSDCard)
    }
}