package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLocalContactsFromUriUseCaseTest {

    private lateinit var underTest: GetLocalContactsFromUriUseCase

    private val contactsRepository: ContactsRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = GetLocalContactsFromUriUseCase(contactsRepository = contactsRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(contactsRepository)
    }

    @Test
    fun `test that invoke returns the local contacts returned by the repository`() = runTest {
        val uriPath = UriPath("content://com.android.contacts/session/1")
        val expected = listOf(
            LocalContact(
                id = 1L,
                name = "name",
                emails = listOf("test@test.com")
            )
        )
        whenever(contactsRepository.getLocalContactsFromUri(uriPath)).thenReturn(expected)

        val actual = underTest(uriPath)

        assertThat(actual).isEqualTo(expected)
        verify(contactsRepository).getLocalContactsFromUri(uriPath)
    }
}
