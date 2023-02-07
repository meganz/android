package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

internal class DefaultUpdateCurrentUserNameTest {
    private lateinit var underTest: UpdateCurrentUserName

    private val repository = mock<ContactsRepository>()
    private val oldFirstName = "firstName"
    private val oldLastName = "lastName"

    @Before
    fun setUp() {
        underTest = DefaultUpdateCurrentUserName(
            repository = repository
        )
    }

    @Test
    fun `test that only updateCurrentUserFirstName invoke when change only first name`() = runTest {
        val newFirstName = "newFirstName"
        underTest(oldFirstName, oldLastName, newFirstName, oldLastName)
        verify(repository, times(1)).updateCurrentUserFirstName(newFirstName)
        verify(repository, times(0)).updateCurrentUserLastName(any())
    }

    @Test
    fun `test that only updateCurrentUserLastName invoke when change only last name`() = runTest {
        val newLastName = "newLastName"
        underTest(oldFirstName, oldLastName, oldFirstName, newLastName)
        verify(repository, times(0)).updateCurrentUserFirstName(any())
        verify(repository, times(1)).updateCurrentUserLastName(newLastName)
    }

    @Test
    fun `test that both updateCurrentUserLastName and updateCurrentUserFirstName invoke when change first name and last name`() =
        runTest {
            val newLastName = "newLastName"
            val newFirstName = "newFirstName"
            underTest(oldFirstName, oldLastName, newFirstName, newLastName)
            verify(repository, times(1)).updateCurrentUserFirstName(newFirstName)
            verify(repository, times(1)).updateCurrentUserLastName(newLastName)
        }
}