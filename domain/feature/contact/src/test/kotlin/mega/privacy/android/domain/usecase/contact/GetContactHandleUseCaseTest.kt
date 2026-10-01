package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

class GetContactHandleUseCaseTest {

    private lateinit var underTest: GetContactHandleUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = GetContactHandleUseCase(chatRepository)
    }

    @ParameterizedTest
    @ValueSource(longs = [1L])
    @NullSource
    fun `test that the right contact's handle is returned`(expected: Long?) = runTest {
        val email = "test@test.com"
        whenever(chatRepository.getContactHandle(email)).thenReturn(expected)

        val actual = underTest(email)

        assertThat(actual).isEqualTo(expected)
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }
}
