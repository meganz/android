package mega.privacy.android.app.main

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.InviteContactViewModel.Companion.ID_PHONE_CONTACTS_HEADER
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteContactViewModelTest {

    private lateinit var underTest: InviteContactViewModel

    private val context: Context = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()

    @BeforeEach
    fun setup() = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)

        underTest = InviteContactViewModel(
            applicationContext = context,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            defaultDispatcher = extension.testDispatcher
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

    @AfterEach
    fun tearDown() {
        reset(
            context,
            getFeatureFlagValueUseCase
        )
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }
}
