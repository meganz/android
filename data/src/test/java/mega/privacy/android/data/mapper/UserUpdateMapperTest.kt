package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import nz.mega.sdk.MegaUser
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserUpdateMapperTest {

    @Test
    fun `test that a user the user id is mapped to the change key`() {
        val id1 = 1L
        val id2 = 2L
        val user1 = mock<MegaUser> {
            on { handle }.thenReturn(id1)
        }
        val user2 = mock<MegaUser> {
            on { handle }.thenReturn(id2)
        }

        val userList = arrayListOf(user1, user2)
        val actual = mapMegaUserListToUserUpdate(userList)
        assertThat(actual.changes).containsKey(UserId(id1))
        assertThat(actual.changes).containsKey(UserId(id2))
    }

    @Test
    fun `test mapping single change`() {
        val id = 1L
        val user = mock<MegaUser> { on { handle }.thenReturn(id) }
        whenever(user.changes).thenReturn(MegaUser.CHANGE_TYPE_ALIAS)

        val actual = mapMegaUserListToUserUpdate(listOf(user))

        assertThat(actual.changes[UserId(id)]).containsExactly(UserChanges.Alias)

    }

    @Test
    fun `test that duplicate users are combined`() {
        val id1 = 1L
        val id2 = 2L
        val user1 = mock<MegaUser> {
            on { handle }.thenReturn(id1)
            on { changes }.thenReturn(MegaUser.CHANGE_TYPE_ALIAS, MegaUser.CHANGE_TYPE_AVATAR)
        }
        val user2 = mock<MegaUser> {
            on { handle }.thenReturn(id2)
            on { changes }.thenReturn(MegaUser.CHANGE_TYPE_ALIAS)
        }

        val userList = arrayListOf(user1, user2, user1)
        val actual = mapMegaUserListToUserUpdate(userList)

        assertThat(actual.changes.size).isEqualTo(2)
        assertThat(actual.changes[UserId(id1)]).containsExactly(
            UserChanges.Alias,
            UserChanges.Avatar
        )
        assertThat(actual.changes[UserId(id2)]).containsExactly(UserChanges.Alias)
    }

    @Test
    fun `test mapping all changes`() {
        val allFlags = listOf(
            MegaUser.CHANGE_TYPE_AUTHRING,
            MegaUser.CHANGE_TYPE_LSTINT,
            MegaUser.CHANGE_TYPE_AVATAR,
            MegaUser.CHANGE_TYPE_FIRSTNAME,
            MegaUser.CHANGE_TYPE_LASTNAME,
            MegaUser.CHANGE_TYPE_EMAIL,
            MegaUser.CHANGE_TYPE_KEYRING,
            MegaUser.CHANGE_TYPE_COUNTRY,
            MegaUser.CHANGE_TYPE_BIRTHDAY,
            MegaUser.CHANGE_TYPE_PUBKEY_CU255,
            MegaUser.CHANGE_TYPE_PUBKEY_ED255,
            MegaUser.CHANGE_TYPE_SIG_PUBKEY_RSA,
            MegaUser.CHANGE_TYPE_SIG_PUBKEY_CU255,
            MegaUser.CHANGE_TYPE_LANGUAGE,
            MegaUser.CHANGE_TYPE_PWD_REMINDER,
            MegaUser.CHANGE_TYPE_DISABLE_VERSIONS,
            MegaUser.CHANGE_TYPE_CONTACT_LINK_VERIFICATION,
            MegaUser.CHANGE_TYPE_RICH_PREVIEWS,
            MegaUser.CHANGE_TYPE_RUBBISH_TIME,
            MegaUser.CHANGE_TYPE_STORAGE_STATE,
            MegaUser.CHANGE_TYPE_GEOLOCATION,
            MegaUser.CHANGE_TYPE_CAMERA_UPLOADS_FOLDER,
            MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER,
            MegaUser.CHANGE_TYPE_PUSH_SETTINGS,
            MegaUser.CHANGE_TYPE_ALIAS,
            MegaUser.CHANGE_TYPE_UNSHAREABLE_KEY,
            MegaUser.CHANGE_TYPE_DEVICE_NAMES,
            MegaUser.CHANGE_TYPE_MY_BACKUPS_FOLDER,
            MegaUser.CHANGE_TYPE_COOKIE_SETTINGS,
            MegaUser.CHANGE_TYPE_NO_CALLKIT,
        ).fold(0) { acc, value -> acc or value }

        val id = 1L
        val user = mock<MegaUser> { on { handle }.thenReturn(id) }
        whenever(user.changes).thenReturn(allFlags)

        val actual = mapMegaUserListToUserUpdate(listOf(user))

        assertThat(actual.changes[UserId(id)]).containsExactlyElementsIn(UserChanges.values())
    }


}