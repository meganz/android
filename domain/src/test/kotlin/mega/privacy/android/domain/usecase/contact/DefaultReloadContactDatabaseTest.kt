package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultReloadContactDatabaseTest {
    private val repository: ContactsRepository = mock()
    private val getUserFirstName: GetUserFirstName = mock()
    private val getUserLastName: GetUserLastName = mock()
    private val getCurrentUserAliases: GetCurrentUserAliases = mock()

    private val underTest by lazy(LazyThreadSafetyMode.NONE) {
        DefaultReloadContactDatabase(
            repository, getUserFirstName, getUserLastName, getCurrentUserAliases
        )
    }

    @Test
    fun `test that call init data base when invoke with isForce as true`() = runTest {
        val expectedMap = mapOf(1L to "email1", 2L to "email2", 3L to "email3")
        whenever(repository.getContactEmails()).thenReturn(expectedMap)
        expectedMap.forEach { entry ->
            whenever(
                getUserFirstName.invoke(
                    entry.key,
                    skipCache = true,
                    shouldNotify = true
                )
            ).thenReturn("firstName${entry.hashCode()}")
            whenever(
                getUserLastName.invoke(
                    entry.key,
                    skipCache = true,
                    shouldNotify = true
                )
            ).thenReturn("lastName${entry.hashCode()}")
        }
        underTest(isForceReload = true)
        verify(repository, times(1)).clearContactDatabase()
        expectedMap.forEach { entry ->
            verify(repository, times(1)).saveContact(
                handle = entry.key,
                email = entry.value,
                firstName = "firstName${entry.hashCode()}",
                lastName = "lastName${entry.hashCode()}"
            )
        }
        verify(getCurrentUserAliases, times(1)).invoke()
    }

    @Test
    fun `test that call init data base when invoke with isForce as false and the database size different contact size`() =
        runTest {
            val expectedMap = mapOf(1L to "email1", 2L to "email2", 3L to "email3")
            whenever(repository.getContactEmails()).thenReturn(expectedMap)
            whenever(repository.getContactDatabaseSize()).thenReturn(expectedMap.size.dec())
            expectedMap.forEach { entry ->
                whenever(
                    getUserFirstName.invoke(
                        entry.key,
                        skipCache = true,
                        shouldNotify = true
                    )
                ).thenReturn("firstName${entry.hashCode()}")
                whenever(
                    getUserLastName.invoke(
                        entry.key,
                        skipCache = true,
                        shouldNotify = true
                    )
                ).thenReturn("lastName${entry.hashCode()}")
            }
            underTest(isForceReload = false)
            verify(repository, times(1)).clearContactDatabase()
            expectedMap.forEach { entry ->
                verify(repository, times(1)).saveContact(
                    handle = entry.key,
                    email = entry.value,
                    firstName = "firstName${entry.hashCode()}",
                    lastName = "lastName${entry.hashCode()}"
                )
            }
            verify(getCurrentUserAliases, times(1)).invoke()
        }

    @Test
    fun `test that no call init data base when invoke with isForce as false and the database size equals contact size`() =
        runTest {
            val expectedMap = mapOf(1L to "email1", 2L to "email2", 3L to "email3")
            whenever(repository.getContactEmails()).thenReturn(expectedMap)
            whenever(repository.getContactDatabaseSize()).thenReturn(expectedMap.size)
            underTest(isForceReload = false)
            verify(repository, times(0)).clearContactDatabase()
            verifyNoInteractions(getUserLastName)
            verifyNoInteractions(getUserLastName)
            verifyNoInteractions(getCurrentUserAliases)
        }
}