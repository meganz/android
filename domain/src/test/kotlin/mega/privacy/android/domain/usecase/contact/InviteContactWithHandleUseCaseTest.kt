package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.repository.ContactsRepository
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
class InviteContactWithHandleUseCaseTest {

    private lateinit var underTest: InviteContactWithHandleUseCase

    private val contactsRepository: ContactsRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = InviteContactWithHandleUseCase(
            repository = contactsRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(contactsRepository)
    }

    @ParameterizedTest
    @EnumSource(InviteContactRequest::class)
    fun `test that the correct invitation contact request is returned`(request: InviteContactRequest) =
        runTest {
            val email = "test@test.com"
            val handle = 123L
            val message = "message"
            whenever(contactsRepository.inviteContact(email, handle, message)) doReturn request

            val actual = underTest(email, handle, message)

            assertThat(actual).isEqualTo(request)
        }
}
