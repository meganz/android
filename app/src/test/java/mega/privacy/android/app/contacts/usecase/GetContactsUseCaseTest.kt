package mega.privacy.android.app.contacts.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserLastGreen
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.TestSchedulerExtension
import java.time.LocalDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(
    InstantExecutorExtension::class,
    CoroutineMainDispatcherExtension::class,
    TestSchedulerExtension::class
)
class GetContactsUseCaseTest {
    private lateinit var underTest: GetContactsUseCase

    private val contactsRepository = mock<ContactsRepository>()
    private val chatRepository = mock<ChatRepository>()
    private val accountRepository = mock<AccountRepository>()

    @BeforeEach
    fun setUp() {
        reset(
            chatRepository,
            accountRepository,
            contactsRepository,
        )

        accountRepository.stub {
            on { monitorUserUpdates() } doReturn emptyFlow()
        }

        contactsRepository.stub {
            onBlocking { getVisibleContacts() } doReturn emptyList()
            on { monitorChatPresenceLastGreenUpdates() } doReturn emptyFlow()
            on { monitorChatOnlineStatusUpdates() } doReturn emptyFlow()
            on { monitorChatConnectionStateUpdates() } doReturn emptyFlow()
        }
    }


    internal fun initUnderTest() {
        underTest = GetContactsUseCase(
            accountsRepository = accountRepository,
            contactsRepository = contactsRepository,
            chatRepository = chatRepository
        )
    }

    @Test
    fun `test that an empty list is returned if no contacts`() = runTest {
        contactsRepository.stub {
            onBlocking { getVisibleContacts() } doReturn emptyList()
        }

        initUnderTest()

        underTest().test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `test that a contact is returned if found`() = runTest {
        val expected = stubContact(userHandle = 1, userEmail = "email")

        initUnderTest()

        underTest().test {
            assertThat(awaitItem()).containsExactly(expected)
        }
    }

    @Test
    fun `test that contact is removed if visibility changes`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val contactItem = mock<ContactItem> {
            on { handle } doReturn userHandle
            on { email } doReturn userEmail
            on { visibility } doReturn UserVisibility.Visible
        }
        stubContactsList(listOf(contactItem), emptyList())

        stubGlobalUpdate(
            userEmail = userEmail,
            userHandle = userHandle,
            changes = emptyList(),
            userVisibility = UserVisibility.Hidden,
        )

        initUnderTest()

        underTest().test {
            assertThat(awaitItem()).isNotEmpty()
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `test that first name is updated when changed`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val initial = ContactData(fullName = "Initial", alias = "alias", avatarUri = null)
        val expected = ContactData(fullName = "Expected", alias = "alias", avatarUri = null)
        val changes = listOf(MegaUser.CHANGE_TYPE_FIRSTNAME)

        stubContact(
            userHandle = userHandle,
            userEmail = userEmail,
            fullName = initial.fullName,
            alias = initial.alias!!,
        )
        stubGlobalUpdate(userEmail, userHandle, changes)

        contactsRepository.stub {
            onBlocking { getContactData(any()) } doReturn expected
        }

        initUnderTest()

        underTest().map { it.first().contactData.fullName.orEmpty() }.test {
            assertThat(awaitItem()).isEqualTo(initial.fullName)
            assertThat(awaitItem()).isEqualTo(expected.fullName)
        }
    }

    @Test
    fun `test that last name is updated when changed`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val initial = ContactData(fullName = "Initial", alias = "alias", avatarUri = null)
        val expected = ContactData(fullName = "Expected", alias = "alias", avatarUri = null)
        val changes = listOf(MegaUser.CHANGE_TYPE_LASTNAME)

        stubContact(
            userHandle = userHandle,
            userEmail = userEmail,
            fullName = initial.fullName,
            alias = initial.alias!!,
        )
        stubGlobalUpdate(userEmail, userHandle, changes)

        contactsRepository.stub {
            onBlocking { getContactData(any()) } doReturn expected
        }

        initUnderTest()

        underTest().map { it.first().contactData.fullName.orEmpty() }.test {
            assertThat(awaitItem()).isEqualTo(initial.fullName)
            assertThat(awaitItem()).isEqualTo(expected.fullName)
        }
    }

