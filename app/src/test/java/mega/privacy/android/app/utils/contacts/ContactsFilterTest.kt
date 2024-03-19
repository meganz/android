package mega.privacy.android.app.utils.contacts

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.main.InvitationContactInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactsFilterTest {

    @ParameterizedTest
    @MethodSource("provideSameContactInformation")
    fun `test that true is returned when the contact ID and display information are the same`(
        firstContact: InvitationContactInfo,
        secondContact: InvitationContactInfo,
    ) {
        val actual = ContactsFilter.isTheSameContact(firstContact, secondContact)

        assertThat(actual).isTrue()
    }

    private fun provideSameContactInformation() = Stream.of(
        Arguments.of(
            InvitationContactInfo(id = 1, displayInfo = "08123456789"),
            InvitationContactInfo(id = 1, displayInfo = "08123456789")
        ),
        Arguments.of(
            InvitationContactInfo(id = 1, displayInfo = "test@GMAIL.COM"),
            InvitationContactInfo(id = 1, displayInfo = "test@gmail.com")
        )
    )

    @ParameterizedTest
    @MethodSource("provideDifferentContactInformation")
    fun `test that false is returned when the contact ID and display information are different`(
        firstContact: InvitationContactInfo,
        secondContact: InvitationContactInfo,
    ) {
        val actual = ContactsFilter.isTheSameContact(firstContact, secondContact)

        assertThat(actual).isFalse()
    }

    private fun provideDifferentContactInformation() = Stream.of(
        Arguments.of(
            InvitationContactInfo(id = 1, displayInfo = "08123456789"),
            InvitationContactInfo(id = 2, displayInfo = "08123456789")
        ),
        Arguments.of(
            InvitationContactInfo(id = 1, displayInfo = "tesst@GMAIL.COM"),
            InvitationContactInfo(id = 1, displayInfo = "test@gmail.com")
        )
    )
}
