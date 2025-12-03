package mega.privacy.android.app.presentation.contact.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.contact.link.dialog.ContactLinkDialogNavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CONTACT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.PENDING_CONTACTS_LINK
import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.contact.ContactLinkQueryFromLinkUseCase
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey.NavType
import mega.privacy.android.navigation.destination.LegacyOpenLinkAfterFetchNodes
import mega.privacy.android.navigation.destination.WebSiteNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactsDeepLinkHandlerTest {
    private lateinit var underTest: ContactsDeepLinkHandler

    private val contactLinkQueryFromLinkUseCase = mock<ContactLinkQueryFromLinkUseCase>()
    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeAll
    fun setup() {
        underTest = ContactsDeepLinkHandler(
            contactLinkQueryFromLinkUseCase = contactLinkQueryFromLinkUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            contactLinkQueryFromLinkUseCase,
            rootNodeExistsUseCase,
            snackbarEventQueue,
        )
        wheneverBlocking { rootNodeExistsUseCase() } doReturn true
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches PENDING_CONTACTS_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.nz/fm/ipc"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest
            .getNavKeysInternal(uri, PENDING_CONTACTS_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(ContactsNavKey(NavType.ReceivedRequests))
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct NavKey is returned when regex pattern type is CONTACT_LINK and root node does not exist`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.nz/!Cwhatever"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(rootNodeExistsUseCase()) doReturn false

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.CONTACT_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).isEqualTo(listOf(LegacyOpenLinkAfterFetchNodes(uriString)))
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches CONTACT_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.nz/!Cwhatever"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }
        val contactLinkQueryResult = mock<ContactLinkQueryResult>()

        whenever(contactLinkQueryFromLinkUseCase(uriString)) doReturn contactLinkQueryResult

        val actual = underTest.getNavKeysInternal(uri, CONTACT_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(ContactLinkDialogNavKey(contactLinkQueryResult))
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches CONTACT_LINK pattern type if use case returns null`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://mega.nz/!Cwhatever"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(contactLinkQueryFromLinkUseCase(uriString)) doReturn null

        val actual = underTest
            .getNavKeysInternal(uri, CONTACT_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(WebSiteNavKey(uriString))
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match PENDING_CONTACTS_LINK or CONTACT_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeysInternal(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }
}

