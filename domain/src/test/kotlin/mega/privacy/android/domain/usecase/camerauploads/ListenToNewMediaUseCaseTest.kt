package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Test class for [ListenToNewMediaUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListenToNewMediaUseCaseTest {

    private lateinit var underTest: ListenToNewMediaUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ListenToNewMediaUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that listen to new media is invoked`() = runTest {
        underTest()

        verify(cameraUploadRepository, times(1)).listenToNewMedia()
    }
}