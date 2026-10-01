package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsPendingToHangUpUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: IsPendingToHangUpUseCase
    val chatId = 123L

    @BeforeEach
    fun setup() {
        underTest = IsPendingToHangUpUseCase(callRepository)
    }

    @Test
    fun `test that is pending hang up call is true`() = runTest {
        whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(true)
        val actual = underTest(chatId)
        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `test that is pending hang up call is false`() = runTest {
        whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(false)
        val actual = underTest(chatId)
        Truth.assertThat(actual).isFalse()
    }
}