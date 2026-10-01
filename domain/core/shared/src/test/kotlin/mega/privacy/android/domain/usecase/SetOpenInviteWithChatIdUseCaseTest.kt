package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetOpenInviteWithChatIdUseCaseTest {

    private lateinit var underTest: SetOpenInviteWithChatIdUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = SetOpenInviteWithChatIdUseCase(chatRepository = chatRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct boolean value is returned indicating whether non-hosts are allowed to add participants`(
        isAllowed: Boolean,
    ) = runTest {
        val chatId = 123L
        whenever(chatRepository.setOpenInvite(chatId = chatId)) doReturn isAllowed

        val actual = underTest(chatId = chatId)

        assertThat(actual).isEqualTo(isAllowed)
    }
}
