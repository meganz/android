package mega.privacy.android.app.contacts.usecase

import android.net.Uri
import androidx.core.net.toUri
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi.STATUS_ONLINE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.TestSchedulerExtension
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(
    InstantExecutorExtension::class,
    CoroutineMainDispatcherExtension::class,
    TestSchedulerExtension::class
)
class GetContactsUseCaseTest {
    private lateinit var underTest: GetContactsUseCase

    private val getChatChangesUseCase = mock<GetChatChangesUseCase>()
    private val megaContactsMapper = mock<(MegaUser, File) -> ContactItem.Data>()
    private val getContacts = mock<() -> ArrayList<MegaUser>>()
    private val getUserAttribute = mock<(String, Int, MegaRequestListenerInterface) -> Unit>()
    private val getContact = mock<(String) -> MegaUser?>()
    private val areCredentialsVerified = mock<(MegaUser) -> Boolean>()
    private val getUserFullnameFromCache = mock<(Long) -> String>()
    private val requestLastGreen = mock<(Long) -> Unit>()
    private val getChatRoomIdByUser = mock<(Long) -> Long?>()
    private val getUserAvatar = mock<(String, String, MegaRequestListenerInterface) -> Unit>()
    private val onlineString = mock<() -> String>()
    private val getUnformattedLastSeenDate = mock<(Int) -> String>()
    private val getAliasMap = mock<(MegaRequest) -> Map<Long, String>>()
    private val getUserUpdates = mock<() -> Flow<UserUpdate>>()

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        reset(
            getChatChangesUseCase,
            megaContactsMapper,
            getContacts,
            getUserAttribute,
            getContact,
            areCredentialsVerified,
            getUserFullnameFromCache,
            requestLastGreen,
            getChatRoomIdByUser,
            getUserAvatar,
            onlineString,
            getUnformattedLastSeenDate,
            getUserUpdates
        )

        getChatChangesUseCase.stub {
            on { get() } doReturn Flowable.empty()
        }


