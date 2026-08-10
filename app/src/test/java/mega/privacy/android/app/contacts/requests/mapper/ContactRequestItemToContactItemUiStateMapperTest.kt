package mega.privacy.android.app.contacts.requests.mapper

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactRequestItemToContactItemUiStateMapperTest {

    private lateinit var underTest: ContactRequestItemToContactItemUiStateMapper

    private val avatarColorArgb: Int = 0xFF2E7D32.toInt()
    private val expectedAvatarColor: Color = Color(avatarColorArgb)

    @BeforeEach
    fun setUp() {
        underTest = ContactRequestItemToContactItemUiStateMapper()
    }

    @Test
    fun `test that mapper uses email as display name`() {
        val item = contactRequestItem(email = "alice@example.com")

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.displayName).isEqualTo("alice@example.com")
    }

    @Test
    fun `test that mapper uses request handle as ui state handle`() {
        val handle = 42L
        val item = contactRequestItem(handle = handle)

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.handle).isEqualTo(handle)
    }

    @Test
    fun `test that mapper emits Unknown status`() {
        val item = contactRequestItem()

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.status).isEqualTo(ContactItemStatus.Unknown)
    }

    @Test
    fun `test that mapper emits null last seen`() {
        val item = contactRequestItem()

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.lastSeen).isNull()
    }

    @Test
    fun `test that mapper never marks contact request as verified`() {
        val item = contactRequestItem()

        val result = underTest(
            item = item,
            avatarFile = null,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.isVerified).isFalse()
    }

    @Test
    fun `test that mapper uses Initials avatar when avatar file is null`() {
        val item = contactRequestItem(email = "alice@example.com")

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
    fun `test that mapper uses Image avatar when avatar file exists and is non empty`(
        @TempDir tempDir: Path,
    ) {
        val avatarFile = tempDir.resolve("avatar.jpg").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val item = contactRequestItem()

        val result = underTest(
            item = item,
            avatarFile = avatarFile,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.avatar).isInstanceOf(AvatarData.Image::class.java)
        assertThat((result.avatar as AvatarData.Image).file).isEqualTo(avatarFile)
    }

    @Test
    fun `test that mapper uses Initials avatar when avatar file is empty`(
        @TempDir tempDir: Path,
    ) {
        val emptyAvatar = tempDir.resolve("empty.jpg").toFile().apply { createNewFile() }
        val item = contactRequestItem(email = "alice@example.com")

        val result = underTest(
            item = item,
            avatarFile = emptyAvatar,
            avatarColorArgb = avatarColorArgb,
        )

        assertThat(result.avatar).isInstanceOf(AvatarData.Initials::class.java)
    }

    private fun contactRequestItem(
        handle: Long = 1L,
        email: String = "alice@example.com",
        isOutgoing: Boolean = false,
    ): ContactRequestItem = ContactRequestItem(
        handle = handle,
        email = email,
        placeholder = mock<Drawable>(),
        createdTime = "just now",
        isOutgoing = isOutgoing,
    )
}
