package mega.privacy.android.app.mediaplayer.service

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioPlayerServiceTest {

    private val appPackage = "mega.privacy.android.app"

    @BeforeEach
    fun resetRunningFlag() {
        AudioPlayerService.isRunning = false
    }

    @Test
    fun `test that start marks the service as running when the system accepts the request`() {
        val context = mock<Context>()

        AudioPlayerService.start(context, mock())

        assertThat(AudioPlayerService.isRunning).isTrue()
    }

    @Test
    fun `test that start restores the previous running flag when the request throws`() {
        val context = mock<Context> {
            on { startService(any()) } doThrow IllegalStateException("background start")
        }

        assertThrows<IllegalStateException> { AudioPlayerService.start(context, mock()) }

        assertThat(AudioPlayerService.isRunning).isFalse()
    }

    @Test
    fun `test that stopAudioPlayer does not start the service when it is not running`() {
        val context = mock<Context>()

        AudioPlayerService.stopAudioPlayer(context)

        verify(context, never()).startService(any())
    }

    @Test
    fun `test that stopAudioPlayer swallows a failed startService when the service is running`() {
        AudioPlayerService.isRunning = true
        val context = mock<Context> {
            on { startService(any()) } doThrow IllegalStateException("background start")
        }

        AudioPlayerService.stopAudioPlayer(context)

        verify(context).startService(any())
    }

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
