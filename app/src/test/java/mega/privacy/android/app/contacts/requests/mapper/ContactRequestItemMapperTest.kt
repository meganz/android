package mega.privacy.android.app.contacts.requests.mapper

import android.graphics.drawable.Drawable
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.repository.AvatarRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.io.File

class ContactRequestItemMapperTest {
    private lateinit var underTest: ContactRequestItemMapper

    private val formatCreationTime = Long::toString
    private val drawable = mock<Drawable>()

    @TempDir
    lateinit var avatarFile: File

    private val avatarRepository = mock<AvatarRepository>()

    @BeforeEach
    internal fun setUp() {
        avatarRepository.stub {
            onBlocking { getAvatarColor(any()) }.thenReturn(1)
            onBlocking { getAvatarFile(any<String>(), any()) }.thenReturn(avatarFile)
        }

        underTest = ContactRequestItemMapper(
            formatCreationTime = formatCreationTime,
            avatarRepository = avatarRepository,
            getImagePlaceholder = { _, _ -> drawable },
        )
    }

    @Test
    fun `test that outgoing requests without target email returns null`() = runTest {
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

        val actual = underTest(request = input)

        assertThat(actual).isNull()
    }

    @Test
    fun `test that email is the source email when the request is incoming`() = runTest {
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

        val actual = underTest(request = input)

        assertThat(actual?.email).isEqualTo(sourceEmail)
    }

    @Test
    fun `test that email is the target email when the request is outgoing`() = runTest {
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

        val actual = underTest(request = input)

        assertThat(actual?.email).isEqualTo(targetEmail)
    }

    @Test
    fun `test that values are mapped correctly`() = runTest {
        val targetEmail = "targetEmail"
        val creationTime: Long = 1
        val isOutgoing = true
        val uri = Uri.parse("uri")
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

        val actual = underTest(request = input)

        assertThat(actual?.email).isEqualTo(targetEmail)
        assertThat(actual?.handle).isEqualTo(handle)
        assertThat(actual?.avatarUri).isEqualTo(uri)
        assertThat(actual?.placeholder).isEqualTo(drawable)
        assertThat(actual?.createdTime).isEqualTo(formatCreationTime(creationTime * 1000))
        assertThat(actual?.isOutgoing).isEqualTo(isOutgoing)
    }

    @Test
    internal fun `test that no file uri is added if repository throws an exception`() = runTest {
        avatarRepository.stub {
            onBlocking {
                getAvatarFile(
                    any<String>(),
                    any()
                )
            }.thenAnswer { throw RuntimeException() }
        }

        val input = ContactRequest(
            handle = 1,
            sourceEmail = "sourceEmail",
            sourceMessage = "sourceMessage",
            targetEmail = "targetEmail",
            creationTime = 1,
            modificationTime = 1,
            status = ContactRequestStatus.Unresolved,
            isOutgoing = true,
            isAutoAccepted = false,
        )

        val actual = underTest(request = input)

        assertThat(actual?.avatarUri).isNull()
    }
}