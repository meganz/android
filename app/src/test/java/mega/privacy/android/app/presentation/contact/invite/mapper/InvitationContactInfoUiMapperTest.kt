package mega.privacy.android.app.presentation.contact.invite.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_LETTER_HEADER
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.presentation.contact.invite.InviteContactViewModel
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvitationContactInfoUiMapperTest {

    private lateinit var underTest: InvitationContactInfoUiMapper

    @OptIn(ExperimentalCoroutinesApi::class)
    private val defaultDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        underTest = InvitationContactInfoUiMapper(
            defaultDispatcher = defaultDispatcher
        )
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["photoUri"])
    fun `test that the right list of mapped contact information is returned`(photoUri: String?) =
        runTest {
            val photoUriPath = photoUri?.let { UriPath(it) }
            val localContacts = listOf(
                LocalContact(
                    id = 1L,
                    name = "name1",
                    phoneNumbers = listOf("08123214322"),
                    photoUri = photoUriPath
                ),
                LocalContact(
                    id = 2L,
                    name = "name2",
                    emails = listOf("test2@test.com")
                ),
                LocalContact(
                    id = 3L,
                    name = "name3"
                ),
                LocalContact(
                    id = 4L,
                    name = "name4",
                    phoneNumbers = listOf("08123214322"),
                    emails = listOf("test4@test.com")
                )
            )

            val actual = underTest(localContacts = localContacts)

            val expected = listOf(
                InvitationContactInfo(
                    id = InviteContactViewModel.ID_PHONE_CONTACTS_HEADER,
                    type = InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER
                ),
                InvitationContactInfo(
                    id = "N".hashCode().toLong(),
                    type = TYPE_LETTER_HEADER,
                    displayInfo = "N"
                ),
                InvitationContactInfo(
                    id = localContacts[0].id,
                    name = localContacts[0].name,
                    type = TYPE_PHONE_CONTACT,
                    filteredContactInfos = localContacts[0].phoneNumbers + localContacts[0].emails,
                    displayInfo = (localContacts[0].phoneNumbers + localContacts[0].emails)[0],
                    photoUri = photoUriPath?.value
                ),
                InvitationContactInfo(
                    id = localContacts[1].id,
                    name = localContacts[1].name,
                    type = TYPE_PHONE_CONTACT,
                    filteredContactInfos = localContacts[1].phoneNumbers + localContacts[1].emails,
                    displayInfo = (localContacts[1].phoneNumbers + localContacts[1].emails)[0]
                ),
                InvitationContactInfo(
                    id = localContacts[3].id,
                    name = localContacts[3].name,
                    type = TYPE_PHONE_CONTACT,
                    filteredContactInfos = localContacts[3].phoneNumbers + localContacts[3].emails,
                    displayInfo = (localContacts[3].phoneNumbers + localContacts[3].emails)[0],
                )
            )
            assertThat(actual).isEqualTo(expected)
        }
}
