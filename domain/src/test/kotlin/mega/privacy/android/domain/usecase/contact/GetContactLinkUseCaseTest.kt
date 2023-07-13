package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetContactLinkUseCaseTest {
    private val contactsRepository: ContactsRepository = mock()

    private lateinit var underTest: GetContactLinkUseCase

    @BeforeAll
    fun setup() {
        underTest = GetContactLinkUseCase(contactsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(contactsRepository)
    }

    @Test
    fun `test that getContactLink invoke correctly`() = runTest {
        val userHandle = Random(1000L).nextLong()
        underTest(userHandle)
        verify(contactsRepository).getContactLink(userHandle)
    }
}