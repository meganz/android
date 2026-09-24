package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteContactWithEmailUseCaseTest {

    private lateinit var underTest: InviteContactWithEmailUseCase

    private val chatRepository: ChatRepository = mock()

    private val email = "test@test.com"

    @BeforeEach
    fun setUp() {
        underTest = InviteContactWithEmailUseCase(chatRepository = chatRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @ParameterizedTest
    @EnumSource(InviteContactRequest::class)
    fun `test that the correct invitation contact request is returned`(request: InviteContactRequest) =
        runTest {
            whenever(chatRepository.inviteContact(email)) doReturn request

            val actual = underTest(email)

            assertThat(actual).isEqualTo(request)
        }
}
