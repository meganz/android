package mega.privacy.android.app.contacts.mapper

import android.graphics.drawable.Drawable
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ContactItemDataMapperTest {
    private lateinit var underTest: ContactItemDataMapper

    private val getUnformattedLastSeenDate = mock<(Int) -> String> {
        on { invoke(any()) } doReturn "default"
    }
    private val getPlaceHolderDrawable = mock<(title: String, colour: Int) -> Drawable> {
        on {
            invoke(
                any(), any()
            )
        } doReturn mock()
    }

    private val wasRecentlyAdded: (Long) -> Boolean = { it > 0 }

    @BeforeEach
    internal fun setUp() {
        underTest = ContactItemDataMapper(
            getUnformattedLastSeenDate = getUnformattedLastSeenDate,
            getPlaceHolderDrawable = getPlaceHolderDrawable,
            wasRecentlyAdded = wasRecentlyAdded
        )
    }

    @Test
    fun `test that copied values are assigned to the correct field`() {
        val expectedHandle = 123L
        val expectedEmail = "email"
        val expectedFullName = "fullName"
        val expectedAlias = "alias"
        val expectedAvatarUri = "avatarUri"
        val expectedIsVerified = true

        val input = getInput(
            expectedHandle = expectedHandle,
            expectedEmail = expectedEmail,
            expectedFullName = expectedFullName,
            expectedAlias = expectedAlias,
            expectedAvatarUri = expectedAvatarUri,
            expectedIsVerified = expectedIsVerified,
        )

        val actual = underTest(contactItem = input)

        assertThat(actual.id).isEqualTo(expectedHandle)
        assertThat(actual.email).isEqualTo(expectedEmail)
        assertThat(actual.fullName).isEqualTo(expectedFullName)
        assertThat(actual.alias).isEqualTo(expectedAlias)
        assertThat(actual.avatarUri).isEqualTo(expectedAvatarUri.toUri())
        assertThat(actual.isVerified).isEqualTo(expectedIsVerified)
    }

    @ParameterizedTest
    @EnumSource(UserChatStatus::class)
    fun `test that status is correctly mapped`(status: UserChatStatus) {
        val input = getInput(
            userChatStatus = status,
        )

        val expected = mapOf(
            UserChatStatus.Online to MegaChatApi.STATUS_ONLINE,
            UserChatStatus.Away to MegaChatApi.STATUS_AWAY,
            UserChatStatus.Busy to MegaChatApi.STATUS_BUSY,
            UserChatStatus.Offline to MegaChatApi.STATUS_OFFLINE,
            UserChatStatus.Invalid to MegaChatApi.STATUS_INVALID,
        )

        val actual = underTest(contactItem = input)

        assertThat(actual.status).isEqualTo(expected[status])
    }

    @Test
    fun `test that a recent contact with no conversation is marked as new`() {
        val input = getInput(isRecentlyAdded = true, hasChatRoom = false)

        val actual = underTest(contactItem = input)

        assertThat(actual.isNew).isTrue()
    }

    @Test
    fun `test that a new contact with a conversation is not marked as new`() {
        val input = getInput(isRecentlyAdded = true, hasChatRoom = true)

        val actual = underTest(contactItem = input)

        assertThat(actual.isNew).isFalse()
    }

    @Test
    fun `test that an old contact with no conversation is not marked as new`() {
        val input = getInput(isRecentlyAdded = false, hasChatRoom = false)

        val actual = underTest(contactItem = input)

        assertThat(actual.isNew).isFalse()
    }

    @Test
    fun `test that an old contact with a conversation is not marked as new`() {
        val input = getInput(isRecentlyAdded = false, hasChatRoom = true)

        val actual = underTest(contactItem = input)

        assertThat(actual.isNew).isFalse()
    }

    private fun getInput(
        expectedHandle: Long = 123L,
        expectedEmail: String = "email",
        expectedFullName: String = "fullName",
        expectedAlias: String = "alias",
        expectedAvatarUri: String = "avatarUri",
        expectedIsVerified: Boolean = true,
        userChatStatus: UserChatStatus = UserChatStatus.Online,
        isRecentlyAdded: Boolean = false,
        defaultAvatarColour: String? = null,
        lastSeen: Int? = null,
        hasChatRoom: Boolean = false,
    ): ContactItem {

        return ContactItem(
            handle = expectedHandle,
            email = expectedEmail,
            contactData = ContactData(
                fullName = expectedFullName,
                alias = expectedAlias,
                avatarUri = expectedAvatarUri,
            ),
            defaultAvatarColor = defaultAvatarColour,
            visibility = UserVisibility.Visible,
            timestamp = if (isRecentlyAdded) 1L else 0L,
            areCredentialsVerified = expectedIsVerified,
            status = userChatStatus,
            lastSeen = lastSeen,
            chatroomId = if (hasChatRoom) 1L else null,
        )
    }

}