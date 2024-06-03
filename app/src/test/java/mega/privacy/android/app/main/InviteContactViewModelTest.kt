package mega.privacy.android.app.main

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.InviteContactViewModel.Companion.ID_PHONE_CONTACTS_HEADER
import mega.privacy.android.app.main.model.InvitationStatusUiState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.usecase.contact.FilterLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.FilterPendingOrAcceptedLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailsUseCase
import mega.privacy.android.domain.usecase.qrcode.CreateContactLinkUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteContactViewModelTest {

    private lateinit var underTest: InviteContactViewModel

    private val context: Context = mock()
    private val createContactLinkUseCase: CreateContactLinkUseCase = mock()
    private val getLocalContactsUseCase: GetLocalContactsUseCase = mock()
    private val filterLocalContactsByEmailUseCase: FilterLocalContactsByEmailUseCase = mock()
    private val filterPendingOrAcceptedLocalContactsByEmailUseCase: FilterPendingOrAcceptedLocalContactsByEmailUseCase =
        mock()
    private val inviteContactWithEmailsUseCase: InviteContactWithEmailsUseCase = mock()
    private val defaultQuery = "defaultQuery"
    private val defaultContactLink = "https://mega.nz/C!wf8jTYRB"

    private lateinit var savedStateHandle: SavedStateHandle

    @BeforeEach
    fun setup() = runTest {
        whenever(createContactLinkUseCase(false)) doReturn defaultContactLink

        savedStateHandle = SavedStateHandle(mapOf("CONTACT_SEARCH_QUERY" to defaultQuery))
        initializeViewModel()
    }

    private fun initializeViewModel() {
        underTest = InviteContactViewModel(
            applicationContext = context,
            defaultDispatcher = extension.testDispatcher,
            getLocalContactsUseCase = getLocalContactsUseCase,
            filterLocalContactsByEmailUseCase = filterLocalContactsByEmailUseCase,
            filterPendingOrAcceptedLocalContactsByEmailUseCase = filterPendingOrAcceptedLocalContactsByEmailUseCase,
            createContactLinkUseCase = createContactLinkUseCase,
            inviteContactWithEmailsUseCase = inviteContactWithEmailsUseCase,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `test that all contacts should be initialized when given a contact list`() {
        val contacts = listOf(InvitationContactInfo())

        underTest.initializeAllContacts(contacts)

        assertThat(underTest.allContacts).isEqualTo(contacts)
    }

    @Test
    fun `test that filtered contacts list should be initialized when given a contact list`() =
        runTest {
            val contacts = listOf(InvitationContactInfo())

            underTest.initializeFilteredContacts(contacts)

            underTest.filterUiState.test {
                assertThat(expectMostRecentItem().filteredContacts).isEqualTo(contacts)
            }
        }

    @ParameterizedTest
    @MethodSource("provideContactInfo")
    fun `test that the highlighted value should be set correctly`(
        contact: InvitationContactInfo,
    ) = runTest {
        underTest.initializeAllContacts(listOf(contact))
        underTest.initializeFilteredContacts(listOf(contact))

        underTest.toggleContactHighlightedInfo(contact)

        val expected = listOf(InvitationContactInfo(isHighlighted = !contact.isHighlighted))
        underTest.filterUiState.test {
            assertThat(expectMostRecentItem().filteredContacts).isEqualTo(expected)
        }
        assertThat(underTest.allContacts).isEqualTo(expected)
    }

    private fun provideContactInfo() = Stream.of(
        Arguments.of(InvitationContactInfo(isHighlighted = true)),
        Arguments.of(InvitationContactInfo(isHighlighted = false))
    )

    @ParameterizedTest(name = "should be set to {1}")
    @MethodSource("provideContactInfoWithExpectedHighlightedValue")
    fun `test that the highlighted value should be set correctly`(
        contact: InvitationContactInfo,
        isHighlighted: Boolean,
    ) = runTest {
        underTest.initializeAllContacts(listOf(contact))
        underTest.initializeFilteredContacts(listOf(contact))

        underTest.toggleContactHighlightedInfo(contact, isHighlighted)

        val expected = listOf(InvitationContactInfo(isHighlighted = isHighlighted))
        underTest.filterUiState.test {
            assertThat(expectMostRecentItem().filteredContacts).isEqualTo(expected)
        }
        assertThat(underTest.allContacts).isEqualTo(expected)
    }

    private fun provideContactInfoWithExpectedHighlightedValue() = Stream.of(
        Arguments.of(
            InvitationContactInfo(isHighlighted = true),
            false
        ),
        Arguments.of(
            InvitationContactInfo(isHighlighted = false),
            true
        )
    )

    @ParameterizedTest(name = "when the query is {0}")
    @NullAndEmptySource
    fun `test that the filtered contact list is reset`(query: String?) = runTest {
        val contacts = listOf(InvitationContactInfo(id = 123L))
        underTest.initializeAllContacts(contacts)

        underTest.filterContacts(query)

        underTest.filterUiState.test {
            assertThat(expectMostRecentItem().filteredContacts).isEqualTo(contacts)
        }
    }

    @Test
    fun `test that an empty list is returned as the filtered contacts when there are no contacts available`() =
        runTest {
            underTest.filterContacts("query")

            underTest.filterUiState.test {
                assertThat(expectMostRecentItem().filteredContacts).isEqualTo(emptyList<InvitationContactInfo>())
            }
        }

    @Test
    fun `test that the filtered contact list contains only a header item and valid phone contacts`() =
        runTest {
            val contacts = listOf(
                InvitationContactInfo(
                    id = 123L,
                    type = TYPE_PHONE_CONTACT,
                    name = "query"
                ),
                InvitationContactInfo(
                    id = 124L,
                    type = TYPE_PHONE_CONTACT,
                    displayInfo = "quer"
                ),
                InvitationContactInfo(
                    id = 125L,
                    type = 100,
                    name = "query 1"
                ),
                InvitationContactInfo(
                    id = 126L,
                    type = TYPE_PHONE_CONTACT,
                    name = "query 1"
                )
            )
            underTest.initializeAllContacts(contacts)
            whenever(context.getString(R.string.contacts_phone)).thenReturn("Phone contacts")

            underTest.filterContacts("que")

            underTest.filterUiState.test {
                val expected = listOf(
                    InvitationContactInfo(
                        ID_PHONE_CONTACTS_HEADER,
                        "Phone contacts",
                        TYPE_PHONE_CONTACT_HEADER
                    ),
                    InvitationContactInfo(
                        id = 123L,
                        type = TYPE_PHONE_CONTACT,
                        name = "query"
                    ),
                    InvitationContactInfo(
                        id = 124L,
                        type = TYPE_PHONE_CONTACT,
                        displayInfo = "quer"
                    ),
                    InvitationContactInfo(
                        id = 126L,
                        type = TYPE_PHONE_CONTACT,
                        name = "query 1"
                    )
                )
                assertThat(expectMostRecentItem().filteredContacts).isEqualTo(expected)
            }
        }

    @Test
    fun `test that no contacts emitted when no local contacts are available`() =
        runTest {
            whenever(context.getString(R.string.contacts_phone)).thenReturn("Phone contacts")
            whenever(getLocalContactsUseCase()).thenReturn(emptyList())

            underTest.initializeContacts()

            underTest.filterUiState.test {
                awaitItem() // Default value
                expectNoEvents()
            }
        }

    @ParameterizedTest
    @MethodSource("provideLocalContactsWithAndWithoutPhoneNumbersAndEmail")
    fun `test that the right list of mapped contact information is returned`(
        localContacts: List<LocalContact>,
    ) = runTest {
        whenever(context.getString(R.string.contacts_phone)).thenReturn("Phone contacts")
        whenever(getLocalContactsUseCase()).thenReturn(localContacts)
        whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
        whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
            localContacts
        )

        underTest.initializeContacts()

        underTest.filterUiState.test {
            val expected = mutableListOf<InvitationContactInfo>()
            localContacts.forEach {
                val phoneNumberList = it.phoneNumbers + it.emails
                if (phoneNumberList.isNotEmpty()) {
                    expected.add(
                        InvitationContactInfo(
                            id = it.id,
                            name = it.name,
                            type = TYPE_PHONE_CONTACT,
                            filteredContactInfos = phoneNumberList,
                            displayInfo = phoneNumberList[0],
                            avatarColorResId = R.color.grey_500_grey_400
                        )
                    )
                }
            }
            assertThat(expectMostRecentItem().filteredContacts).isEqualTo(
                listOf(
                    InvitationContactInfo(
                        id = ID_PHONE_CONTACTS_HEADER,
                        name = "Phone contacts",
                        type = TYPE_PHONE_CONTACT_HEADER
                    )
                ).plus(expected)
            )
        }
    }

    private fun provideLocalContactsWithAndWithoutPhoneNumbersAndEmail() = Stream.of(
        Arguments.of(
            listOf(
                LocalContact(
                    id = 1L,
                    name = "name1",
                    phoneNumbers = listOf("08123214322")
                )
            ),
        ),
        Arguments.of(
            listOf(
                LocalContact(
                    id = 2L,
                    name = "name2",
                    emails = listOf("test2@test.com")
                )
            )
        ),
        Arguments.of(
            listOf(
                LocalContact(
                    id = 3L,
                    name = "name3"
                )
            )
        ),
        Arguments.of(
            listOf(
                LocalContact(
                    id = 4L,
                    name = "name4",
                    phoneNumbers = listOf("08123214322"),
                    emails = listOf("test4@test.com")
                )
            )
        )
    )

    @Test
    fun `test that search query state is updated given a different search query when the search query is changed`() =
        runTest {
            val newSearchQuery = "newSearchQuery"

            underTest.onSearchQueryChange(newSearchQuery)

            val actual = savedStateHandle.get<String>(key = "CONTACT_SEARCH_QUERY").orEmpty()
            assertThat(actual).isEqualTo(newSearchQuery)
        }

    @Test
    fun `test that filter contacts is invoked when the search query is changed`() =
        runTest {
            whenever(context.getString(R.string.contacts_phone)).thenReturn("Phone contacts")
            val newSearchQuery = "newSearchQuery"

            underTest.onSearchQueryChange(newSearchQuery)

            underTest.filterUiState.test {
                assertThat(expectMostRecentItem().filteredContacts).isEqualTo(emptyList<InvitationContactInfo>())
            }
        }

    @Test
    fun `test that the contact link state value is updated after successfully creating the contact link`() =
        runTest {
            whenever(createContactLinkUseCase(false)) doReturn defaultContactLink

            underTest.uiState.test {
                assertThat(expectMostRecentItem().contactLink).isEqualTo(defaultContactLink)
            }
        }

    @Test
    fun `test that the correct invitation status state is updated after successfully inviting contacts by email`() =
        runTest {
            val emails = listOf("email1@email.com", "email2@email.com", "email3@email.com")
            whenever(inviteContactWithEmailsUseCase(emails)) doReturn listOf(
                InviteContactRequest.Sent,
                InviteContactRequest.Resent,
                InviteContactRequest.Sent
            )

            underTest.inviteContactsByEmail(emails)

            underTest.uiState.test {
                val expected = InvitationStatusUiState(
                    emails = emails,
                    totalInvitationSent = 2
                )
                assertThat(expectMostRecentItem().invitationStatus).isEqualTo(expected)
            }
        }

    @Test
    fun `test that the selected contact information is updated correctly`() = runTest {
        val contactId = 1L
        val addedContactInfo = InvitationContactInfo(id = contactId, displayInfo = "(121)-234-567")
        underTest.addSelectedContactInformation(addedContactInfo)
        val updatedContactInfo = listOf(
            InvitationContactInfo(
                id = contactId,
                displayInfo = "email@email.email",
                isHighlighted = true
            )
        )

        underTest.updateSelectedContactInfo(updatedContactInfo)

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().selectedContactInformation
            ).isEqualTo(updatedContactInfo)
        }
    }

    @Test
    fun `test that the de-selected contact information is successfully removed from the existing list by a specified index`() =
        runTest {
            val contactInfoWithPhoneNumber = InvitationContactInfo(
                id = 1L,
                displayInfo = "(121)-234-567"
            )
            val contactInfoWithEmail = InvitationContactInfo(
                id = 2L,
                displayInfo = "email@email.email"
            )
            underTest.addSelectedContactInformation(contactInfoWithPhoneNumber)
            underTest.addSelectedContactInformation(contactInfoWithEmail)

            underTest.removeSelectedContactInformationAt(1)

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().selectedContactInformation
                ).isEqualTo(
                    listOf(contactInfoWithPhoneNumber)
                )
            }
        }

    @Test
    fun `test that the de-selected contact information is successfully removed from the existing list by a specified contact info`() =
        runTest {
            val contactInfoWithPhoneNumber = InvitationContactInfo(
                id = 1L,
                displayInfo = "(121)-234-567"
            )
            val contactInfoWithEmail = InvitationContactInfo(
                id = 2L,
                displayInfo = "email@email.email"
            )
            underTest.addSelectedContactInformation(contactInfoWithPhoneNumber)
            underTest.addSelectedContactInformation(contactInfoWithEmail)

            underTest.removeSelectedContactInformationByContact(contactInfoWithEmail)

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().selectedContactInformation
                ).isEqualTo(
                    listOf(contactInfoWithPhoneNumber)
                )
            }
        }

    @ParameterizedTest
    @MethodSource("provideSameContactInformation")
    fun `test that true is returned when the contact ID and display information are the same`(
        firstContact: InvitationContactInfo,
        secondContact: InvitationContactInfo,
    ) {
        val actual = underTest.isTheSameContact(firstContact, secondContact)

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
        val actual = underTest.isTheSameContact(firstContact, secondContact)

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

    @AfterEach
    fun tearDown() {
        reset(
            context,
            createContactLinkUseCase,
            getLocalContactsUseCase,
            filterLocalContactsByEmailUseCase,
            filterPendingOrAcceptedLocalContactsByEmailUseCase,
            inviteContactWithEmailsUseCase
        )
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }
}
