package mega.privacy.android.app.main

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvitationContactInfoTest {

    @ParameterizedTest
    @MethodSource("provideSingleContactInformation")
    fun `test that false is returned if the total filtered contacts is less than or equal to one`(
        contact: InvitationContactInfo,
    ) {
        val actual = contact.hasMultipleContactInfos()

        assertThat(actual).isFalse()
    }

    private fun provideSingleContactInformation() = Stream.of(
        Arguments.of(
            InvitationContactInfo(),
            InvitationContactInfo(
                filteredContactInfos = listOf("test")
            )
        )
    )

    @Test
    fun `test that true is returned if the total filtered contacts is more than one`() {
        val contact = InvitationContactInfo(
            filteredContactInfos = listOf("1", "2")
        )

        val actual = contact.hasMultipleContactInfos()

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that the contact's name is returned if it is not empty`() {
        val contact = InvitationContactInfo(name = "name", displayInfo = "displayInfo")

        val actual = contact.getContactName()

        assertThat(actual).isEqualTo("name")
    }

    @Test
    fun `test that the contact's display information is returned if the name is empty`() {
        val contact = InvitationContactInfo(displayInfo = "displayInfo")

        val actual = contact.getContactName()

        assertThat(actual).isEqualTo("displayInfo")
    }

    @Test
    fun `test that true is returned if the contact's display information contains the '@' sign`() {
        val contact = InvitationContactInfo(displayInfo = "test@test.com")

        val actual = contact.isEmailContact()

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned if the contact's display information doesn't contain the '@' sign`() {
        val contact = InvitationContactInfo(displayInfo = "testtest.com")

        val actual = contact.isEmailContact()

        assertThat(actual).isFalse()
    }
}
