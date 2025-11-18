package mega.privacy.android.app.presentation.contact.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.PENDING_CONTACTS_LINK
import mega.privacy.android.navigation.destination.ContactsNavKey
import mega.privacy.android.navigation.destination.ContactsNavKey.NavType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactsDeepLinkHandlerTest {
    private lateinit var underTest: ContactsDeepLinkHandler

    @BeforeAll
    fun setup() {
        underTest = ContactsDeepLinkHandler()
    }

    @Test
    fun `test that correct nav key is returned when uri matches PENDING_CONTACTS_LINK pattern type`() =
        runTest {
            val uriString = "https://mega.nz/fm/ipc"
            val expected = ContactsNavKey(NavType.ReceivedRequests)
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, PENDING_CONTACTS_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match PENDING_CONTACTS_LINK pattern type`() =
        runTest {
            val uriString = "https://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK)

            assertThat(actual).isNull()
        }
}