    @Test
    fun `test that avatar is updated when changed`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val initial = ContactData(fullName = "Initial", alias = "alias", avatarUri = null)
        val newUri = "newUri"
        val expected = ContactData(fullName = "Expected", alias = "alias", avatarUri = newUri)
        val changes = listOf(MegaUser.CHANGE_TYPE_FIRSTNAME)

        stubContact(
            userHandle = userHandle,
            userEmail = userEmail,
            fullName = initial.fullName,
            alias = initial.alias!!,
        )
        stubGlobalUpdate(userEmail, userHandle, changes)

        contactsRepository.stub {
            onBlocking { getContactData(any()) } doReturn expected
        }

        initUnderTest()

        underTest().test {
            assertThat(awaitItem().first().contactData.avatarUri).isEqualTo(null)
            assertThat(awaitItem().first().contactData.avatarUri).isEqualTo(newUri)
        }
    }

    @Test
    fun `test that alias is updated when changed`() = runTest {
        val userEmail = "email"
        val notUserEmail = userEmail + "Not"
        val userHandle = 1L
        val notUserHandle = userHandle + 1
        val oldAlias = ContactData(
            fullName = "name", alias = "oldAlias", avatarUri = null
        )

        stubContact(
            userHandle = userHandle, userEmail = userEmail, alias = oldAlias.alias.orEmpty()
        )
        stubGlobalUpdate(notUserEmail, notUserHandle, listOf(MegaUser.CHANGE_TYPE_ALIAS))

        val newAlias = ContactData(
            fullName = "name", alias = "newAlias", avatarUri = null
        )
        contactsRepository.stub {
            onBlocking { getContactData(any()) } doReturn newAlias
        }
        initUnderTest()

        underTest().test {
            assertThat(awaitItem().first().contactData.alias).isEqualTo(oldAlias.alias)
            assertThat(awaitItem().first().contactData.alias).isEqualTo(newAlias.alias)
        }
    }

    @Test
    fun `test that new user is added if a visible change is returned from user changes`() =
        runTest {
            val userHandle = 1L
            val userEmail = "email"
            val contactItem = mock<ContactItem> {
                on { handle } doReturn userHandle
                on { email } doReturn userEmail
            }
            stubContactsList(emptyList(), listOf(contactItem))

            stubGlobalUpdate(
                userEmail = userEmail,
                userHandle = userHandle,
                changes = emptyList(),
                userVisibility = UserVisibility.Visible
            )

            initUnderTest()

            underTest().test {
                assertThat(awaitItem()).isEmpty()
                assertThat(awaitItem()).isNotEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that chat online update with online status updates the contact status`() = runTest {
        val userHandle = 1
        val lastSeen = 42
        stubContact(userHandle = userHandle.toLong(), userEmail = "email", lastSeen = lastSeen)

        val expectedStatus = UserChatStatus.Online
        stubOnlineStatusUpdate(userHandle = userHandle, status = expectedStatus)

        initUnderTest()

        underTest().test {
            awaitItem()
            val actual = awaitItem().first()
            assertThat(actual.status).isEqualTo(expectedStatus)
            assertThat(actual.lastSeen).isEqualTo(lastSeen)
        }
    }

    @Test
    fun `test that last green updates update the last seen field`() = runTest {
        val userHandle = 1L
        stubContact(userHandle = userHandle, userEmail = "email")

        val expectedLastGreen = 123456
        stubLastGreenUpdate(userHandle = userHandle, lastGreen = expectedLastGreen)

        initUnderTest()

        underTest().test {
            awaitItem()
            assertThat(awaitItem().first().lastSeen).isEqualTo(expectedLastGreen)
        }
    }

    @Test
    fun `test that chat connection update sets contact is new status to false`() = runTest {
        val userHandle = 1
        stubContact(userHandle = userHandle.toLong(), userEmail = "email", isNew = true)

        val expectedChatId = 123L
        val expectedState = MegaChatApi.CHAT_CONNECTION_ONLINE

        val chatRoom = mock<ChatRoom> {
            on { chatId } doReturn expectedChatId
        }
        chatRepository.stub {
            onBlocking { getChatRoomByUser(userHandle.toLong()) } doReturn chatRoom
        }

        stubChatConnectionUpdate(expectedChatId, expectedState)

        initUnderTest()

        underTest().test {
            assertThat(awaitItem().first().chatroomId).isNull()
            assertThat(awaitItem().first().chatroomId).isNotNull()
        }
    }

    @Test
    fun `test that authentication update fetches the contacts again`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val contactItem = mock<ContactItem> {
            on { handle } doReturn userHandle
            on { email } doReturn userEmail
            on { areCredentialsVerified }.doReturn(false, true)
            on { visibility } doReturn UserVisibility.Visible
        }
        stubContactsList(listOf(contactItem), listOf(contactItem))

        stubGlobalUpdate(
            userEmail = userEmail,
            userHandle = userHandle,
            changes = listOf(MegaUser.CHANGE_TYPE_AUTHRING),
        )

        initUnderTest()

        verifyNoInteractions(contactsRepository)

        underTest().test { cancelAndIgnoreRemainingEvents() }

        verifyBlocking(contactsRepository, times(2)) { getVisibleContacts() }
    }

    private fun stubChatConnectionUpdate(expectedChatId: Long, expectedState: Int) {
        contactsRepository.stub {
            on { monitorChatConnectionStateUpdates() } doReturn flow {
                emit(
                    ChatConnectionState(
                        chatId = expectedChatId,
                        chatConnectionStatus = getConnectedStatusFromInt(expectedState)
                    )
                )
                awaitCancellation()
            }
        }
    }

    private fun getConnectedStatusFromInt(expectedState: Int) = when (expectedState) {
        MegaChatApi.CHAT_CONNECTION_OFFLINE -> ChatConnectionStatus.Offline
        MegaChatApi.CHAT_CONNECTION_IN_PROGRESS -> ChatConnectionStatus.InProgress
        MegaChatApi.CHAT_CONNECTION_ONLINE -> ChatConnectionStatus.Online
        else -> ChatConnectionStatus.Unknown
    }

    private fun stubLastGreenUpdate(userHandle: Long, lastGreen: Int) {
        contactsRepository.stub {
            on { monitorChatPresenceLastGreenUpdates() } doReturn flow {
                emit(
                    UserLastGreen(
                        handle = userHandle, lastGreen = lastGreen
                    )
                )
                awaitCancellation()
            }
        }
    }

    private fun stubOnlineStatusUpdate(userHandle: Int, status: UserChatStatus) {
        contactsRepository.stub {
            on { monitorChatOnlineStatusUpdates() } doReturn flow {
                emit(
                    OnlineStatus(
                        userHandle = userHandle.toLong(),
                        status = status,
                        inProgress = false
                    )
                )
                awaitCancellation()
            }
        }
    }

    private fun stubGlobalUpdate(
        userEmail: String,
        userHandle: Long,
        changes: List<Int>,
        userVisibility: UserVisibility = UserVisibility.Visible,
    ) {
        accountRepository.stub {
            on { monitorUserUpdates() } doReturn flow {
                emit(
                    UserUpdate(
                        changes = stubChangesList(userHandle, changes, userVisibility),
                        emailMap = mapOf(UserId(userHandle) to userEmail)
                    )
                )

                awaitCancellation()
            }
        }
    }

    private fun stubChangesList(
        userHandle: Long,
        changes: List<Int>,
        userVisibility: UserVisibility,
    ) = mapOf(
        UserId(userHandle) to (mapChanges(changes).takeUnless { it.isEmpty() }
            ?: listOf(
                UserChanges.Visibility(userVisibility)
            ))
    )

    private fun mapChanges(changes: List<Int>) = changes.mapNotNull {
        when (it) {
            MegaUser.CHANGE_TYPE_AUTHRING -> UserChanges.AuthenticationInformation
            MegaUser.CHANGE_TYPE_LSTINT -> UserChanges.LastInteractionTimestamp
            MegaUser.CHANGE_TYPE_AVATAR -> UserChanges.Avatar
            MegaUser.CHANGE_TYPE_FIRSTNAME -> UserChanges.Firstname
            MegaUser.CHANGE_TYPE_LASTNAME -> UserChanges.Lastname
            MegaUser.CHANGE_TYPE_EMAIL -> UserChanges.Email
            MegaUser.CHANGE_TYPE_KEYRING -> UserChanges.Keyring
            MegaUser.CHANGE_TYPE_COUNTRY -> UserChanges.Country
            MegaUser.CHANGE_TYPE_BIRTHDAY -> UserChanges.Birthday
            MegaUser.CHANGE_TYPE_PUBKEY_CU255 -> UserChanges.ChatPublicKey
            MegaUser.CHANGE_TYPE_PUBKEY_ED255 -> UserChanges.SigningPublicKey
            MegaUser.CHANGE_TYPE_SIG_PUBKEY_RSA -> UserChanges.RsaPublicKeySignature
            MegaUser.CHANGE_TYPE_SIG_PUBKEY_CU255 -> UserChanges.ChatPublicKeySignature
            MegaUser.CHANGE_TYPE_LANGUAGE -> UserChanges.Language
            MegaUser.CHANGE_TYPE_PWD_REMINDER -> UserChanges.PasswordReminder
            MegaUser.CHANGE_TYPE_DISABLE_VERSIONS -> UserChanges.DisableVersions
            MegaUser.CHANGE_TYPE_CONTACT_LINK_VERIFICATION -> UserChanges.ContactLinkVerification
            MegaUser.CHANGE_TYPE_RICH_PREVIEWS -> UserChanges.RichPreviews
            MegaUser.CHANGE_TYPE_RUBBISH_TIME -> UserChanges.RubbishTime
            MegaUser.CHANGE_TYPE_STORAGE_STATE -> UserChanges.StorageState
            MegaUser.CHANGE_TYPE_GEOLOCATION -> UserChanges.Geolocation
            MegaUser.CHANGE_TYPE_CAMERA_UPLOADS_FOLDER -> UserChanges.CameraUploadsFolder
            MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER -> UserChanges.MyChatFilesFolder
            MegaUser.CHANGE_TYPE_PUSH_SETTINGS -> UserChanges.PushSettings
            MegaUser.CHANGE_TYPE_ALIAS -> UserChanges.Alias
            MegaUser.CHANGE_TYPE_UNSHAREABLE_KEY -> UserChanges.UnshareableKey
            MegaUser.CHANGE_TYPE_DEVICE_NAMES -> UserChanges.DeviceNames
            MegaUser.CHANGE_TYPE_MY_BACKUPS_FOLDER -> UserChanges.MyBackupsFolder
            MegaUser.CHANGE_TYPE_COOKIE_SETTINGS -> UserChanges.CookieSettings
            MegaUser.CHANGE_TYPE_NO_CALLKIT -> UserChanges.NoCallkit
            else -> null
        }
    }

    private fun stubContactsList(list: List<ContactItem>, vararg lists: List<ContactItem>) {
        contactsRepository.stub {
            onBlocking { getVisibleContacts() }.doReturn(list, *lists)
        }
    }

    private fun stubContact(
        userHandle: Long,
        userEmail: String,
        fullName: String? = "name",
        alias: String = "alias",
        isNew: Boolean = false,
        lastSeen: Int? = null,
    ): ContactItem {
        val domainContact = ContactItem(
            handle = userHandle,
            email = userEmail,
            contactData = ContactData(
                fullName = fullName, alias = alias, avatarUri = null
            ),
            defaultAvatarColor = null,
            visibility = UserVisibility.Visible,
            lastSeen = lastSeen,
            timestamp = if (isNew) LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(0)) else 0L,
            status = UserChatStatus.Offline,
            areCredentialsVerified = false,
            chatroomId = null,
        )
        contactsRepository.stub {
            onBlocking { getVisibleContacts() } doReturn listOf(domainContact)
        }

        return domainContact
    }
}