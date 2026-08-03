package mega.privacy.android.feature.contact.requests.mapper

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactRequestUiItemMapperTest {

    private val underTest = ContactRequestUiItemMapper()

    @Test
    fun `test that handle is mapped from request handle`() {
        val result = underTest(createContactRequest(handle = 42L))

        assertThat(result.handle).isEqualTo(42L)
        assertThat(result.contact.handle).isEqualTo(42L)
    }

    @Test
    fun `test that isOutgoing is mapped from request isOutgoing`() {
        val result = underTest(createContactRequest(isOutgoing = true))

        assertThat(result.isOutgoing).isTrue()
    }

    @Test
    fun `test that email uses target email when request is outgoing`() {
        val result = underTest(
            createContactRequest(
                isOutgoing = true,
                sourceEmail = "source@example.com",
                targetEmail = "target@example.com",
            )
        )

        assertThat(result.contact.email).isEqualTo("target@example.com")
        assertThat(result.contact.displayName).isEqualTo("target@example.com")
    }

    @Test
    fun `test that email uses source email when request is incoming`() {
        val result = underTest(
            createContactRequest(
                isOutgoing = false,
                sourceEmail = "source@example.com",
                targetEmail = "target@example.com",
            )
        )

        assertThat(result.contact.email).isEqualTo("source@example.com")
        assertThat(result.contact.displayName).isEqualTo("source@example.com")
    }

    @Test
    fun `test that email falls back to source email when outgoing target email is null`() {
        val result = underTest(
            createContactRequest(
                isOutgoing = true,
                sourceEmail = "source@example.com",
                targetEmail = null,
            )
        )

        assertThat(result.contact.email).isEqualTo("source@example.com")
    }

    @Test
    fun `test that status is unknown and contact is not verified`() {
        val result = underTest(createContactRequest())

        assertThat(result.contact.status).isEqualTo(ContactItemStatus.Unknown)
        assertThat(result.contact.isVerified).isFalse()
        assertThat(result.contact.lastSeen).isNull()
    }

    @Test
    fun `test that avatar is initials built from the first letter of the email`() {
        val result = underTest(
            createContactRequest(isOutgoing = false, sourceEmail = "alice@example.com")
        )

        val avatar = result.contact.avatar
        assertThat(avatar).isInstanceOf(AvatarData.Initials::class.java)
        assertThat((avatar as AvatarData.Initials).initials).isEqualTo("A")
        assertThat(avatar.avatarColor).isEqualTo(Color.Unspecified)
    }

    @Test
    fun `test that createdTime is the creation time formatted as a medium localized date`() {
        val creationTime = 1_700_000_000L

        val result = underTest(createContactRequest(creationTime = creationTime))

        val expected = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(
            Instant.ofEpochSecond(creationTime).atZone(ZoneId.systemDefault()).toLocalDate()
        )
        assertThat(result.createdTime).isEqualTo(expected)
    }

    private fun createContactRequest(
        handle: Long = 1L,
        sourceEmail: String = "source@example.com",
        targetEmail: String? = "target@example.com",
        creationTime: Long = 1_700_000_000L,
        isOutgoing: Boolean = false,
    ) = ContactRequest(
        handle = handle,
        sourceEmail = sourceEmail,
        sourceMessage = null,
        targetEmail = targetEmail,
        creationTime = creationTime,
        modificationTime = creationTime,
        status = ContactRequestStatus.Unresolved,
        isOutgoing = isOutgoing,
        isAutoAccepted = false,
    )
}
