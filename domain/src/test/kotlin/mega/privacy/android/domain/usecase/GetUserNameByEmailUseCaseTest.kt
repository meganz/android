package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.MediaPlayerRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUserNameByEmailUseCaseTest {
    private lateinit var underTest: GetUserNameByEmailUseCase
    private val mediaPlayerRepository = mock<MediaPlayerRepository>()

    private val testEmail = "test@test.com"
    private val testUserName = "username"

    @BeforeAll
    fun setUp() {
        underTest = GetUserNameByEmailUseCase(mediaPlayerRepository = mediaPlayerRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(mediaPlayerRepository)
    }

    @Test
    fun `test that result is null`() =
        runTest {
            whenever(mediaPlayerRepository.getUserNameByEmail(testEmail)).thenReturn(null)
            assertThat(underTest(testEmail)).isNull()
        }

    @Test
    fun `test that the username is returned`() =
        runTest {
            whenever(mediaPlayerRepository.getUserNameByEmail(testEmail)).thenReturn(testUserName)
            assertThat(underTest(testEmail)).isEqualTo(testUserName)
        }

    @Test
    fun `test that the function is invoked as expected`() =
        runTest {
            underTest(testEmail)
            verify(mediaPlayerRepository).getUserNameByEmail(testEmail)
        }
}