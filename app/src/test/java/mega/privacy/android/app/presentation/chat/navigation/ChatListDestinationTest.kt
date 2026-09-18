package mega.privacy.android.app.presentation.chat.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.transparent.TransparentMetadata
import mega.privacy.android.navigation.destination.ChatListNavKey
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatListDestinationTest {

    private val underTest: (NavKey) -> NavEntry<NavKey> =
        entryProvider { chatListDestination(navigationHandler = mock<NavigationHandler>()) }

    /**
     * A transparent entry is rendered as an overlay above the scene beneath it, which composes the
     * chat list a second time while a pop onto it settles and crashes the app (AND-25048).
     */
    @Test
    fun `test that the chat list entry is not transparent`() {
        val entry = underTest(ChatListNavKey())

        assertThat(entry.metadata).doesNotContainKey(TransparentMetadata.KEY)
    }
}
