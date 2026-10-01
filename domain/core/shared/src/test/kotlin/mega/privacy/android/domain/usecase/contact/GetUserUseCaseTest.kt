package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUserUseCaseTest {

    private lateinit var underTest: GetUserUseCase
    private val contactsRepository = mock<ContactsRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetUserUseCase(contactsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            contactsRepository,
        )
    }

    @Test
    fun `test that repository method is called when use case is invoked`() = runTest {
        val userId = UserId(123L)
        underTest(userId)
        verify(contactsRepository).getUser(userId)
    }


}