        getUserUpdates.stub {
            on { invoke() } doReturn emptyFlow()
        }
    }



    internal fun initUnderTest() {
        underTest = GetContactsUseCase(
            getChatChangesUseCase = getChatChangesUseCase,
            megaContactsMapper = megaContactsMapper,
            getContacts = getContacts,
            getUserAttribute = getUserAttribute,

            getContact = getContact,
            areCredentialsVerified = areCredentialsVerified,
            getUserFullnameFromCache = getUserFullnameFromCache,
            requestLastGreen = requestLastGreen,
            getChatRoomIdByUser = getChatRoomIdByUser,
            getUserAvatar = getUserAvatar,
            onlineString = onlineString,
            getUnformattedLastSeenDate = getUnformattedLastSeenDate,
            getAliasMap = getAliasMap,
            getUserUpdates = getUserUpdates,
        )
    }

    @Test
    fun `test that an empty list is returned if no contacts`() {
        getContacts.stub {
            on { invoke() } doReturn arrayListOf()
        }
        initUnderTest()

        underTest.get(tempDir).test().assertValue(emptyList())
    }

    @Test
    fun `test that a contact is returned if found`() {
        val megaUser = mock<MegaUser> {
            on { visibility } doReturn MegaUser.VISIBILITY_VISIBLE
        }
        val expected = mock<ContactItem.Data>()
        megaContactsMapper.stub {
            on { invoke(any(), any()) } doReturn expected
        }
        getContacts.stub {
            on { invoke() } doReturn arrayListOf(megaUser)
        }

        initUnderTest()

        underTest.get(tempDir).test().assertValue(listOf(expected))
    }

    @Test
    fun `test that contact is removed if visibility changes`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val megaUser = mock<MegaUser> {
            on { handle } doReturn userHandle
            on { visibility }.thenReturn(MegaUser.VISIBILITY_VISIBLE, MegaUser.VISIBILITY_HIDDEN)
        }
        val expected = mock<ContactItem.Data> {
            on { handle } doReturn userHandle
            on { email } doReturn userEmail
        }
        megaContactsMapper.stub {
            on { invoke(any(), any()) } doReturn expected
        }
        getContacts.stub {
            on { invoke() } doReturn arrayListOf(megaUser)
        }

        stubGlobalUpdate(userEmail, userHandle, emptyList(), MegaUser.VISIBILITY_HIDDEN)

        initUnderTest()

        underTest.get(tempDir).test().assertValues(listOf(expected), emptyList())
    }

    @Test
    fun `test that first name is updated when changed`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val initial = "initial"
        val expected = "Expected"
        val attributeType = MegaApiJava.USER_ATTR_FIRSTNAME
        val changes = listOf(MegaUser.CHANGE_TYPE_FIRSTNAME)

        stubContact(
            userHandle = userHandle,
            userEmail = userEmail,
            fullName = initial,
        )
        stubUserAttribute(attributeType = attributeType, userEmail = userEmail)
        stubGlobalUpdate(userEmail, userHandle, changes)
        getUserFullnameFromCache.stub {
            on { invoke(userHandle) } doReturn expected
        }

        initUnderTest()

        underTest.get(tempDir).map { it.first().fullName ?: "NO NAME" }.test()
            .assertValues(initial, expected)
    }

    @Test
    fun `test that last name is updated when changed`() = runTest {
        val userHandle = 1L
        val userEmail = "email"
        val initial = "initial"
        val expected = "Expected"
        val attributeType = MegaApiJava.USER_ATTR_LASTNAME
        val changes = listOf(MegaUser.CHANGE_TYPE_LASTNAME)

        stubContact(
            userHandle = userHandle,
            userEmail = userEmail,
            fullName = initial,
        )
        stubUserAttribute(attributeType = attributeType, userEmail = userEmail)
        stubGlobalUpdate(userEmail, userHandle, changes)
        getUserFullnameFromCache.stub {
            on { invoke(userHandle) } doReturn expected
        }

        initUnderTest()

        underTest.get(tempDir).map { it.first().fullName ?: "NO NAME" }.test()
            .assertValues(initial, expected)
    }

    @Test
    fun `test that avatar is updated when changed`() {
        val userEmail = "email"
        val userHandle = 1L
        stubContact(userHandle = userHandle, userEmail = userEmail)
        val file = "file"
        stubUserAttribute(
            attributeType = MegaApiJava.USER_ATTR_AVATAR,
            userEmail = userEmail,
            fileName = file
        )
        stubGlobalUpdate(userEmail, userHandle, listOf(MegaUser.CHANGE_TYPE_AVATAR))

        initUnderTest()

        underTest.get(tempDir).test()
            .assertValueAt(0) { it.first().avatarUri == Uri.EMPTY }
            .assertValueAt(1) { it.first().avatarUri == File(file).toUri() }
    }

    @Test
    fun `test that alias is updated when changed`() {
        val userEmail = "email"
        val notUserEmail = userEmail + "Not"
        val userHandle = 1L
        val notUserHandle = userHandle + 1
        val oldAlias = "alias"

        stubContact(userHandle = userHandle, userEmail = userEmail, alias = oldAlias)
        stubGlobalUpdate(notUserEmail, notUserHandle, listOf(MegaUser.CHANGE_TYPE_ALIAS))

        val newAlias = "new alias"
        stubUserAttribute(
            attributeType = MegaApiJava.USER_ATTR_ALIAS,
            userEmail = notUserEmail,
            alias = newAlias
        )
        getAliasMap.stub {
            on { invoke(any()) } doReturn mapOf(userHandle to newAlias)
        }

        initUnderTest()

        underTest.get(tempDir).test()
            .assertValueAt(0) { it.first().alias == oldAlias }
            .assertValueAt(1) { it.first().alias == newAlias }
    }

    @Test
    fun `test that new user is added if a visible change is returned from user changes`() {
        val userHandle = 1L
        val userEmail = "email"
        val megaUser = mock<MegaUser> {
            on { email } doReturn userEmail
            on { handle } doReturn userHandle
            on { visibility }.thenReturn(MegaUser.VISIBILITY_VISIBLE)
        }
        val expected = mock<ContactItem.Data> {
            on { handle } doReturn userHandle
        }
        megaContactsMapper.stub {
            on { invoke(any(), any()) } doReturn expected
        }
        getContacts.stub {
            on { invoke() }.doReturn(arrayListOf(), arrayListOf(megaUser))
        }

        stubGlobalUpdate(userEmail, userHandle, listOf(MegaUser.VISIBILITY_HIDDEN))

        initUnderTest()

        underTest.get(tempDir).test().assertValues(emptyList(), listOf(expected))
    }

    private fun stubGlobalUpdate(
        userEmail: String,
        userHandle: Long,
        changes: List<Int>,
        userVisibility: Int = MegaUser.VISIBILITY_VISIBLE,
    ) {
        getUserUpdates.stub {
            on { invoke() } doReturn flow {
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
        userVisibility: Int,
    ) = mapOf(
        UserId(userHandle) to (mapChanges(changes).takeUnless { it.isEmpty() }
            ?: listOf(
                UserChanges.Visibility(
                    when (userVisibility) {
                        MegaUser.VISIBILITY_HIDDEN -> UserVisibility.Hidden
                        MegaUser.VISIBILITY_VISIBLE -> UserVisibility.Visible
                        MegaUser.VISIBILITY_BLOCKED -> UserVisibility.Blocked
                        MegaUser.VISIBILITY_UNKNOWN -> UserVisibility.Unknown
                        MegaUser.VISIBILITY_INACTIVE -> UserVisibility.Inactive
                        else -> UserVisibility.Unknown
                    }
                )
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

    private fun stubContact(
        userHandle: Long,
        userEmail: String,
        fullName: String? = "name",
        alias: String = "alias",
    ) {
        val megaUser = mock<MegaUser> {
            on { visibility }.thenReturn(MegaUser.VISIBILITY_VISIBLE)
        }

        val item = ContactItem.Data(
            handle = userHandle,
            email = userEmail,
            placeholder = mock(),
            fullName = fullName,
            avatarUri = Uri.EMPTY,
            alias = alias,
            status = STATUS_ONLINE,
        )
        megaContactsMapper.stub {
            on { invoke(any(), any()) } doReturn item
        }
        getContacts.stub {
            on { invoke() } doReturn arrayListOf(megaUser)
        }
    }

    private fun stubUserAttribute(
        attributeType: Int,
        userEmail: String,
        fileName: String? = null,
        alias: String? = null,
    ) {
        val request = mock<MegaRequest> {
            on { paramType } doReturn attributeType
            on { email } doReturn userEmail
            on { file } doReturn fileName
            on { text } doReturn alias
        }
        val error = mock<MegaError> {
            on { errorCode } doReturn MegaError.API_OK
        }
        val api = mock<MegaApiJava>()


        getUserAttribute.stub {
            on { invoke(any(), eq(attributeType), any()) }.thenAnswer {
                val listener = it.getArgument<MegaRequestListenerInterface>(2)
                listener.onRequestFinish(api, request, error)
            }
        }
    }
}