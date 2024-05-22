package mega.privacy.android.app.contacts.requests.mapper

import android.graphics.drawable.Drawable
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class ContactRequestItemMapperTest {
    private lateinit var underTest: ContactRequestItemMapper

    private val formatCreationTime = Long::toString

    @BeforeEach
    internal fun setUp() {
        underTest = ContactRequestItemMapper(formatCreationTime = formatCreationTime)
    }

    @Test
    fun `test that outgoing requests without target email returns null`() {
        val input = ContactRequest(
            handle = 1,
            sourceEmail = "sourceEmail",
            sourceMessage = "sourceMessage",
            targetEmail = null,
            creationTime = 1,
            modificationTime = 1,
            status = ContactRequestStatus.Unresolved,
            isOutgoing = true,
            isAutoAccepted = false,
        )

        val actual = underTest(request = input, avatarUri = null, placeHolder = mock())

        assertThat(actual).isNull()
    }

    @Test
    fun `test that email is the source email when the request is incoming`() {
        val sourceEmail = "sourceEmail"
        val input = ContactRequest(
            handle = 1,
            sourceEmail = sourceEmail,
            sourceMessage = "sourceMessage",
            targetEmail = null,
            creationTime = 1,
            modificationTime = 1,
            status = ContactRequestStatus.Unresolved,
            isOutgoing = false,
            isAutoAccepted = false,
        )

        val actual = underTest(request = input, avatarUri = null, placeHolder = mock())

        assertThat(actual?.email).isEqualTo(sourceEmail)
    }

    @Test
    fun `test that email is the target email when the request is outgoing`() {
        val targetEmail = "targetEmail"
        val input = ContactRequest(
            handle = 1,
            sourceEmail = "sourceEmail",
            sourceMessage = "sourceMessage",
            targetEmail = targetEmail,
            creationTime = 1,
            modificationTime = 1,
            status = ContactRequestStatus.Unresolved,
            isOutgoing = true,
            isAutoAccepted = false,
        )

        val actual = underTest(request = input, avatarUri = null, placeHolder = mock())

        assertThat(actual?.email).isEqualTo(targetEmail)
    }

    @Test
    fun `test that values are mapped correctly`() {
        val targetEmail = "targetEmail"
        val creationTime: Long = 1
        val isOutgoing = true
        val uri = Uri.parse("uri")
        val drawable = mock<Drawable>()
        val handle: Long = 1123

        val input = ContactRequest(
            handle = handle,
            sourceEmail = "sourceEmail",
            sourceMessage = "sourceMessage",
            targetEmail = targetEmail,
            creationTime = creationTime,
            modificationTime = 1,
            status = ContactRequestStatus.Unresolved,
            isOutgoing = isOutgoing,
            isAutoAccepted = false,
        )

        val actual = underTest(request = input, avatarUri = uri, placeHolder = drawable)

        assertThat(actual?.email).isEqualTo(targetEmail)
        assertThat(actual?.handle).isEqualTo(handle)
        assertThat(actual?.avatarUri).isEqualTo(uri)
        assertThat(actual?.placeholder).isEqualTo(drawable)
        assertThat(actual?.createdTime).isEqualTo(formatCreationTime(creationTime))
        assertThat(actual?.isOutgoing).isEqualTo(isOutgoing)
    }
}