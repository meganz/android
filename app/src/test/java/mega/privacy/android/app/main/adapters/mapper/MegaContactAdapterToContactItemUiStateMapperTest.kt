package mega.privacy.android.app.main.adapters.mapper

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.MegaContactAdapter
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
class MegaContactAdapterToContactItemUiStateMapperTest {

    private lateinit var underTest: MegaContactAdapterToContactItemUiStateMapper

    private val avatarColorArgb: Int = 0xFF2E7D32.toInt()
    private val expectedAvatarColor: Color = Color(avatarColorArgb)

    @BeforeEach
    fun setUp() {
        underTest = MegaContactAdapterToContactItemUiStateMapper(
            contactItemStatusMapper = ContactItemStatusMapper(),
        )
    }

    @Test
    fun `test that full name maps to display name`() {
        val contact = megaContact(fullName = "Alice Anderson")

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_ONLINE,
            isVerified = false,
        )

        assertThat(result.displayName).isEqualTo("Alice Anderson")
    }

    @Test
    fun `test that display name falls back to mail when full name is null`() {
        val contact = megaContact(fullName = null)

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.displayName).isEqualTo("alice@example.com")
    }

    @Test
    fun `test that display name falls back to mail when full name is blank`() {
        val contact = megaContact(fullName = "   ")

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.displayName).isEqualTo("alice@example.com")
    }

    @Test
    fun `test that chat status online maps to ContactItemStatus Online`() {
        val contact = megaContact()

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_ONLINE,
            isVerified = false,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Online)
    }

    @Test
    fun `test that chat status busy maps to ContactItemStatus Busy`() {
        val contact = megaContact()

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_BUSY,
            isVerified = false,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Busy)
    }

    @Test
    fun `test that chat status away maps to ContactItemStatus Away`() {
        val contact = megaContact()

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_AWAY,
            isVerified = false,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Away)
    }

    @Test
    fun `test that chat status offline maps to ContactItemStatus Offline`() {
        val contact = megaContact()

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Offline)
    }

    @Test
    fun `test that propagates isVerified true`() {
        val contact = megaContact()

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = true,
        )

        assertThat(result.isVerified).isTrue()
    }

    @Test
    fun `test that uses Initials avatar when avatar file is null`() {
        val contact = megaContact(fullName = "Alice")

        val result = underTest(
            megaContact = contact,
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
    fun `test that uses Image avatar when avatar file exists and is non empty`(
        @TempDir tempDir: Path,
    ) {
        val avatarFile = tempDir.resolve("avatar.jpg").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val contact = megaContact()

        val result = underTest(
            megaContact = contact,
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
    fun `test that uses Initials avatar when avatar file is empty`(
        @TempDir tempDir: Path,
    ) {
        val emptyAvatar = tempDir.resolve("empty.jpg").toFile().apply { createNewFile() }
        val contact = megaContact(fullName = "Alice")

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = emptyAvatar,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.avatar).isInstanceOf(AvatarData.Initials::class.java)
    }

    @Test
    fun `test that uses mega user handle as ui state handle`() {
        val handle = 42L
        val contact = megaContact(handle = handle)

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.handle).isEqualTo(handle)
    }

    @Test
    fun `test that mapper does not expose selection state`() {
        // Selection is intentionally a Compose-only concern (owned by
        // ShareContactRowState) — the mapper must not surface an isSelected
        // field on ContactItemUiState. This test will fail to compile if a
        // future change re-introduces the field.
        val result = underTest(
            megaContact = megaContact(fullName = "Alice"),
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.javaClass.declaredFields.map { it.name })
            .doesNotContain("isSelected")
    }

    @Test
    fun `test that last seen is null because adapter does not surface minute precise data`() {
        val contact = megaContact()

        val result = underTest(
            megaContact = contact,
            mail = "alice@example.com",
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
            chatStatusValue = MegaChatApi.STATUS_OFFLINE,
            isVerified = false,
        )

        assertThat(result.lastSeen).isNull()
    }

    private fun megaContact(
        handle: Long = 1L,
        fullName: String? = "Alice",
    ): MegaContactAdapter {
        val megaUser = mock<MegaUser> {
            whenever(it.handle).thenReturn(handle)
            whenever(it.email).thenReturn("alice@example.com")
        }
        return MegaContactAdapter(
            contact = null,
            megaUser = megaUser,
            fullName = fullName,
        )
    }
}
