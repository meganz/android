package mega.privacy.android.app.contacts.list.mapper

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.model.AvatarData
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactItemDataToContactItemUiStateMapperTest {

    private lateinit var underTest: ContactItemDataToContactItemUiStateMapper

    private val avatarColorArgb: Int = 0xFF2E7D32.toInt()
    private val expectedAvatarColor: Color = Color(avatarColorArgb)
    private val placeholder: Drawable = mock()

    @BeforeEach
    fun setUp() {
        underTest = ContactItemDataToContactItemUiStateMapper(
            contactItemStatusMapper = ContactItemStatusMapper(),
        )
    }

    @Test
    fun `test that invoke maps alias to display name when alias is set`() {
        val item = contactDataRow(alias = "Ally", fullName = "Alice Anderson")

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.displayName).isEqualTo("Ally")
    }

    @Test
    fun `test that invoke maps full name to display name when alias is blank`() {
        val item = contactDataRow(alias = null, fullName = "Alice Anderson")

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.displayName).isEqualTo("Alice Anderson")
    }

    @Test
    fun `test that invoke falls back to email when alias and full name are blank`() {
        val item = contactDataRow(alias = null, fullName = null, email = "alice@example.com")

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.displayName).isEqualTo("alice@example.com")
    }

    @Test
    fun `test that invoke maps chat status online to ContactItemStatus Online`() {
        val item = contactDataRow(status = MegaChatApi.STATUS_ONLINE)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Online)
    }

    @Test
    fun `test that invoke maps chat status busy to ContactItemStatus Busy`() {
        val item = contactDataRow(status = MegaChatApi.STATUS_BUSY)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Busy)
    }

    @Test
    fun `test that invoke maps chat status away to ContactItemStatus Away`() {
        val item = contactDataRow(status = MegaChatApi.STATUS_AWAY)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Away)
    }

    @Test
    fun `test that invoke maps chat status offline to ContactItemStatus Offline`() {
        val item = contactDataRow(status = MegaChatApi.STATUS_OFFLINE)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Offline)
    }

    @Test
    fun `test that invoke maps null status to ContactItemStatus Unknown`() {
        val item = contactDataRow(status = null)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Unknown)
    }

    @Test
    fun `test that invoke leaves lastSeen null because legacy lastSeen string is not a minute count`() {
        val item = contactDataRow(lastSeen = "Last seen Yesterday")

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.lastSeen).isNull()
    }

    @Test
    fun `test that invoke propagates isVerified true`() {
        val item = contactDataRow(isVerified = true)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.isVerified).isTrue()
    }

    @Test
    fun `test that invoke propagates isVerified false`() {
        val item = contactDataRow(isVerified = false)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.isVerified).isFalse()
    }

    @Test
    fun `test that invoke uses Initials avatar when avatar file is null`() {
        val item = contactDataRow(fullName = "Alice")

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        val avatar = result.avatar as AvatarData.Initials
        assertThat(avatar.initials).isEqualTo("A")
        assertThat(avatar.avatarColor).isEqualTo(expectedAvatarColor)
    }

    @Test
    fun `test that invoke uses Image avatar when avatar file exists and is non empty`(
        @TempDir tempDir: Path,
    ) {
        val avatarFile = tempDir.resolve("avatar.jpg").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val item = contactDataRow()

        val result = underTest(
            item = item,
            avatarFile = avatarFile,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.avatar).isInstanceOf(AvatarData.Image::class.java)
        assertThat((result.avatar as AvatarData.Image).file).isEqualTo(avatarFile)
    }

    @Test
    fun `test that invoke uses Initials avatar when avatar file is empty`(
        @TempDir tempDir: Path,
    ) {
        val emptyAvatar = tempDir.resolve("empty.jpg").toFile().apply { createNewFile() }
        val item = contactDataRow(fullName = "Alice")

        val result = underTest(
            item = item,
            avatarFile = emptyAvatar,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.avatar).isInstanceOf(AvatarData.Initials::class.java)
    }

    @Test
    fun `test that invoke uses item handle as ui state handle`() {
        val handle = 42L
        val item = contactDataRow(handle = handle)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.handle).isEqualTo(handle)
    }

    private fun contactDataRow(
        handle: Long = 1L,
        email: String = "alice@example.com",
        fullName: String? = "Alice",
        alias: String? = null,
        status: Int? = MegaChatApi.STATUS_OFFLINE,
        lastSeen: String? = null,
        isVerified: Boolean = false,
    ): ContactItem.Data = ContactItem.Data(
        handle = handle,
        email = email,
        fullName = fullName,
        alias = alias,
        status = status,
        statusColor = null,
        avatarUri = null,
        placeholder = placeholder,
        lastSeen = lastSeen,
        isNew = false,
        isVerified = isVerified,
    )
}
