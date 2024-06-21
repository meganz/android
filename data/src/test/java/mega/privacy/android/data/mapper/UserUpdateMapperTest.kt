package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserUpdateMapperTest {
    private val underTest = UserUpdateMapper()

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
        val actual = underTest(userList)
        assertThat(actual.changes).containsKey(UserId(id1))
        assertThat(actual.changes).containsKey(UserId(id2))
    }

    @Test
    fun `test mapping single change`() {
        val id = 1L
        val user = mock<MegaUser> { on { handle }.thenReturn(id) }
        whenever(user.changes).thenReturn(MegaUser.CHANGE_TYPE_ALIAS.toLong())

        val actual = underTest(listOf(user))

        assertThat(actual.changes[UserId(id)]).contains(UserChanges.Alias)

    }

    @Test
    fun `test that duplicate users are combined`() {
        val id1 = 1L
        val id2 = 2L
        val user1 = mock<MegaUser> {
            on { handle }.thenReturn(id1)
            on { changes }.thenReturn(
                MegaUser.CHANGE_TYPE_ALIAS.toLong(),
                MegaUser.CHANGE_TYPE_AVATAR.toLong()
            )
            on { visibility }.thenReturn(MegaUser.VISIBILITY_VISIBLE)
        }
        val user2 = mock<MegaUser> {
            on { handle }.thenReturn(id2)
            on { changes }.thenReturn(MegaUser.CHANGE_TYPE_ALIAS.toLong())
            on { visibility }.thenReturn(MegaUser.VISIBILITY_BLOCKED)
        }

        val userList = arrayListOf(user1, user2, user1)
        val actual = underTest(userList)

        assertThat(actual.changes.size).isEqualTo(2)
        assertThat(actual.changes[UserId(id1)]).containsExactly(
            UserChanges.Alias,
            UserChanges.Avatar,
            UserChanges.Visibility(UserVisibility.Visible)
        )
        assertThat(actual.changes[UserId(id2)]).containsExactly(
            UserChanges.Alias,
            UserChanges.Visibility(UserVisibility.Blocked)
        )
    }


    @Test
    fun `test mapping all changes`() {
        val id = 1L
        val user = mock<MegaUser> { on { handle }.thenReturn(id) }
        whenever(user.changes).thenReturn(allFlags)

        val actual = underTest(listOf(user))

        assertThat(actual.changes[UserId(id)]).containsAtLeastElementsIn(
            listOf(
                UserChanges.AuthenticationInformation,
                UserChanges.LastInteractionTimestamp,
                UserChanges.Avatar,
                UserChanges.Firstname,
                UserChanges.Lastname,
                UserChanges.Email,
                UserChanges.Keyring,
                UserChanges.Country,
                UserChanges.Birthday,
                UserChanges.ChatPublicKey,
                UserChanges.SigningPublicKey,
                UserChanges.RsaPublicKeySignature,
                UserChanges.ChatPublicKeySignature,
                UserChanges.Language,
                UserChanges.PasswordReminder,
                UserChanges.DisableVersions,
                UserChanges.ContactLinkVerification,
                UserChanges.RichPreviews,
                UserChanges.RubbishTime,
                UserChanges.StorageState,
                UserChanges.Geolocation,
                UserChanges.CameraUploadsFolder,
                UserChanges.MyChatFilesFolder,
                UserChanges.PushSettings,
                UserChanges.Alias,
                UserChanges.UnshareableKey,
                UserChanges.DeviceNames,
                UserChanges.MyBackupsFolder,
                UserChanges.CookieSettings,
                UserChanges.NoCallkit
            )
        )
    }

    @Test
    fun `test that an empty list of changes is mapped to visibility change`() {
        val id = 1L
        val user = mock<MegaUser> {
            on { handle }.thenReturn(id)
            on { visibility }.thenReturn(MegaUser.VISIBILITY_HIDDEN)
        }
        whenever(user.changes).thenReturn(0L)

        val actual = underTest(listOf(user))

        assertThat(actual.changes[UserId(id)]?.all { it is UserChanges.Visibility }).isTrue()
    }

    @ParameterizedTest(name = "if visibility value is {0}, visibility should be: {1}")
    @MethodSource("visibilityChanges")
    fun `test that visibility change include user visibility`(
        megaVisibility: Int,
        userVisibility: UserVisibility,
    ) {
        val id = 1L
        val user = mock<MegaUser> {
            on { handle }.thenReturn(id)
            on { visibility }.thenReturn(megaVisibility)
        }
        whenever(user.changes).thenReturn(0L)

        val actual = underTest(listOf(user))

        assertThat(actual.changes[UserId(id)]).containsExactly(UserChanges.Visibility(userVisibility))
    }

    private fun visibilityChanges() = Stream.of(
        Arguments.of(MegaUser.VISIBILITY_HIDDEN, UserVisibility.Hidden),
        Arguments.of(MegaUser.VISIBILITY_VISIBLE, UserVisibility.Visible),
        Arguments.of(MegaUser.VISIBILITY_BLOCKED, UserVisibility.Blocked),
        Arguments.of(MegaUser.VISIBILITY_UNKNOWN, UserVisibility.Unknown),
        Arguments.of(MegaUser.VISIBILITY_INACTIVE, UserVisibility.Inactive),
    )

    @Test
    fun `test that email map includes all users`() {
        val id1 = 1L
        val id2 = 2L
        val email1 = "email1"
        val email2 = "email2"
        val user1 = mock<MegaUser> {
            on { handle }.thenReturn(id1)
            on { changes }.thenReturn(
                MegaUser.CHANGE_TYPE_ALIAS.toLong(), MegaUser.CHANGE_TYPE_AVATAR.toLong()
            )
            on { email }.thenReturn(email1)
        }
        val user2 = mock<MegaUser> {
            on { handle }.thenReturn(id2)
            on { changes }.thenReturn(MegaUser.CHANGE_TYPE_ALIAS.toLong())
            on { email }.thenReturn(email2)
        }

        val userList = arrayListOf(user1, user2, user1)
        val actual = underTest(userList).emailMap
        assertThat(actual).containsExactlyEntriesIn(
            mapOf(
                UserId(id1) to email1, UserId(id2) to email2
            )
        )
    }

    @ParameterizedTest(name = "changes should include visibility change of: {1}")
    @MethodSource("visibilityChanges")
    fun `test that all changes include visibility`(
        megaVisibility: Int,
        userVisibility: UserVisibility,
    ) {
        val allChangesId = 1L
        val allChangesUser = mock<MegaUser> {
            on { handle }.thenReturn(allChangesId)
            on { visibility }.thenReturn(megaVisibility)
        }
        whenever(allChangesUser.changes).thenReturn(allFlags)

        val noChangesId = 1L
        val noChangesUser = mock<MegaUser> {
            on { handle }.thenReturn(noChangesId)
            on { visibility }.thenReturn(megaVisibility)
        }
        whenever(noChangesUser.changes).thenReturn(allFlags)

        val actual = underTest(listOf(allChangesUser, noChangesUser))

        assertWithMessage("User with other changes did not include visibility").that(
            actual.changes[UserId(
                allChangesId
            )]
        ).contains(
            UserChanges.Visibility(
                userVisibility
            )
        )
        assertWithMessage("User with no other changes did not include visibility").that(
            actual.changes[UserId(
                noChangesId
            )]
        ).contains(
            UserChanges.Visibility(
                userVisibility
            )
        )
    }

    private val allFlags: Long = listOf(
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
    ).map { it.toLong() }
        .fold(0L) { acc, value -> acc or value }
}