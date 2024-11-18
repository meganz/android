package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsChatOpeningWithLinkUseCaseTest {
    private lateinit var underTest: IsChatOpeningWithLinkUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setup() {
        underTest = IsChatOpeningWithLinkUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that whether chat is opening with link or not is returned when invoked `(isOpened: Boolean) =
        runTest {
            val chatId = 123L
            whenever(chatRepository.isChatOpeningWithLink(chatId)) doReturn isOpened
            val actual = underTest(chatId)
            Truth.assertThat(actual).isEqualTo(isOpened)
        }
}
