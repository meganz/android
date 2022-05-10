package test.mega.privacy.android.app.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.mapper.fromMegaUserChangeFlags
import mega.privacy.android.app.domain.entity.user.UserChanges
import nz.mega.sdk.MegaUser
import org.junit.Test

class UserChangesMapperTest {

    @Test
    fun `test mapping single change`() {
        assertThat(fromMegaUserChangeFlags(MegaUser.CHANGE_TYPE_ALIAS)).containsExactly(UserChanges.Alias)
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

        assertThat(fromMegaUserChangeFlags(allFlags)).containsExactlyElementsIn(UserChanges.values())
    }
}