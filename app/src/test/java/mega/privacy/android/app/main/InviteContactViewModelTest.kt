package mega.privacy.android.app.main

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.presentation.contact.invite.contact.InviteContactViewModel
import mega.privacy.android.app.presentation.contact.invite.contact.InviteContactViewModel.Companion.ID_PHONE_CONTACTS_HEADER
import mega.privacy.android.app.presentation.contact.invite.contact.mapper.InvitationContactInfoUiMapper
import mega.privacy.android.app.presentation.contact.invite.contact.mapper.InvitationStatusMessageUiMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.usecase.contact.FilterLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.FilterPendingOrAcceptedLocalContactsByEmailUseCase
import mega.privacy.android.domain.usecase.contact.GetLocalContactsUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailsUseCase
import mega.privacy.android.domain.usecase.meeting.AreThereOngoingVideoCallsUseCase
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

    private val createContactLinkUseCase: CreateContactLinkUseCase = mock()
    private val getLocalContactsUseCase: GetLocalContactsUseCase = mock()
    private val filterLocalContactsByEmailUseCase: FilterLocalContactsByEmailUseCase = mock()
    private val filterPendingOrAcceptedLocalContactsByEmailUseCase: FilterPendingOrAcceptedLocalContactsByEmailUseCase =
        mock()
    private val inviteContactWithEmailsUseCase: InviteContactWithEmailsUseCase = mock()
    private val areThereOngoingVideoCallsUseCase: AreThereOngoingVideoCallsUseCase = mock()
    private val defaultQuery = "defaultQuery"
    private val defaultContactLink = "https://mega.nz/C!wf8jTYRB"

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var invitationContactInfoUiMapper: InvitationContactInfoUiMapper
    private lateinit var invitationStatusMessageUiMapper: InvitationStatusMessageUiMapper

    @BeforeEach
    fun setup() = runTest {
        whenever(createContactLinkUseCase(false)) doReturn defaultContactLink

        savedStateHandle = SavedStateHandle(mapOf("CONTACT_SEARCH_QUERY" to defaultQuery))
        invitationContactInfoUiMapper = InvitationContactInfoUiMapper(
            defaultDispatcher = extension.testDispatcher
        )
        invitationStatusMessageUiMapper = InvitationStatusMessageUiMapper()
        initializeViewModel()
    }

    private fun initializeViewModel() {
        underTest = InviteContactViewModel(
            defaultDispatcher = extension.testDispatcher,
            getLocalContactsUseCase = getLocalContactsUseCase,
            filterLocalContactsByEmailUseCase = filterLocalContactsByEmailUseCase,
            filterPendingOrAcceptedLocalContactsByEmailUseCase = filterPendingOrAcceptedLocalContactsByEmailUseCase,
            createContactLinkUseCase = createContactLinkUseCase,
            inviteContactWithEmailsUseCase = inviteContactWithEmailsUseCase,
            areThereOngoingVideoCallsUseCase = areThereOngoingVideoCallsUseCase,
            invitationContactInfoUiMapper = invitationContactInfoUiMapper,
            invitationStatusMessageUiMapper = invitationStatusMessageUiMapper,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `test that all contacts should be initialized when given a contact list`() = runTest {
        val localContacts = listOf(LocalContact(id = 1L))
        whenever(getLocalContactsUseCase()).thenReturn(localContacts)
        whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
        whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
            localContacts
        )

        underTest.initializeContacts()

        assertThat(underTest.allContacts).isEqualTo(invitationContactInfoUiMapper(localContacts))
    }

    @Test
    fun `test that filtered contacts list should be initialized when given a contact list`() =
        runTest {
            val localContacts = listOf(LocalContact(id = 1L))
            whenever(getLocalContactsUseCase()).thenReturn(localContacts)
            whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
            whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
                localContacts
            )

            underTest.initializeContacts()

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().filteredContacts
                ).isEqualTo(
                    invitationContactInfoUiMapper(localContacts)
                )
            }
        }

    @Test
    fun `test that the highlighted value should be set correctly`() = runTest {
        val localContacts = listOf(
            LocalContact(id = 1L, name = "firstContact", phoneNumbers = listOf("123123123")),
            LocalContact(id = 2L, name = "secondContact", phoneNumbers = listOf("321321321"))
        )
        whenever(getLocalContactsUseCase()).thenReturn(localContacts)
        whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
        whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
            localContacts
        )
        underTest.initializeContacts()
        val mappedContacts = invitationContactInfoUiMapper(localContacts)

        underTest.toggleContactHighlightedInfo(mappedContacts[1].copy(isHighlighted = true))
        underTest.toggleContactHighlightedInfo(mappedContacts[2])

        val expected = listOf(
            mappedContacts[0], // Header item
            mappedContacts[1].copy(isHighlighted = false),
            mappedContacts[2].copy(isHighlighted = true)
        )
        underTest.uiState.test {
            assertThat(expectMostRecentItem().filteredContacts).isEqualTo(expected)
        }
        assertThat(underTest.allContacts).isEqualTo(expected)
    }

    @Test
    fun `test that the highlighted value should be set based on a given value`() = runTest {
        val localContacts = listOf(
            LocalContact(id = 1L, name = "firstContact", phoneNumbers = listOf("123123123")),
            LocalContact(id = 2L, name = "secondContact", phoneNumbers = listOf("321321321"))
        )
        whenever(getLocalContactsUseCase()).thenReturn(localContacts)
        whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
        whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
            localContacts
        )
        underTest.initializeContacts()
        val mappedContacts = invitationContactInfoUiMapper(localContacts)

        underTest.toggleContactHighlightedInfo(mappedContacts[1], true)
        underTest.toggleContactHighlightedInfo(mappedContacts[2].copy(isHighlighted = true), false)

        val expected = listOf(
            mappedContacts[0], // Header item
            mappedContacts[1].copy(isHighlighted = true),
            mappedContacts[2].copy(isHighlighted = false)
        )
        underTest.uiState.test {
            assertThat(expectMostRecentItem().filteredContacts).isEqualTo(expected)
        }
        assertThat(underTest.allContacts).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when the query is {0}")
    @NullAndEmptySource
    fun `test that the filtered contact list is reset`(query: String?) = runTest {
        val localContacts = listOf(
            LocalContact(id = 1L, name = "firstContact", phoneNumbers = listOf("123123123")),
            LocalContact(id = 2L, name = "secondContact", phoneNumbers = listOf("321321321"))
        )
        whenever(getLocalContactsUseCase()).thenReturn(localContacts)
        whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
        whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
            localContacts
        )
        underTest.initializeContacts()

        underTest.filterContacts(query)

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().filteredContacts
            ).isEqualTo(
                invitationContactInfoUiMapper(localContacts)
            )
        }
    }

    @Test
    fun `test that an empty list is returned as the filtered contacts when there are no contacts available`() =
        runTest {
            underTest.filterContacts("query")

            underTest.uiState.test {
                assertThat(expectMostRecentItem().filteredContacts).isEqualTo(emptyList<InvitationContactInfo>())
            }
        }

    @Test
    fun `test that the filtered contact list contains only a header item and valid phone contacts`() =
        runTest {
            val localContacts = listOf(
                LocalContact(
                    id = 123L,
                    name = "query",
                    phoneNumbers = listOf("123123123")
                ),
                LocalContact(
                    id = 124L,
                    name = "quer",
                    phoneNumbers = listOf("321321321")
                ),
                LocalContact(
                    id = 125L,
                    name = "astaga",
                    phoneNumbers = listOf("345345345")
                ),
                LocalContact(
                    id = 126L,
                    name = "query 1",
                    phoneNumbers = listOf("543534543")
                )
            )
            whenever(getLocalContactsUseCase()).thenReturn(localContacts)
            whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
            whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
                localContacts
            )
            underTest.initializeContacts()

            underTest.filterContacts("que")

            underTest.uiState.test {
                val expected = invitationContactInfoUiMapper(localContacts).filterNot {
                    it.name == localContacts[2].name
                }
                assertThat(expectMostRecentItem().filteredContacts).isEqualTo(expected)
            }
        }

    @Test
    fun `test that no contacts emitted when no local contacts are available`() =
        runTest {
            whenever(getLocalContactsUseCase()).thenReturn(emptyList())

            underTest.initializeContacts()

            underTest.uiState.test {
                awaitItem() // Default value
                expectNoEvents()
            }
        }

    @ParameterizedTest
    @MethodSource("provideLocalContactsWithAndWithoutPhoneNumbersAndEmail")
    fun `test that the right list of mapped contact information is returned`(
        localContacts: List<LocalContact>,
    ) = runTest {
        whenever(getLocalContactsUseCase()).thenReturn(localContacts)
        whenever(filterLocalContactsByEmailUseCase(localContacts)).thenReturn(localContacts)
        whenever(filterPendingOrAcceptedLocalContactsByEmailUseCase(localContacts)).thenReturn(
            localContacts
        )

        underTest.initializeContacts()

        underTest.uiState.test {
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
    fun `test that list of filter contacts is updated when the search query is changed`() =
        runTest {
            val newSearchQuery = "newSearchQuery"

            underTest.onSearchQueryChange(newSearchQuery)

            underTest.uiState.test {
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
    fun `test that the correct invitation status state is updated after successfully inviting contacts with email and a phone number`() =
        runTest {
            val emails = listOf(
                "email1@email.com",
                "email2@email.com",
                "email3@email.com"
            )
            val requests = listOf(
                InviteContactRequest.Sent,
                InviteContactRequest.Resent,
                InviteContactRequest.Sent
            )
            whenever(inviteContactWithEmailsUseCase(emails)) doReturn requests
            underTest.addSelectedContactInformation(
                InvitationContactInfo(id = 1L, displayInfo = emails[0])
            )
            underTest.addSelectedContactInformation(
                InvitationContactInfo(id = 2L, displayInfo = emails[1])
            )
            underTest.addSelectedContactInformation(
                InvitationContactInfo(id = 3L, displayInfo = emails[2])
            )
            val phoneNumber = "123123123123"
            underTest.addSelectedContactInformation(
                InvitationContactInfo(id = 4L, displayInfo = phoneNumber)
            )

            underTest.inviteContacts()

            underTest.uiState.test {
                val expectedStatus = invitationStatusMessageUiMapper(
                    isFromAchievement = false,
                    requests = requests,
                    emails = emails
                )
                val item = expectMostRecentItem()
                assertThat(item.invitationStatusResult).isEqualTo(expectedStatus)
                assertThat(item.pendingPhoneNumberInvitations).isEqualTo(listOf(phoneNumber))
            }
        }

    @Test
    fun `test that the pending phone numbers to be invited are set correctly when the invited contacts are only phone numbers`() =
        runTest {
            val phoneNumbers = listOf("123123123123", "3213123123")
            underTest.addSelectedContactInformation(
                InvitationContactInfo(id = 1L, displayInfo = phoneNumbers[0])
            )
            underTest.addSelectedContactInformation(
                InvitationContactInfo(id = 2L, displayInfo = phoneNumbers[1])
            )

            underTest.inviteContacts()

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().pendingPhoneNumberInvitations
                ).isEqualTo(
                    phoneNumbers
                )
            }
        }

    @Test
    fun `test that the selected contact information is updated correctly based on a contact info with multiple contacts`() =
        runTest {
            val contactId = 1L
            val phoneNumber = "(121)-234-567"
            val email = "email@email.email"
            val contactInfo = InvitationContactInfo(
                id = contactId,
                name = "name",
                displayInfo = phoneNumber,
                filteredContactInfos = listOf(
                    phoneNumber,
                    email
                )
            )
            val addedInfo = contactInfo.copy(displayInfo = phoneNumber)
            underTest.addSelectedContactInformation(addedInfo)
            val newInfo = listOf(contactInfo.copy(displayInfo = email))

            underTest.updateSelectedContactInfoByInfoWithMultipleContacts(
                newListOfSelectedContact = newInfo,
                contactInfo = contactInfo
            )

            underTest.uiState.test {
                assertThat(
                    expectMostRecentItem().selectedContactInformation
                ).isEqualTo(newInfo)
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

            underTest.removeSelectedContactInformation(contactInfoWithEmail)

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

    @Test
    fun `test that the open camera confirmation is shown when there are ongoing video calls`() =
        runTest {
            whenever(areThereOngoingVideoCallsUseCase()) doReturn true

            underTest.validateCameraAvailability()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().showOpenCameraConfirmation).isTrue()
            }
        }

    @Test
    fun `test that the open camera confirmation state is reset after successfully shown`() =
        runTest {
            whenever(areThereOngoingVideoCallsUseCase()) doReturn true

            underTest.validateCameraAvailability()
            underTest.onOpenCameraConfirmationShown()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().showOpenCameraConfirmation).isFalse()
            }
        }

    @Test
    fun `test that the QR scanner initialization is state is reset after successfully initialized`() =
        runTest {
            whenever(areThereOngoingVideoCallsUseCase()) doReturn false

            underTest.validateCameraAvailability()
            underTest.onQRScannerInitialized()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().shouldInitializeQR).isFalse()
            }
        }

    @AfterEach
    fun tearDown() {
        reset(
            createContactLinkUseCase,
            getLocalContactsUseCase,
            filterLocalContactsByEmailUseCase,
            filterPendingOrAcceptedLocalContactsByEmailUseCase,
            inviteContactWithEmailsUseCase,
            areThereOngoingVideoCallsUseCase
        )
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }
}
