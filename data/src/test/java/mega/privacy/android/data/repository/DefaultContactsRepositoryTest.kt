package mega.privacy.android.data.repository

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.contact.ContactGateway
import mega.privacy.android.data.gateway.preferences.CredentialsPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ContactRequestMapper
import mega.privacy.android.data.mapper.InviteContactRequestMapper
import mega.privacy.android.data.mapper.UserUpdateMapper
import mega.privacy.android.data.mapper.chat.ChatConnectionStateMapper
import mega.privacy.android.data.mapper.chat.ChatConnectionStatusMapper
import mega.privacy.android.data.mapper.chat.OnlineStatusMapper
import mega.privacy.android.data.mapper.chat.UserLastGreenMapper
import mega.privacy.android.data.mapper.contact.ContactCredentialsMapper
import mega.privacy.android.data.mapper.contact.ContactDataMapper
import mega.privacy.android.data.mapper.contact.ContactItemMapper
import mega.privacy.android.data.mapper.contact.ContactRequestActionMapper
import mega.privacy.android.data.mapper.contact.UserChatStatusMapper
import mega.privacy.android.data.mapper.contact.UserMapper
import mega.privacy.android.data.mapper.contact.UserVisibilityMapper
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.wrapper.ContactWrapper
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.exception.ContactDoesNotExistException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultContactsRepositoryTest {

    private lateinit var underTest: ContactsRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val cacheGateway = mock<CacheGateway>()
    private val contactRequestMapper = mock<ContactRequestMapper>()
    private val userLastGreenMapper = mock<UserLastGreenMapper>()
    private val userUpdateMapper = mock<UserUpdateMapper>()
    private val onlineStatusMapper = mock<OnlineStatusMapper>()
    private val contactItemMapper = mock<ContactItemMapper>()
    private val contactDataMapper = mock<ContactDataMapper>()
    private val contactCredentialsMapper = mock<ContactCredentialsMapper>()
    private val inviteContactRequestMapper = mock<InviteContactRequestMapper>()
    private val contactRequestActionMapper = mock<ContactRequestActionMapper>()
    private val chatConnectionStateMapper =
        ChatConnectionStateMapper(chatConnectionStatusMapper = ChatConnectionStatusMapper())
    private val credentialsPreferencesGateway = mock<CredentialsPreferencesGateway>()
    private val contactWrapper: ContactWrapper = mock()
    private val databaseHandler: DatabaseHandler = mock()
    private val megaLocalRoomGateway: MegaLocalRoomGateway = mock()
    private val context: Context = mock()
    private val contactGateway: ContactGateway = mock()

    private val userEmail = "test@mega.io"
    private val userHandle = -123456L
    private val user = mock<MegaUser> { on { email }.thenReturn(userEmail) }
    private val testName = "Test User Name"
    private val avatarUri = "Avatar url"
    private val success = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
    private val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }
    private val testDispatcher = UnconfinedTestDispatcher()
    private val userChatStatusMapper: UserChatStatusMapper = mock()
    private val userVisibilityMapper =
        mock<UserVisibilityMapper> { on { invoke(anyOrNull()) } doReturn UserVisibility.Unknown }

    private val userMapper: UserMapper = mock()

    private val request = mock<MegaRequest> {
        on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
        on { paramType }.thenReturn(MegaApiJava.USER_ATTR_FIRSTNAME)
        on { text }.thenReturn(testName)
        on { email }.thenReturn(userEmail)
        on { name }.thenReturn(testName)
        on { file }.thenReturn(avatarUri)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        initRepository()

        whenever(megaApiGateway.userHandleToBase64(userHandle)).thenReturn("LTEyMzQ1Ng==")
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    private fun initRepository() {
        underTest = DefaultContactsRepository(
            megaApiGateway = megaApiGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = testDispatcher,
            cacheGateway = cacheGateway,
            contactRequestMapper = contactRequestMapper,
            userLastGreenMapper = userLastGreenMapper,
            userUpdateMapper = userUpdateMapper,
            onlineStatusMapper = onlineStatusMapper,
            contactItemMapper = contactItemMapper,
            contactDataMapper = contactDataMapper,
            contactCredentialsMapper = contactCredentialsMapper,
            inviteContactRequestMapper = inviteContactRequestMapper,
            credentialsPreferencesGateway = { credentialsPreferencesGateway },
            contactRequestActionMapper = contactRequestActionMapper,
            contactWrapper = contactWrapper,
            databaseHandler = { databaseHandler },
            chatConnectionStateMapper = chatConnectionStateMapper,
            context = context,
            megaLocalRoomGateway = megaLocalRoomGateway,
            userChatStatusMapper = userChatStatusMapper,
            userMapper = userMapper,
            sharingScope = CoroutineScope(UnconfinedTestDispatcher()),
            contactGateway = contactGateway,
            userVisibilityMapper = userVisibilityMapper,
        )
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
            val expectedCredentials = contactCredentialsMapper(
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

    @Test
    fun `test that get contact credentials returns null if user exists but api returns error`() =
        runTest {
            whenever(megaApiGateway.getContact(userEmail)).thenReturn(user)
            whenever(megaApiGateway.getUserCredentials(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), error
                )
            }
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
            assertThat(underTest.getContactCredentials(userEmail)).isNull()
        }

    @Test
    fun `test that get contact credentials returns null if api returns user is null`() = runTest {
        whenever(megaApiGateway.getContact(userEmail)).thenReturn(null)
        assertThat(underTest.getContactCredentials(userEmail)).isNull()
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
        whenever(megaApiGateway.getUserAttribute(anyString(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        val result =
            underTest.getUserFirstName(handle = userHandle, skipCache = true, shouldNotify = false)
        verify(megaLocalRoomGateway).updateContactFistNameByHandle(userHandle, testName)
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
            whenever(credentialsPreferencesGateway.monitorCredentials()).thenReturn(
                flowOf(
                    credentials
                )
            )
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
            whenever(credentialsPreferencesGateway.monitorCredentials()).thenReturn(
                flowOf(
                    credentials
                )
            )

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
            whenever(credentialsPreferencesGateway.monitorCredentials()).thenReturn(
                flowOf(
                    credentials
                )
            )

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedFirstName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }
            underTest.getCurrentUserFirstName(forceRefresh = false)
            verify(credentialsPreferencesGateway).saveFirstName(expectedFirstName)
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
            whenever(credentialsPreferencesGateway.monitorCredentials()).thenReturn(
                flowOf(
                    credentials
                )
            )
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
            whenever(credentialsPreferencesGateway.monitorCredentials()).thenReturn(
                flowOf(
                    credentials
                )
            )

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
            whenever(credentialsPreferencesGateway.monitorCredentials()).thenReturn(
                flowOf(
                    credentials
                )
            )

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedLastName)
            }
            whenever(megaApiGateway.getUserAttribute(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, success
                )
            }
            underTest.getCurrentUserLastName(forceRefresh = false)
            verify(credentialsPreferencesGateway).saveLastName(expectedLastName)
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
            verify(credentialsPreferencesGateway, never()).monitorCredentials()
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
        verify(credentialsPreferencesGateway).saveFirstName(expectedFirstName)
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
        verify(credentialsPreferencesGateway, never()).monitorCredentials()
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
        verify(credentialsPreferencesGateway).saveLastName(expectedLastName)
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
        val myEmail = "myEmail@mega.co.nz"
        val outgoingContactRequests = arrayListOf<MegaContactRequest>()
        val request = mock<MegaRequest> {
            on { email }.thenReturn(myEmail)
        }
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val expectedResult = InviteContactRequest.Sent


        whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, megaError
            )
        }
        whenever(megaApiGateway.outgoingContactRequests()).thenReturn(outgoingContactRequests)
        whenever(
            inviteContactRequestMapper(
                megaError,
                myEmail,
                outgoingContactRequests
            )
        ).thenReturn(expectedResult)

        val result = underTest.inviteContact(userEmail, userHandle, null)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that inviting existing contact returns AlreadyContact enum value`() = runTest {
        val myEmail = "myEmail@mega.co.nz"
        val outgoingContactRequests = arrayListOf<MegaContactRequest>()
        val request = mock<MegaRequest> {
            on { email }.thenReturn(myEmail)
        }
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) }
        val expectedResult = InviteContactRequest.AlreadyContact

        whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, megaError
            )
        }
        whenever(megaApiGateway.outgoingContactRequests()).thenReturn(outgoingContactRequests)
        whenever(
            inviteContactRequestMapper(
                megaError,
                myEmail,
                outgoingContactRequests
            )
        ).thenReturn(expectedResult)

        val result = underTest.inviteContact(userEmail, userHandle, null)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that inviting contact with self email returns InvalidEmail enum value`() = runTest {
        val myEmail = "myEmail@mega.co.nz"
        val outgoingContactRequests = arrayListOf<MegaContactRequest>()
        val request = mock<MegaRequest> {
            on { email }.thenReturn(myEmail)
        }
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }
        val expectedResult = InviteContactRequest.InvalidEmail

        whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, megaError
            )
        }
        whenever(megaApiGateway.outgoingContactRequests()).thenReturn(outgoingContactRequests)
        whenever(
            inviteContactRequestMapper(
                megaError,
                myEmail,
                outgoingContactRequests
            )
        ).thenReturn(expectedResult)

        val result = underTest.inviteContact(userEmail, userHandle, null)
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test(expected = MegaException::class)
    fun `test that error on inviting contact throw MegaException when mapper throw exception`() =
        runTest {
            val myEmail = "myEmail@mega.co.nz"
            val outgoingContactRequests = arrayListOf<MegaContactRequest>()
            val request = mock<MegaRequest> {
                on { email }.thenReturn(myEmail)
            }
            val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EACCESS) }

            whenever(megaApiGateway.inviteContact(any(), any(), anyOrNull(), any())).thenAnswer {
                ((it.arguments[3]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, megaError
                )
            }
            whenever(megaApiGateway.outgoingContactRequests()).thenReturn(outgoingContactRequests)
            whenever(
                inviteContactRequestMapper(
                    megaError,
                    myEmail,
                    outgoingContactRequests
                )
            ).thenThrow(MegaException::class.java)

            underTest.inviteContact(userEmail, userHandle, null)
        }

    @Test
    fun `test that updateCurrentUserFirstName return correct value when calling API returns success`() =
        runTest {
            val expectedNewFirstName = "myNewName"

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedNewFirstName)
            }
            whenever(
                megaApiGateway.setUserAttribute(
                    any(),
                    anyString(),
                    any()
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(), request = request, error = success
                )
            }

            assertEquals(
                expectedNewFirstName,
                underTest.updateCurrentUserFirstName(expectedNewFirstName)
            )

            verify(credentialsPreferencesGateway).saveFirstName(expectedNewFirstName)
        }

    @Test
    fun `test that updateCurrentUserLastName return correct value when calling API returns success`() =
        runTest {
            val expectedNewLastName = "myNewName"

            val request = mock<MegaRequest> {
                on { text }.thenReturn(expectedNewLastName)
            }
            whenever(
                megaApiGateway.setUserAttribute(
                    any(),
                    anyString(),
                    any()
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(), request = request, error = success
                )
            }

            assertEquals(
                expectedNewLastName,
                underTest.updateCurrentUserLastName(expectedNewLastName)
            )

            verify(credentialsPreferencesGateway).saveLastName(expectedNewLastName)
        }

    @Test
    fun `test that getContactEmails return correctly when megaApiGateway getContacts returns`() =
        runTest {
            val expectedMap = mapOf(1L to "email1", 2L to "email2", 3L to "email3")
            val users = expectedMap.map { entry ->
                mock<MegaUser> {
                    on { handle }.thenReturn(entry.key)
                    on { email }.thenReturn(entry.value)
                }
            }
            whenever(megaApiGateway.getContacts()).thenReturn(users)
            assertEquals(expectedMap, underTest.getContactEmails())
        }

    @Test
    fun `test that clear contact database when call clearContactDatabase`() =
        runTest {
            underTest.clearContactDatabase()
            verify(databaseHandler).clearContacts()
        }

    @Test
    fun `test that save contact database when call saveContact with value`() =
        runTest {
            whenever(megaLocalRoomGateway.getContactByHandle(any())).thenReturn(null)
            underTest.createOrUpdateContact(
                -1L,
                "myEmail",
                "firstName",
                "lastName",
                "nickname",
            )
            verify(megaLocalRoomGateway).insertContact(any())
        }

    @Test
    fun `test that getContactEmail calls to database and returns value when calling API returns success`() =
        runTest {
            val expectedEmail = "contactEmail"
            val handle = 1L

            val request = mock<MegaRequest> {
                on { email }.thenReturn(expectedEmail)
            }
            whenever(megaApiGateway.getUserEmail(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    api = mock(), request = request, error = success
                )
            }

            assertEquals(
                expectedEmail,
                underTest.getContactEmail(handle)
            )

            verify(megaLocalRoomGateway).updateContactMailByHandle(handle, expectedEmail)
        }

    @Test
    fun `test getUserEmail returns api email if skipCache is true`() = runTest {
        val expected = "email@example.com"
        val request = mock<MegaRequest> {
            on { email }.thenReturn(expected)
        }
        whenever(megaApiGateway.getUserEmail(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        val email = underTest.getUserEmail(1L, true)
        assertEquals(expected, email)
    }

    @Test
    fun `test getUserEmail returns chat api cached email if skipCache is false`() = runTest {
        val expected = "email@example.com"
        whenever(megaChatApiGateway.getUserEmailFromCache(any())).thenReturn(expected)
        val email = underTest.getUserEmail(1L, false)
        assertEquals(expected, email)
    }

    @Test
    fun `test getContactItem returns updated info, not from cache if skip is true`() = runTest {
        val megaUser = mock<MegaUser> {
            on { handle }.thenReturn(userHandle)
            on { email }.thenReturn(userEmail)
        }
        val expected = mockContactItemForMegaUser(megaUser)
        whenever(megaApiGateway.getContact(any())).thenReturn(megaUser)
        whenever(megaApiGateway.userHandleToBase64(userHandle)).thenReturn(userEmail)
        whenever(
            megaApiGateway.getUserAlias(anyLong(), any())
        ).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        whenever(
            megaApiGateway.getContactAvatar(anyString(), anyString(), any())
        ).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }

        val result = underTest.getContactItem(UserId(userHandle), true)

        assertEquals(expected, result)
        verify(megaApiGateway, atLeast(1)).getUserAlias(anyLong(), any())
        verify(megaApiGateway, atLeast(1)).getContactAvatar(anyString(), anyString(), any())
    }

    @Test
    fun `test getVisibleContacts returns visible contacts from cache, no update calls`() = runTest {
        val megaUserHidden = mock<MegaUser> {
            on { handle }.thenReturn(1L)
            on { email }.thenReturn("hidden@example.com")
            on { visibility }.thenReturn(MegaUser.VISIBILITY_HIDDEN)
        }
        val megaUserVisible = mock<MegaUser> {
            on { handle }.thenReturn(userHandle)
            on { email }.thenReturn(userEmail)
            on { visibility }.thenReturn(MegaUser.VISIBILITY_VISIBLE)
        }

        whenever(megaApiGateway.getContacts()).thenReturn(listOf(megaUserVisible, megaUserHidden))

        val expected = listOf(
            mockContactItemForMegaUser(megaUserVisible)
        )

        val result = underTest.getVisibleContacts()

        assertEquals(expected, result)
        verify(megaApiGateway, never()).getUserAlias(anyLong(), any())
        verify(megaApiGateway, never()).getContactAvatar(anyString(), anyString(), any())
    }

    /**
     * common mock initialization for returning the ContactItem for a given MegaUser
     * It may have further mocking, for instance if skipCache is true
     * @return the ContactItem of the user
     */
    private suspend fun mockContactItemForMegaUser(megaUser: MegaUser): ContactItem {
        val expectedFullName = "full name"
        val expectedStatus = MegaChatApi.STATUS_ONLINE
        val expectedColor = "color"
        val expectedCredentials = true
        val cacheFile = mock<File> {
            on { absolutePath }.thenReturn(avatarUri)
        }
        whenever(megaApiGateway.getUserAvatarColor(megaUser)).thenReturn(expectedColor)
        whenever(megaApiGateway.areCredentialsVerified(any())).thenReturn(expectedCredentials)
        whenever(megaChatApiGateway.getUserEmailFromCache(any())).thenReturn(userEmail)
        whenever(megaChatApiGateway.getUserFullNameFromCache(any())).thenReturn(expectedFullName)
        whenever(megaChatApiGateway.getUserOnlineStatus(any())).thenReturn(expectedStatus)
        whenever(cacheGateway.buildAvatarFile(any())).thenReturn(cacheFile)
        whenever(megaChatApiGateway.getUserAliasFromCache(any())).thenReturn(testName)

        val expectedContactData = contactDataMapper(
            fullName = expectedFullName, alias = testName, avatarUri = avatarUri,
            userVisibility = UserVisibility.Unknown,
        )
        return contactItemMapper(
            megaUser,
            expectedContactData,
            expectedColor,
            expectedCredentials,
            expectedStatus,
            null,
            null,
        )
    }

    @Test
    fun `test if peer handle is returned when chat id is passed`() = runTest {
        val megaChatRoom = mock<MegaChatRoom>()
        whenever(megaChatApiGateway.getChatRoom(0L)).thenReturn(megaChatRoom)
        whenever(megaChatRoom.getPeerHandle(0)).thenReturn(userHandle)
        whenever(megaApiGateway.userHandleToBase64(userHandle)).thenReturn(userEmail)
        val email = underTest.getUserEmailFromChat(0L)
        assertEquals(userEmail, email)
    }

    @Test
    fun `test if contact is returned from email`() = runTest {
        val megaUser = mock<MegaUser> {
            on { handle }.thenReturn(userHandle)
            on { email }.thenReturn(userEmail)
        }
        val expectedFullName = "full name"
        val expectedStatus = MegaChatApi.STATUS_ONLINE
        val expectedColor = "color"
        val expectedCredentials = true
        val expectedAlias = "alias"

        whenever(megaApiGateway.getContact(any())).thenReturn(megaUser)
        val firstNameRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_FIRSTNAME)
            on { text }.thenReturn("full")
        }
        whenever(megaApiGateway.getUserAttribute(anyString(), anyInt(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface)
                .onRequestFinish(mock(), firstNameRequest, success)
        }
        val lastNameRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_LASTNAME)
            on { text }.thenReturn("name")
        }
        whenever(megaApiGateway.getUserAttribute(anyString(), anyInt(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface)
                .onRequestFinish(mock(), lastNameRequest, success)
        }
        whenever(megaChatApiGateway.getUserFullNameFromCache(any())).thenReturn(expectedFullName)

        val aliasRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_ALIAS)
            on { name }.thenReturn(expectedAlias)
        }
        whenever(megaApiGateway.getUserAlias(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface)
                .onRequestFinish(mock(), aliasRequest, success)
        }
        whenever(megaChatApiGateway.getUserOnlineStatus(any())).thenReturn(expectedStatus)
        val avatarRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_AVATAR)
            on { file }.thenReturn(avatarUri)
        }
        whenever(megaApiGateway.getContactAvatar(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface)
                .onRequestFinish(mock(), avatarRequest, success)
        }
        whenever(megaApiGateway.getUserAvatarColor(megaUser)).thenReturn(expectedColor)
        whenever(megaApiGateway.areCredentialsVerified(any())).thenReturn(expectedCredentials)

        val expectedContactData = contactDataMapper(
            fullName = expectedFullName, alias = expectedAlias, avatarUri = avatarUri,
            userVisibility = UserVisibility.Unknown,
        )
        val expectedContact = contactItemMapper(
            megaUser,
            expectedContactData,
            expectedColor,
            expectedCredentials,
            expectedStatus,
            null,
            null,
        )

        val contact = underTest.getContactItemFromUserEmail(userEmail, skipCache = true)
        assertEquals(contact, expectedContact)
    }

    @Test
    fun `test that monitorChatConnectionStateUpdates returns flow of chatConnectionState`() {
        val chatId = 123456L
        whenever(megaChatApiGateway.chatUpdates).thenReturn(
            flowOf(
                ChatUpdate.OnChatConnectionStateUpdate(
                    chatId = chatId,
                    newState = MegaChatApi.CHAT_CONNECTION_ONLINE
                )
            )
        )
        runTest {
            underTest.monitorChatConnectionStateUpdates().test {
                val actual = awaitItem()
                awaitComplete()
                assertThat(actual.chatConnectionStatus).isEqualTo(ChatConnectionStatus.Online)
            }
        }
    }

    @Test
    fun `test that empty list is returned when gateway returns null for incoming contact requests`() =
        runTest {
            whenever(megaApiGateway.getIncomingContactRequests()).thenReturn(null)
            assertThat(underTest.getIncomingContactRequests().size).isEqualTo(0)
        }

    @Test
    fun `test that api gateway is called when get incoming contact requests is triggered`() =
        runTest {
            val contactRequest = mock<MegaContactRequest>()
            whenever(megaApiGateway.getIncomingContactRequests()).thenReturn(
                arrayListOf(contactRequest)
            )
            whenever(contactRequestMapper(any())).thenReturn(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "sourceEmail",
                    sourceMessage = "sourceMessage,",
                    targetEmail = "targetEmail",
                    creationTime = 1L,
                    modificationTime = 2L,
                    status = ContactRequestStatus.Accepted,
                    isOutgoing = false,
                    isAutoAccepted = true,
                )
            )
            assertThat(underTest.getIncomingContactRequests().size).isEqualTo(1)
        }

    @Test
    fun `test that getContactLink returns correctly when calling api gateway returns successfully and contacts return the list contain visible user`() =
        runTest {
            val myEmail = "myEmail@mega.co.nz"
            val myParentHandle = 1L
            val myNodeHandle = 2L
            val myName = "Name"
            val myText = "Text"
            val request = mock<MegaRequest> {
                on { email }.thenReturn(myEmail)
                on { parentHandle }.thenReturn(myParentHandle)
                on { nodeHandle }.thenReturn(myNodeHandle)
                on { name }.thenReturn(myName)
                on { text }.thenReturn(myText)
            }
            val user = mock<MegaUser> {
                on { email }.thenReturn(myEmail)
                on { visibility }.thenReturn(MegaUser.VISIBILITY_VISIBLE)
            }
            whenever(megaApiGateway.getContactLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface)
                    .onRequestFinish(mock(), request, success)
            }
            whenever(megaApiGateway.getContacts()).thenReturn(listOf(user))
            assertThat(underTest.getContactLink(1L)).isEqualTo(
                ContactLink(
                    isContact = true,
                    email = request.email,
                    contactHandle = request.parentHandle,
                    contactLinkHandle = request.nodeHandle,
                    fullName = "${request.name} ${request.text}"
                )
            )
        }

    @Test
    fun `test that getContactLink returns correctly when calling api gateway returns successfully and contacts return the list contain inactive user`() =
        runTest {
            val myEmail = "myEmail@mega.co.nz"
            val myParentHandle = 1L
            val myNodeHandle = 2L
            val myName = "Name"
            val myText = "Text"
            val request = mock<MegaRequest> {
                on { email }.thenReturn(myEmail)
                on { parentHandle }.thenReturn(myParentHandle)
                on { nodeHandle }.thenReturn(myNodeHandle)
                on { name }.thenReturn(myName)
                on { text }.thenReturn(myText)
            }
            val user = mock<MegaUser> {
                on { email }.thenReturn(myEmail)
                on { visibility }.thenReturn(MegaUser.VISIBILITY_INACTIVE)
            }
            whenever(megaApiGateway.getContactLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface)
                    .onRequestFinish(mock(), request, success)
            }
            whenever(megaApiGateway.getContacts()).thenReturn(listOf(user))
            assertThat(underTest.getContactLink(1L)).isEqualTo(
                ContactLink(
                    isContact = false,
                    email = request.email,
                    contactHandle = request.parentHandle,
                    contactLinkHandle = request.nodeHandle,
                    fullName = "${request.name} ${request.text}"
                )
            )
        }

    @Test
    fun `test that getContactLink returns correctly when calling api gateway returns successfully and contacts return the list empty`() =
        runTest {
            val myEmail = "myEmail@mega.co.nz"
            val myParentHandle = 1L
            val myNodeHandle = 2L
            val myName = "Name"
            val myText = "Text"
            val request = mock<MegaRequest> {
                on { email }.thenReturn(myEmail)
                on { parentHandle }.thenReturn(myParentHandle)
                on { nodeHandle }.thenReturn(myNodeHandle)
                on { name }.thenReturn(myName)
                on { text }.thenReturn(myText)
            }
            whenever(megaApiGateway.getContactLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface)
                    .onRequestFinish(mock(), request, success)
            }
            whenever(megaApiGateway.getContacts()).thenReturn(emptyList())
            assertThat(underTest.getContactLink(1L)).isEqualTo(
                ContactLink(
                    isContact = false,
                    email = request.email,
                    contactHandle = request.parentHandle,
                    contactLinkHandle = request.nodeHandle,
                    fullName = "${request.name} ${request.text}"
                )
            )
        }

    @Test
    fun `test that getContactLink returns correctly when calling api gateway returns API_EEXIST`() =
        runTest {
            val request = mock<MegaRequest>()
            val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) }
            whenever(megaApiGateway.getContactLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface)
                    .onRequestFinish(mock(), request, error)
            }
            assertThat(underTest.getContactLink(1L))
                .isEqualTo(ContactLink(isContact = false))
        }

    @Test(expected = MegaException::class)
    fun `test that getContactLink throws MegaException when calling api gateway returns failed`() =
        runTest {
            val request = mock<MegaRequest>()
            whenever(megaApiGateway.getContactLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface)
                    .onRequestFinish(mock(), request, error)
            }
            underTest.getContactLink(1L)
        }

    @Test
    fun `test that isContactRequestAlreadySent returns true when outgoingContactRequests contains email`() =
        runTest {
            val myEmail = "myEmail"
            val contactRequest = mock<MegaContactRequest> {
                on { targetEmail }.thenReturn(myEmail)
            }
            whenever(megaApiGateway.outgoingContactRequests()).thenReturn(arrayListOf(contactRequest))
            assertThat(underTest.isContactRequestSent(myEmail)).isTrue()
        }

    @Test
    fun `test that isContactRequestAlreadySent returns false when outgoingContactRequests doesn't contain email`() =
        runTest {
            val myEmail = "myEmail"
            val contactRequest = mock<MegaContactRequest> {
                on { targetEmail }.thenReturn("abc")
            }
            whenever(megaApiGateway.outgoingContactRequests()).thenReturn(arrayListOf(contactRequest))
            assertThat(underTest.isContactRequestSent(myEmail)).isFalse()
        }

    @Test
    fun `test that hasAnyContact returns true if has any contact`() = runTest {
        val contact = mock<MegaUser> {
            on { visibility } doReturn MegaUser.VISIBILITY_VISIBLE
        }
        whenever(megaApiGateway.getContacts()).thenReturn(listOf(mock(), contact, mock()))
        assertThat(underTest.hasAnyContact()).isTrue()
    }

    @Test
    fun `test that hasAnyContact returns false if does not has any contact`() = runTest {
        whenever(megaApiGateway.getContacts()).thenReturn(emptyList())
        assertThat(underTest.hasAnyContact()).isFalse()
    }

    @Test
    fun `test that monitorContactRemoved emits the user handles of removed contacts`() = runTest {
        val userHandle = 321L
        val myUserHandle = 123L
        val user1 = mock<MegaUser> {
            on { handle } doReturn userHandle
            on { changes } doReturn 0L
        }
        val user2 = mock<MegaUser> {
            on { handle } doReturn myUserHandle
            on { changes } doReturn MegaUser.CHANGE_TYPE_AVATAR.toLong()
        }
        whenever(megaApiGateway.myUserHandle).thenReturn(myUserHandle)
        whenever(megaApiGateway.globalUpdates).thenReturn(
            flowOf(GlobalUpdate.OnUsersUpdate(arrayListOf(user1, user2)))
        )
        underTest.monitorContactRemoved().test {
            val actual = awaitItem()
            awaitComplete()
            assertThat(actual).isEqualTo(listOf(userHandle))
        }
    }

    @Test
    fun `test that monitorNewContacts emits the user handles of new contacts`() = runTest {
        val userHandle = 321L
        val request1 = mock<MegaContactRequest> {
            on { handle } doReturn userHandle
            on { status } doReturn MegaContactRequest.STATUS_ACCEPTED
        }
        val request2 = mock<MegaContactRequest> {
            on { handle } doReturn 123L
            on { status } doReturn MegaContactRequest.STATUS_DELETED
        }
        whenever(megaApiGateway.globalUpdates).thenReturn(
            flowOf(GlobalUpdate.OnContactRequestsUpdate(arrayListOf(request1, mock(), request2)))
        )
        underTest.monitorNewContacts().test {
            val actual = awaitItem()
            awaitComplete()
            assertThat(actual).isEqualTo(listOf(userHandle))
        }
    }

    @Test
    fun `test that null is returned when the provided user in getContactUserNameFromDatabase is null`() =
        runTest {
            assertThat(underTest.getContactUserNameFromDatabase(null)).isNull()
        }

    @Test
    fun `test that the provided user is returned in getContactUserNameFromDatabase when getContact returns null`() =
        runTest {
            val user = "Test User"
            whenever(megaApiGateway.getContact(any())).thenReturn(null)
            assertThat(underTest.getContactUserNameFromDatabase(user)).isEqualTo(user)
        }

    @Test
    fun `test that the user name in the mega database is returned in getContactUserNameFromDatabase`() =
        runTest {
            val user = "Test User"
            val userNameInDatabase = "Test Username in Database"

            whenever(megaApiGateway.getContact(any())).thenReturn(mock<MegaUser>())
            whenever(contactWrapper.getMegaUserNameDB(any())).thenReturn(userNameInDatabase)
            assertThat(underTest.getContactUserNameFromDatabase(user)).isEqualTo(userNameInDatabase)
        }

    @Test
    fun `test that avatar cache update when user updates avatar`() = runTest {
        val otherUserHandle = 321L
        val myUserHandle = 123L
        val user = mock<MegaUser> {
            on { handle } doReturn otherUserHandle
            on { changes } doReturn MegaUser.CHANGE_TYPE_AVATAR.toLong()
            on { email } doReturn "email"
            on { hasChanged(MegaUser.CHANGE_TYPE_AVATAR.toLong()) } doReturn true
        }
        whenever(megaApiGateway.myUserHandle).thenReturn(myUserHandle)
        whenever(megaApiGateway.globalUpdates).thenReturn(
            flowOf(GlobalUpdate.OnUsersUpdate(arrayListOf(user)))
        )
        val userUpdate = UserUpdate(
            changes = mapOf(UserId(otherUserHandle) to listOf(UserChanges.Avatar)),
            emailMap = emptyMap(),
        )
        whenever(userUpdateMapper(listOf(user))).thenReturn(userUpdate)
        whenever(megaApiGateway.getContact(anyOrNull())).thenReturn(user)
        val cacheFile = mock<File> {
            on { absolutePath }.thenReturn(avatarUri)
        }
        whenever(cacheGateway.buildAvatarFile(any())).thenReturn(cacheFile)
        val avatarRequest = mock<MegaRequest> {
            on { type }.thenReturn(MegaRequest.TYPE_GET_ATTR_USER)
            on { paramType }.thenReturn(MegaApiJava.USER_ATTR_AVATAR)
            on { file }.thenReturn(avatarUri)
        }
        whenever(megaApiGateway.getContactAvatar(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface)
                .onRequestFinish(mock(), avatarRequest, success)
        }
        initRepository()
        underTest.monitorContactCacheUpdates.test {
            val actual = awaitItem()
            verify(megaApiGateway).getContactAvatar(any(), any(), any())
            assertThat(actual).isEqualTo(userUpdate)
        }
    }

    @Test
    fun `test that getUser returns null if megaApiGateway returns null`() = runTest {
        whenever(megaApiGateway.getContact(any())).thenReturn(null)
        val user = underTest.getUser(UserId(1L))
        assertThat(user).isNull()
    }

    @Test
    fun `test that getUser returns user if megaApiGateway returns user`() = runTest {
        val megaUser = mock<MegaUser>() {
            on { handle } doReturn 1L
            on { email } doReturn "email"
            on { visibility } doReturn MegaUser.VISIBILITY_VISIBLE
            on { changes } doReturn 0L
        }
        val expectedUser = User(
            handle = 1L,
            email = "email",
            visibility = UserVisibility.Visible,
            timestamp = 2L,
            userChanges = emptyList(),
        )
        whenever(megaApiGateway.getContact("email")).thenReturn(megaUser)
        whenever(megaApiGateway.userHandleToBase64(1L)).thenReturn("email")
        whenever(userMapper(megaUser)).thenReturn(expectedUser)
        assertThat(underTest.getUser(UserId(1L))).isEqualTo(expectedUser)
    }

    @Test
    fun `test that first name cache update when user updates first name`() = runTest {
        val myUserHandle = 123L
        val otherUserHandle = 321L
        val user = mock<MegaUser> {
            on { handle } doReturn otherUserHandle
            on { changes } doReturn MegaUser.CHANGE_TYPE_FIRSTNAME.toLong()
            on { email } doReturn "email"
            on { hasChanged(MegaUser.CHANGE_TYPE_FIRSTNAME.toLong()) } doReturn true
        }
        whenever(megaApiGateway.globalUpdates).thenReturn(
            flowOf(GlobalUpdate.OnUsersUpdate(arrayListOf(user)))
        )
        whenever(megaApiGateway.myUserHandle).thenReturn(myUserHandle)
        val userUpdate = UserUpdate(
            changes = mapOf(pair = UserId(otherUserHandle) to listOf(UserChanges.Firstname)),
            emailMap = emptyMap(),
        )
        whenever(userUpdateMapper(listOf(user))).thenReturn(userUpdate)
        whenever(megaApiGateway.getUserAttribute(anyOrNull<String>(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        initRepository()
        underTest.monitorContactCacheUpdates.test {
            val actual = awaitItem()
            verify(megaApiGateway).getUserAttribute(
                anyOrNull<String>(),
                eq(MegaApiJava.USER_ATTR_FIRSTNAME),
                any()
            )
            assertThat(actual).isEqualTo(userUpdate)
        }
    }

    @Test
    fun `test that last name cache update when user updates last name`() = runTest {
        val myUserHandle = 123L
        val otherUserHandle = 321L
        val user = mock<MegaUser> {
            on { handle } doReturn otherUserHandle
            on { changes } doReturn MegaUser.CHANGE_TYPE_LASTNAME.toLong()
            on { email } doReturn "email"
            on { hasChanged(MegaUser.CHANGE_TYPE_LASTNAME.toLong()) } doReturn true
        }
        whenever(megaApiGateway.globalUpdates).thenReturn(
            flowOf(GlobalUpdate.OnUsersUpdate(arrayListOf(user)))
        )
        whenever(megaApiGateway.myUserHandle).thenReturn(myUserHandle)
        val userUpdate = UserUpdate(
            changes = mapOf(UserId(otherUserHandle) to listOf(UserChanges.Lastname)),
            emailMap = emptyMap(),
        )
        whenever(userUpdateMapper(listOf(user))).thenReturn(userUpdate)
        whenever(megaApiGateway.getUserAttribute(anyOrNull<String>(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), request, success
            )
        }
        initRepository()
        underTest.monitorContactCacheUpdates.test {
            val actual = awaitItem()
            verify(megaApiGateway).getUserAttribute(
                anyOrNull<String>(),
                eq(MegaApiJava.USER_ATTR_LASTNAME),
                any()
            )
            assertThat(actual).isEqualTo(userUpdate)
        }
    }

    @Test
    fun `test that the correct contact is returned when the local contact is not null`() = runTest {
        val contactId = 123L
        val contact = Contact(userId = contactId, email = "email@test.com")
        whenever(megaLocalRoomGateway.monitorContactByHandle(contactId)) doReturn flow {
            emit(contact)
            awaitCancellation()
        }

        underTest.monitorContactByHandle(contactId).test {
            assertThat(awaitItem()).isEqualTo(contact)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitor online status by handle returns the initial status`() = runTest {
        val initialStatus = MegaChatApi.STATUS_ONLINE
        val mappedInitialStatus = UserChatStatus.Online
        whenever(userChatStatusMapper(initialStatus)) doReturn mappedInitialStatus
        whenever(megaChatApiGateway.getUserOnlineStatus(any())) doReturn initialStatus

        underTest.monitorOnlineStatusByHandle(1L).test {
            assertThat(awaitItem()).isEqualTo(mappedInitialStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that subsequent status changes are emitted`() = runTest {
        val initialStatus = MegaChatApi.STATUS_ONLINE
        val subsequentStatus = MegaChatApi.STATUS_BUSY
        val mappedInitialStatus = UserChatStatus.Online
        val mappedSubsequentStatus = UserChatStatus.Busy
        whenever(userChatStatusMapper(initialStatus)) doReturn mappedInitialStatus
        whenever(userChatStatusMapper(subsequentStatus)) doReturn mappedSubsequentStatus
        whenever(megaChatApiGateway.getUserOnlineStatus(any())) doReturn initialStatus
        whenever(megaChatApiGateway.chatUpdates) doReturn flow {
            emit(ChatUpdate.OnChatOnlineStatusUpdate(1L, subsequentStatus, false))
            awaitCancellation()
        }

        underTest.monitorOnlineStatusByHandle(1L).test {
            assertThat(awaitItem()).isEqualTo(mappedInitialStatus)
            assertThat(awaitItem()).isEqualTo(mappedSubsequentStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

}
