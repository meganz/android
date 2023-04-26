package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class SaveContactByEmailUseCaseTest {
    private val getUserFirstName: GetUserFirstName = mock()
    private val getUserLastName: GetUserLastName = mock()
    private val contactsRepository: ContactsRepository = mock()
    private val underTest =
        SaveContactByEmailUseCase(getUserFirstName, getUserLastName, contactsRepository)

    @Test(expected = IllegalArgumentException::class)
    fun `test that throw IllegalArgumentException when can not find the contact by email`() =
        runTest {
            whenever(contactsRepository.getContactHandleByEmail(any())).thenReturn(-1L)
            underTest.invoke("email")
        }

    @Test
    fun `test that calling get firt name, last name, nick name when contact handle returns`() =
        runTest {
            val handle = 123L
            val firstName = "firstName"
            val lastName = "lastName"
            val nickName = "nickName"
            val email = "email"
            whenever(contactsRepository.getContactHandleByEmail(any())).thenReturn(handle)
            whenever(getUserFirstName(handle, skipCache = true, shouldNotify = true)).thenReturn(
                firstName
            )
            whenever(getUserLastName(handle, skipCache = true, shouldNotify = true)).thenReturn(
                lastName
            )
            whenever(contactsRepository.getUserAlias(handle)).thenReturn(nickName)
            underTest.invoke(email)
            verify(contactsRepository, times(1)).createOrUpdateContact(
                handle = handle,
                email = email,
                firstName = firstName,
                lastName = lastName, nickname = nickName
            )
        }
}