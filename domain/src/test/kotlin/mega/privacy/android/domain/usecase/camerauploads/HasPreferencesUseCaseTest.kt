package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [HasPreferencesUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HasPreferencesUseCaseTest {
    private lateinit var underTest: HasPreferencesUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HasPreferencesUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that the user preferences exist`() = runTest {
        whenever(cameraUploadsRepository.doPreferencesExist()).thenReturn(true)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that the user preferences do not exist`() = runTest {
        whenever(cameraUploadsRepository.doPreferencesExist()).thenReturn(false)

        assertThat(underTest()).isFalse()
    }
}