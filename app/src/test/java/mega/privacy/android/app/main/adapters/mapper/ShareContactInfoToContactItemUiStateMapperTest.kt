package mega.privacy.android.app.main.adapters.mapper

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.main.PhoneContactInfo
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.model.AvatarData
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShareContactInfoToContactItemUiStateMapperTest {

    private lateinit var underTest: ShareContactInfoToContactItemUiStateMapper

    private val avatarColorArgb: Int = 0xFF2E7D32.toInt()
    private val expectedAvatarColor: Color = Color(avatarColorArgb)

    @BeforeEach
    fun setUp() {
        underTest = ShareContactInfoToContactItemUiStateMapper(
            contactItemStatusMapper = ContactItemStatusMapper(),
        )
    }

    @Test
    fun `test that mega contact maps full name to display name`() {
        val info = megaContactRow(fullName = "Alice Anderson")

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_ONLINE,
            isVerified = false,
        )

        assertThat(result.displayName).isEqualTo("Alice Anderson")
    }

    @Test
    fun `test that mega contact falls back to mail when full name is null`() {
        val info = megaContactRow(fullName = null)

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.displayName).isEqualTo("alice@example.com")
    }

    @Test
    fun `test that mega contact maps chat status online to ContactItemStatus Online`() {
        val info = megaContactRow()

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_ONLINE,
            isVerified = false,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Online)
    }

    @Test
    fun `test that mega contact maps chat status busy to ContactItemStatus Busy`() {
        val info = megaContactRow()

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_BUSY,
            isVerified = false,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Busy)
    }

    @Test
    fun `test that mega contact propagates isVerified true`() {
        val info = megaContactRow()

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = true,
        )

        assertThat(result.isVerified).isTrue()
    }

    @Test
    fun `test that mega contact uses Initials avatar when avatar file is null`() {
        val info = megaContactRow(fullName = "Alice")

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        val avatar = result.avatar as AvatarData.Initials
        assertThat(avatar.initials).isEqualTo("A")
        assertThat(avatar.avatarColor).isEqualTo(expectedAvatarColor)
    }

    @Test
    fun `test that mega contact uses Image avatar when avatar file exists and is non empty`(
        @TempDir tempDir: Path,
    ) {
        val avatarFile = tempDir.resolve("avatar.jpg").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val info = megaContactRow()

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = avatarFile,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.avatar).isInstanceOf(AvatarData.Image::class.java)
        assertThat((result.avatar as AvatarData.Image).file).isEqualTo(avatarFile)
    }

    @Test
    fun `test that mega contact uses Initials avatar when avatar file is empty`(
        @TempDir tempDir: Path,
    ) {
        val emptyAvatar = tempDir.resolve("empty.jpg").toFile().apply { createNewFile() }
        val info = megaContactRow(fullName = "Alice")

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = emptyAvatar,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.avatar).isInstanceOf(AvatarData.Initials::class.java)
    }

    @Test
    fun `test that mega contact uses mega user handle as ui state handle`() {
        val handle = 42L
        val info = megaContactRow(handle = handle)

        val result = underTest(
            info = info,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.handle).isEqualTo(handle)
    }

    @Test
    fun `test that phone contact maps name to display name`() {
        val info = phoneContactRow(name = "Bob Brown")

        val result = underTest(
            info = info,
            mail = "bob@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = 0,
            isVerified = false,
        )

        assertThat(result.displayName).isEqualTo("Bob Brown")
    }

    @Test
    fun `test that phone contact emits Unknown status`() {
        val info = phoneContactRow()

        val result = underTest(
            info = info,
            mail = "bob@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_ONLINE,
            isVerified = false,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Unknown)
    }

    @Test
    fun `test that phone contact is never verified`() {
        val info = phoneContactRow()

        val result = underTest(
            info = info,
            mail = "bob@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = 0,
            isVerified = true,
        )

        assertThat(result.isVerified).isFalse()
    }

    @Test
    fun `test that phone contact uses Initials avatar when avatar file is null`() {
        val info = phoneContactRow(name = "Bob")

        val result = underTest(
            info = info,
            mail = "bob@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = 0,
            isVerified = false,
        )

        val avatar = result.avatar as AvatarData.Initials
        assertThat(avatar.initials).isEqualTo("B")
        assertThat(avatar.avatarColor).isEqualTo(expectedAvatarColor)
    }

    @Test
    fun `test that phone contact uses phone contact id as ui state handle`() {
        val phoneId = 99L
        val info = phoneContactRow(id = phoneId)

        val result = underTest(
            info = info,
            mail = "bob@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = 0,
            isVerified = false,
        )

        assertThat(result.handle).isEqualTo(phoneId)
    }

    @Test
    fun `test that phone contact falls back to email when name is null`() {
        val info = phoneContactRow(name = null, email = "fallback@example.com")

        val result = underTest(
            info = info,
            mail = "bob@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = 0,
            isVerified = false,
        )

        assertThat(result.displayName).isEqualTo("fallback@example.com")
    }

    private fun megaContactRow(
        handle: Long = 1L,
        fullName: String? = "Alice",
    ): ShareContactInfo {
        val megaUser = mock<MegaUser> {
            whenever(it.handle).thenReturn(handle)
            whenever(it.email).thenReturn("alice@example.com")
        }
        val megaContactAdapter = MegaContactAdapter(
            contact = null,
            megaUser = megaUser,
            fullName = fullName,
        )
        return ShareContactInfo(null, megaContactAdapter, "alice@example.com")
    }

    private fun phoneContactRow(
        id: Long = 7L,
        name: String? = "Bob",
        email: String? = "bob@example.com",
    ): ShareContactInfo {
        val phoneContactInfo = PhoneContactInfo(id, name, email, "+1234")
        return ShareContactInfo(phoneContactInfo, null, email ?: "")
    }
}
