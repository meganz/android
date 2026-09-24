package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsContactRequestByEmailInPendingOrAcceptedStateUseCaseTest {

    private lateinit var underTest: IsContactRequestByEmailInPendingOrAcceptedStateUseCase

    private val email = "test@test.com"

    @BeforeEach
    fun setup() {
        underTest = IsContactRequestByEmailInPendingOrAcceptedStateUseCase()
    }

    @ParameterizedTest(name = "when the request is: {0}")
    @EnumSource(value = ContactRequestStatus::class, names = ["Accepted", "Unresolved"])
    fun `test that true is returned`(status: ContactRequestStatus) {
        val contactRequest = ContactRequest(
            handle = 1L,
            sourceEmail = email,
            sourceMessage = "",
            targetEmail = email,
            creationTime = System.currentTimeMillis(),
            modificationTime = System.currentTimeMillis(),
            status = status,
            isOutgoing = true,
            isAutoAccepted = true
        )
        val actual = underTest(contactRequest, email)

        assertThat(actual).isTrue()
    }

    @ParameterizedTest(name = "when is email the same: {0} and contact status: {1}")
    @MethodSource("providePendingAndAcceptedContacts")
    fun `test that false is returned`(
        hasSameEmail: Boolean,
        status: ContactRequestStatus,
    ) {
        val contactRequest = ContactRequest(
            handle = 1L,
            sourceEmail = email,
            sourceMessage = "",
            targetEmail = if (hasSameEmail) email else "",
            creationTime = System.currentTimeMillis(),
            modificationTime = System.currentTimeMillis(),
            status = status,
            isOutgoing = true,
            isAutoAccepted = true
        )
        val actual = underTest(contactRequest, email)

        assertThat(actual).isFalse()
    }

    private fun providePendingAndAcceptedContacts() = Stream.of(
        Arguments.of(false, ContactRequestStatus.Accepted),
        Arguments.of(false, ContactRequestStatus.Unresolved),
        Arguments.of(true, ContactRequestStatus.Denied),
    )
}
