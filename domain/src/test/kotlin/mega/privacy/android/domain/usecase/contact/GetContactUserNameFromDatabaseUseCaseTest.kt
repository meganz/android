package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetContactUserNameFromDatabaseUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetContactUserNameFromDatabaseUseCaseTest {

    private lateinit var underTest: GetContactUserNameFromDatabaseUseCase

    private val contactsRepository = mock<ContactsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetContactUserNameFromDatabaseUseCase(contactsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(contactsRepository)
    }

    @Test
    fun `test that the contact user name is retrieved from the database`() = runTest {
        val user = "Test User"
        val userNameFromDatabase = "Test User Name from Database"

        whenever(contactsRepository.getContactUserNameFromDatabase(any())).thenReturn(
            userNameFromDatabase
        )
        assertThat(underTest(user)).isEqualTo(userNameFromDatabase)
    }

    @Test
    fun `test that the contact user name retrieved from the database is null`() = runTest {
        val user = "Test User"
        whenever(contactsRepository.getContactUserNameFromDatabase(any())).thenReturn(null)
        assertThat(underTest(user)).isNull()
    }
}