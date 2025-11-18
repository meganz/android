package mega.privacy.android.app.presentation.chat.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.NEW_MESSAGE_CHAT_LINK
import mega.privacy.android.navigation.destination.ChatsNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatsDeepLinkHandlerTest {
    private lateinit var underTest: ChatsDeepLinkHandler


    @BeforeAll
    fun setup() {
        underTest = ChatsDeepLinkHandler()
    }

    @Test
    fun `test that correct nav key is returned when uri matches NEW_MESSAGE_CHAT_LINK pattern type`() =
        runTest {
            val uriString = "https://mega.nz/fm/chat"
            val expected = ChatsNavKey
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, NEW_MESSAGE_CHAT_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match NEW_MESSAGE_CHAT_LINK pattern type`() =
        runTest {
            val uriString = "https://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK)

            assertThat(actual).isNull()
        }
}

