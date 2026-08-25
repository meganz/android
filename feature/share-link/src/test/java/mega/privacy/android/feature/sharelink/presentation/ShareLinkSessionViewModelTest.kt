package mega.privacy.android.feature.sharelink.presentation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sharelink.session.LinkPassword
import org.junit.jupiter.api.Test

class ShareLinkSessionViewModelTest {

    @Test
    fun `test that a session starts with nothing held`() {
        val underTest = ShareLinkSessionViewModel()

        assertThat(underTest.session.password.value).isNull()
        assertThat(underTest.session.isKeySeparate.value).isFalse()
        assertThat(underTest.session.publicLink).isNull()
    }

    @Test
    fun `test that a new instance does not inherit the previous one's session`() {
        // The whole point of the nav scope: leaving the Share link flow clears this ViewModel, and
        // the next visit must not see what the last one held for the same node.
        val previous = ShareLinkSessionViewModel()
        previous.session.password.value = LinkPassword(PASSWORD, ENCRYPTED_LINK)
        previous.session.isKeySeparate.value = true
        previous.session.publicLink = PUBLIC_LINK

        val underTest = ShareLinkSessionViewModel()

        assertThat(underTest.session.password.value).isNull()
        assertThat(underTest.session.isKeySeparate.value).isFalse()
        assertThat(underTest.session.publicLink).isNull()
    }

    private companion object {
        const val PASSWORD = "Str0ngP@ss"
        const val PUBLIC_LINK = "https://mega.nz/file/abc"
        const val ENCRYPTED_LINK = "https://mega.nz/#P!encrypted"
    }
}
