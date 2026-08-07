package mega.privacy.android.app.mediaplayer.service

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioPlayerServiceTest {

    private val appPackage = "mega.privacy.android.app"

    @Test
    fun `test that isPackageTrusted returns true for own package`() {
        assertThat(AudioPlayerService.isPackageTrusted(appPackage, appPackage)).isTrue()
    }

    @Test
    fun `test that isPackageTrusted returns true for android system package`() {
        assertThat(AudioPlayerService.isPackageTrusted("android", appPackage)).isTrue()
    }

    @Test
    fun `test that isPackageTrusted returns false for third-party package`() {
        assertThat(AudioPlayerService.isPackageTrusted("com.attacker.app", appPackage)).isFalse()
    }

    @Test
    fun `test that isPackageTrusted returns false for package that is a prefix of own package`() {
        assertThat(AudioPlayerService.isPackageTrusted("mega.privacy", appPackage)).isFalse()
    }

    @Test
    fun `test that isPackageTrusted returns false for package that extends own package`() {
        assertThat(AudioPlayerService.isPackageTrusted("$appPackage.evil", appPackage)).isFalse()
    }

    @Test
    fun `test that isPackageTrusted returns false for empty package`() {
        assertThat(AudioPlayerService.isPackageTrusted("", appPackage)).isFalse()
    }
}
