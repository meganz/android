package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.getCredentials
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ContactCredentialsMapper
import mega.privacy.android.data.mapper.ContactDataMapper
import mega.privacy.android.data.mapper.ContactItemMapper
import mega.privacy.android.data.mapper.ContactRequestMapper
import mega.privacy.android.data.mapper.InviteContactRequestMapper
import mega.privacy.android.data.mapper.MegaChatPeerListMapper
import mega.privacy.android.data.mapper.OnlineStatusMapper
import mega.privacy.android.data.mapper.UserLastGreenMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.mapper.toContactCredentials
import mega.privacy.android.data.wrapper.ContactWrapper
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.exception.ContactDoesNotExistException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultContactsRepositoryTest {

    private lateinit var underTest: ContactsRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val cacheFolderGateway = mock<CacheFolderGateway>()
    private val contactRequestMapper = mock<ContactRequestMapper>()
    private val userLastGreenMapper = mock<UserLastGreenMapper>()
    private val userUpdateMapper = mock<UserUpdateMapper>()
    private val megaChatPeerListMapper = mock<MegaChatPeerListMapper>()
    private val onlineStatusMapper = mock<OnlineStatusMapper>()
    private val contactItemMapper = mock<ContactItemMapper>()
    private val contactDataMapper = mock<ContactDataMapper>()
    private val inviteContactRequestMapper = mock<InviteContactRequestMapper>()
    private val localStorageGateway = mock<MegaLocalStorageGateway>()
    private val contactWrapper: ContactWrapper = mock()
    private val databaseHandler: DatabaseHandler = mock()
    private val context: Context = mock()

    private val contactCredentialsMapper: ContactCredentialsMapper =
        { credentials: String?, email: String, name: String ->
            (credentials?.getCredentials()?.let {
                AccountCredentials.ContactCredentials(it, email, name)
            })
        }

    private val userEmail = "test@mega.nz"
    private val userHandle = -123456L
    private val user = mock<MegaUser> { on { email }.thenReturn(userEmail) }
    private val testName = "Test User Name"
    private val success = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
    private val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        underTest = DefaultContactsRepository(
            megaApiGateway = megaApiGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = testDispatcher,
            cacheFolderGateway = cacheFolderGateway,
            contactRequestMapper = contactRequestMapper,
            userLastGreenMapper = userLastGreenMapper,
            userUpdateMapper = userUpdateMapper,
            megaChatPeerListMapper = megaChatPeerListMapper,
            onlineStatusMapper = onlineStatusMapper,
            contactItemMapper = contactItemMapper,
            contactDataMapper = contactDataMapper,
            contactCredentialsMapper = contactCredentialsMapper,
            inviteContactRequestMapper = inviteContactRequestMapper,
            localStorageGateway = localStorageGateway,
            contactWrapper = contactWrapper,
            databaseHandler = databaseHandler,
            context = context,
        )

        whenever(megaApiGateway.handleToBase64(userHandle)).thenReturn("LTEyMzQ1Ng==")
    }

    @Test
    fun `test that get contact credentials returns valid credentials if user exists and api returns valid credentials`() =
        runTest {
            val validCredentials = "KJ9hFK67vhj3cNCIUHAi8ccwciojiot4hVE5yab3"
            val requestCredentials = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
                on { paramType }.thenReturn(MegaApiJava.USER_ATTR_ED25519_PUBLIC_KEY)
                on { password }.thenReturn(validCredentials)
            }
            val alias = "testAlias"
            val requestAlias = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
                on { paramType }.thenReturn(MegaApiJava.USER_ATTR_ALIAS)
                on { name }.thenReturn(alias)
            }
            val expectedCredentials = toContactCredentials(
                validCredentials, userEmail, alias
            )

            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.getUserCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), requestCredentials, success
                )
            }
            whenever(megaApiGateway.getUserAlias(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), requestAlias, success
                )
            }
            assertThat(underTest.getContactCredentials(userEmail)).isEqualTo(expectedCredentials)
        }

    @Test(expected = MegaException::class)
    fun `test that get contact credentials throws a MegaException if user exists but api returns error`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.getUserCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), error
                )
            }
            assertThat(underTest.getContactCredentials(userEmail))
        }

    @Test
    fun `test that get contact credentials returns null if api returns user is null`() = runTest {
        whenever(megaApiGateway.getContact(userEmail)).thenReturn(null)
        assertThat(underTest.getContactCredentials(userEmail)).isNull()
    }

    @Test(expected = MegaException::class)
    fun `test that get contact credentials fails with MegaException if api returns an error`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
                on { paramType }.thenReturn(MegaApiJava.USER_ATTR_ED25519_PUBLIC_KEY)
            }

            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.getUserCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, error
                )
            }
            assertThat(underTest.getContactCredentials(userEmail))
        }

    @Test
    fun `test that get contact alias returns the alias if api returns the alias`() = runTest {
        val alias = "testAlias"
        val request = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_ALIAS)
            on { name }.thenReturn(alias)
        }

        whenever(megaApiGateway.getUserAlias(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        assertThat(underTest.getUserAlias(userHandle)).isEqualTo(alias)
    }

    @Test(expected = MegaException::class)
    fun `test that get user alias throws a MegaException if api fails with error`() = runTest {
        whenever(megaApiGateway.getUserAlias(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), error
            )
        }
        assertThat(underTest.getUserAlias(userHandle))
    }

    @Test
    fun `test that get user first name returns the name if api returns the first name`() = runTest {
        val request = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_FIRSTNAME)
            on { text }.thenReturn(testName)
            on { email }.thenReturn(userEmail)
        }

        whenever(megaApiGateway.getUserAttribute(anyString(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        val result =
            underTest.getUserFirstName(handle = userHandle, skipCache = true, shouldNotify = false)
        verify(databaseHandler, times(1)).setContactName(testName, userEmail)
        verifyNoInteractions(contactWrapper)

        assertThat(result).isEqualTo(testName)
    }

    @Test
    fun `test that get user first name returns the name if api returns the first name from cache`() =
        runTest {
            whenever(megaChatApiGateway.getUserFirstnameFromCache(any())).thenReturn(testName)

            val result = underTest.getUserFirstName(userHandle, false)
            verifyNoInteractions(contactWrapper)
            verifyNoInteractions(databaseHandler)

            assertThat(result).isEqualTo(testName)
        }

    @Test(expected = MegaException::class)
    fun `test that get user first name throws a MegaException if api fails with error`() = runTest {
        whenever(megaApiGateway.getUserAttribute(anyString(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), error
            )
        }

        underTest.getUserFirstName(userHandle, true)
    }

    @Test
    fun `test that get user last name returns the name if api returns the last name`() = runTest {
        val request = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_LASTNAME)
            on { text }.thenReturn(testName)
        }

        whenever(megaApiGateway.getUserAttribute(anyString(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        val result = underTest.getUserLastName(userHandle, true)

        assertThat(result).isEqualTo(testName)
    }

    @Test
    fun `test that get user last name returns the name if api returns the last name from cache`() =
        runTest {
            whenever(megaChatApiGateway.getUserLastnameFromCache(any())).thenReturn(testName)

            val result = underTest.getUserLastName(userHandle, false)

            assertThat(result).isEqualTo(testName)
        }

    @Test(expected = MegaException::class)
    fun `test that get user last name throws a MegaException if api fails with error`() = runTest {
        whenever(megaApiGateway.getUserAttribute(anyString(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), error
            )
        }

        underTest.getUserLastName(userHandle, true)
    }

    @Test
    fun `test that get user full name returns the name if api returns the full name`() = runTest {
        whenever(megaChatApiGateway.getUserFullNameFromCache(any())).thenReturn(testName)

        val result = underTest.getUserFullName(userHandle, false)

        assertThat(result).isEqualTo(testName)
    }

    @Test
    fun `test that are credentials verified returns true if user exists and api returns true`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.areCredentialsVerified(user)).thenReturn(true)
            assertThat(underTest.areCredentialsVerified(userEmail)).isTrue()
        }

    @Test
    fun `test that are credentials verified returns false if user exists and api returns false`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.areCredentialsVerified(user)).thenReturn(false)
            assertThat(underTest.areCredentialsVerified(userEmail)).isFalse()
        }

    @Test(expected = ContactDoesNotExistException::class)
    fun `test that are credentials verified throws a ContactDoesNotExistException if user does not exist`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(null)
            assertThat(underTest.areCredentialsVerified(userEmail))
        }

    @Test
    fun `test that reset credentials finish correctly if user exists and api completes successfully`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.resetCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            assertThat(underTest.resetCredentials(userEmail))
        }

    @Test(expected = MegaException::class)
    fun `test that reset credentials throws a MegaException if user exists but api fails with error`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.resetCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), error
                )
            }
            assertThat(underTest.resetCredentials(userEmail))
        }

    @Test(expected = ContactDoesNotExistException::class)
    fun `test that reset credentials throws a ContactDoesNotExistException if user does not exist`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(null)
            assertThat(underTest.resetCredentials(userEmail))
        }

    @Test
    fun `test that verify credentials finish correctly if user exists and api completes successfully`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.verifyCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), success
                )
            }
            assertThat(underTest.verifyCredentials(userEmail))
        }

    @Test(expected = MegaException::class)
    fun `test that verify credentials throws a MegaException if user exists but api fails with error`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.verifyCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), error
                )
            }
            assertThat(underTest.verifyCredentials(userEmail))
        }

    @Test(expected = ContactDoesNotExistException::class)
    fun `test that verify credentials throws a ContactDoesNotExistException if user does not exist`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(null)
            assertThat(underTest.verifyCredentials(userEmail))
        }

    @Test
    fun `test that getCurrentUserFirstName returns correct cache value when database handler has it in cache`() =
        runTest {
            val expectedFirstName = "abc"
            val credentials = UserCredentials(
                firstName = expectedFirstName,
                email = null,
                session = null,
                lastName = null,
                myHandle = null,
            )
            whenever(localStorageGateway.getUserCredentials()).thenReturn(credentials)
            assertThat(underTest.getCurrentUserFirstName(forceRefresh = false)).isEqualTo(
                expectedFirstName
            )
        }

    @Test
    fun `test that getCurrentUserFirstName return correct value when it is not in database handler cache and getUserAttribute is success`() =
        runTest {
            val expectedFirstName = "abc"
            val credentials = UserCredentials(
                firstName = null,
                email = null,
                session = null,
                lastName = null,
                myHandle = null,
            )
            whenever(localStorageGateway.getUserCredentials()).thenReturn(credentials)

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedFirstName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(), request = request, error = success
                )
            }
            assertEquals(expectedFirstName, underTest.getCurrentUserFirstName(forceRefresh = false))
        }

    @Test
    fun `test that userFirstName is saved in database handler when it is not in database handler cache and getUserAttribute is success`() =
        runTest {
            val expectedFirstName = "abc"
            val credentials = UserCredentials(
                firstName = null,
                email = null,
                session = null,
                lastName = null,
                myHandle = null,
            )
            whenever(localStorageGateway.getUserCredentials()).thenReturn(credentials)

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedFirstName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }
            underTest.getCurrentUserFirstName(forceRefresh = false)
            verify(localStorageGateway).saveMyFirstName(expectedFirstName)
        }

    @Test
    fun `test that getCurrentUserLastName returns correct cache value when database handler has it in cache`() =
        runTest {
            val expectedLastName = "abc"
            val credentials = UserCredentials(
                firstName = null,
                email = null,
                session = null,
                lastName = expectedLastName,
                myHandle = null,
            )
            whenever(localStorageGateway.getUserCredentials()).thenReturn(credentials)
            assertThat(underTest.getCurrentUserLastName(forceRefresh = false)).isEqualTo(
                expectedLastName
            )
        }

    @Test
    fun `test that getCurrentUserLastName return correct value when it is not in database handler cache and getUserAttribute success`() =
        runTest {
            val expectedLastName = "cde"
            val credentials = UserCredentials(
                email = null,
                firstName = null,
                session = null,
                lastName = null,
                myHandle = null,
            )
            whenever(localStorageGateway.getUserCredentials()).thenReturn(credentials)

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedLastName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }
            assertEquals(expectedLastName, underTest.getCurrentUserLastName(forceRefresh = false))
        }

    @Test
    fun `test that userLastName is saved when it is not in database handler cache and getUserAttribute success`() =
        runTest {
            val expectedLastName = "cde"
            val credentials = UserCredentials(
                email = null,
                firstName = null,
                session = null,
                lastName = null,
                myHandle = null,
            )
            whenever(localStorageGateway.getUserCredentials()).thenReturn(credentials)

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedLastName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }
            underTest.getCurrentUserLastName(forceRefresh = false)
            verify(localStorageGateway).saveMyLastName(expectedLastName)
        }

    @Test
    fun `test that cache value of first name is not used when force refresh is enabled`() =
        runTest {

            val expectedFirstName = "cde"
            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedFirstName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }

            underTest.getCurrentUserFirstName(forceRefresh = true)
            verify(localStorageGateway, never()).getUserCredentials()
        }

    @Test
    fun `test that new first name is saved when force refresh is enabled`() = runTest {

        val expectedFirstName = "cde"
        val request = mock<MegaRequest> {
            on { text }.thenReturn(expectedFirstName)
        }
        whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }

        underTest.getCurrentUserFirstName(forceRefresh = true)
        verify(localStorageGateway).saveMyFirstName(expectedFirstName)
    }

    @Test
    fun `test that mega api gateway to get first name is always called when force refresh is enabled`() =
        runTest {

            val expectedFirstName = "cde"
            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedFirstName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }

            underTest.getCurrentUserFirstName(forceRefresh = true)
            verify(megaApiGateway).getUserAttribute(any(), any())
        }

    /////
    @Test
    fun `test that cache value of last name is not used when force refresh is enabled`() = runTest {

        val expectedLastName = "cde"
        val request = mock<MegaRequest> {
            on { text }.thenReturn(expectedLastName)
        }
        whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }

        underTest.getCurrentUserLastName(forceRefresh = true)
        verify(localStorageGateway, never()).getUserCredentials()
    }

    @Test
    fun `test that new last name is saved when force refresh is enabled`() = runTest {

        val expectedLastName = "cde"
        val request = mock<MegaRequest> {
            on { text }.thenReturn(expectedLastName)
        }
        whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }

        underTest.getCurrentUserLastName(forceRefresh = true)
        verify(localStorageGateway).saveMyLastName(expectedLastName)
    }

    @Test
    fun `test that mega api gateway to get last name is always called when force refresh is enabled`() =
        runTest {

            val expectedLastName = "cde"
            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedLastName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }

            underTest.getCurrentUserLastName(forceRefresh = true)
            verify(megaApiGateway).getUserAttribute(any(), any())
        }

    @Test
    fun `test that successful invite contact returns Sent enum value`() = runTest {

        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val expectedResult = InviteContactRequest.Sent

        whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), megaError
            )
        }
        whenever(inviteContactRequestMapper(megaError)).thenReturn(expectedResult)

        val result = underTest.inviteContact(userEmail, userHandle, null)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that inviting existing contact returns AlreadyContact enum value`() = runTest {

        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) }
        val expectedResult = InviteContactRequest.AlreadyContact

        whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), megaError
            )
        }
        whenever(inviteContactRequestMapper(megaError)).thenReturn(expectedResult)

        val result = underTest.inviteContact(userEmail, userHandle, null)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that inviting contact with self email returns InvalidEmail enum value`() = runTest {

        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }
        val expectedResult = InviteContactRequest.InvalidEmail

        whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), megaError
            )
        }
        whenever(inviteContactRequestMapper(megaError)).thenReturn(expectedResult)

        val result = underTest.inviteContact(userEmail, userHandle, null)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that error on inviting contact returns InvalidStatus enum value`() = runTest {

        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EACCESS) }
        val expectedResult = InviteContactRequest.InvalidStatus

        whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), megaError
            )
        }
        whenever(inviteContactRequestMapper(megaError)).thenReturn(expectedResult)

        val result = underTest.inviteContact(userEmail, userHandle, null)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that updateCurrentUserFirstName return correct value when calling API returns success`() =
        runTest {
            val expectedNewFirstName = "myNewName"

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedNewFirstName)
            }
            whenever(megaApiGateway.setUserAttribute(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(), request = request, error = success
                )
            }

            assertEquals(
                expectedNewFirstName,
                underTest.updateCurrentUserFirstName(expectedNewFirstName)
            )

            verify(localStorageGateway, times(1)).saveMyFirstName(expectedNewFirstName)
        }

    @Test
    fun `test that updateCurrentUserLastName return correct value when calling API returns success`() =
        runTest {
            val expectedNewLastName = "myNewName"

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedNewLastName)
            }
            whenever(megaApiGateway.setUserAttribute(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(), request = request, error = success
                )
            }

            assertEquals(
                expectedNewLastName,
                underTest.updateCurrentUserLastName(expectedNewLastName)
            )

            verify(localStorageGateway, times(1)).saveMyLastName(expectedNewLastName)
        }
}