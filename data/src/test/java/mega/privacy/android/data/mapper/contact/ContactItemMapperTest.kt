package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaUser
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ContactItemMapperTest {
    private lateinit var underTest: ContactItemMapper

    private var userChatStatusMapper = UserChatStatusMapper()

    private lateinit var user: MegaUser
    private val expectedFullName = "Clark Kent"
    private val expectedAlias = "Superman"
    private val expectedAvatar = "<S>"
    private val expectedContactData = ContactData(expectedFullName, expectedAlias, expectedAvatar)
    private val userHandle = 1L
    private val userEmail = "superman@kripton.com"
    private val userTimestamp = 100L
    private val avatarColor = "color"
    private val status = MegaChatApi.STATUS_ONLINE

    @Before
    fun setUp() {
        user = mock {
            on { handle }.thenReturn(userHandle)
            on { email }.thenReturn(userEmail)
            on { timestamp }.thenReturn(userTimestamp)
        }
        underTest = ContactItemMapper(userChatStatusMapper)
    }

    @Test
    fun `test ContactItem handle is mapped from MegaUser handle`() {
        val actual = underTest.invoke(
            megaUser = user,
            contactData = expectedContactData,
            defaultAvatarColor = avatarColor,
            areCredentialsVerified = true,
            status = status,
            lastSeen = null
        )
        Truth.assertThat(actual.handle).isEqualTo(userHandle)
    }

    @Test
    fun `test ContactItem email is mapped from MegaUser email`() {
        val actual = underTest.invoke(
            megaUser = user,
            contactData = expectedContactData,
            defaultAvatarColor = avatarColor,
            areCredentialsVerified = true,
            status = status,
            lastSeen = null
        )
        Truth.assertThat(actual.email).isEqualTo(userEmail)
    }

    @Test
    fun `test ContactItem timestamp is mapped from MegaUser timestamp`() {
        val actual = underTest.invoke(
            megaUser = user,
            contactData = expectedContactData,
            defaultAvatarColor = avatarColor,
            areCredentialsVerified = true,
            status = status,
            lastSeen = null
        )
        Truth.assertThat(actual.timestamp).isEqualTo(userTimestamp)
    }

    @Test
    fun `test ContactItem visibility is mapped from MegaUser visibility`() {
        UserVisibility.values().forEach { visibility ->
            val sdkValue =
                ContactItemMapper.userVisibility.entries.find { it.value == visibility }?.key
            whenever(user.visibility).thenReturn(sdkValue)
            val actual =
                underTest.invoke(
                    megaUser = user,
                    contactData = expectedContactData,
                    defaultAvatarColor = avatarColor,
                    areCredentialsVerified = true,
                    status = status,
                    lastSeen = null
                )
            Truth.assertThat(actual.visibility).isEqualTo(visibility)
        }
    }

    @Test
    fun `test ContactItem visibility is mapped to unknown if MegaUser visibility is unknown`() {
        whenever(user.visibility).thenReturn(-55)
        val actual =
            underTest.invoke(
                megaUser = user,
                contactData = expectedContactData,
                defaultAvatarColor = avatarColor,
                areCredentialsVerified = true,
                status = status,
                lastSeen = null
            )
        Truth.assertThat(actual.visibility).isEqualTo(UserVisibility.Unknown)
    }

    @Test
    fun `test ContactItem status is mapped from status`() {
        listOf(
            MegaChatApi.STATUS_OFFLINE,
            MegaChatApi.STATUS_AWAY,
            MegaChatApi.STATUS_ONLINE,
            MegaChatApi.STATUS_BUSY,
            MegaChatApi.STATUS_INVALID,
        ).forEach { status ->
            val actual = underTest.invoke(
                megaUser = user,
                contactData = expectedContactData,
                defaultAvatarColor = avatarColor,
                areCredentialsVerified = true,
                status = status,
                lastSeen = null
            )
            val expectedUserChatStatus = userChatStatusMapper(status)

            Truth.assertThat(actual.status).isEqualTo(expectedUserChatStatus)
        }

    }
}