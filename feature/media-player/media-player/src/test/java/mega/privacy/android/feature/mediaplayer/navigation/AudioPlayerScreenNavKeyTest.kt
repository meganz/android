package mega.privacy.android.feature.mediaplayer.navigation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import org.junit.jupiter.api.Test

/**
 * Covers the marker interfaces of [AudioPlayerScreenNavKey].
 *
 * Nothing fails to compile when a marker is dropped, and the navigation handler holds unmarked
 * destinations back silently, so the symptom would be a public link that never opens the player
 * for a logged out user.
 */
internal class AudioPlayerScreenNavKeyTest {

    @Test
    fun `test that the key is reachable without a session`() {
        val underTest = AudioPlayerScreenNavKey("launch-id")

        assertThat(underTest).isInstanceOf(NoSessionNavKey.Optional::class.java)
    }

    @Test
    fun `test that the key does not redirect a logged in user away`() {
        val underTest = AudioPlayerScreenNavKey("launch-id")

        assertThat(underTest).isNotInstanceOf(NoSessionNavKey.Mandatory::class.java)
    }
}
