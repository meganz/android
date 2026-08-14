package mega.privacy.android.app.meeting

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.navigation.destination.ChatNavKey
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Robolectric tests for [CallServiceStarter].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class CallServiceStarterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val underTest = CallServiceStarter(context)

    @Test
    fun `test that no service is started when the chat id is invalid`() {
        underTest(MEGACHAT_INVALID_HANDLE)

        assertThat(shadowOf(context as Application).nextStartedService).isNull()
    }

    @Test
    fun `test that the call service is started with the chat id when the chat id is valid`() {
        val chatId = 123L

        underTest(chatId)

        val startedIntent = shadowOf(context as Application).nextStartedService
        assertThat(startedIntent.component?.className).isEqualTo(CallService::class.java.name)
        assertThat(
            startedIntent.getLongExtra(ChatNavKey.LEGACY_CHAT_ID, MEGACHAT_INVALID_HANDLE)
        ).isEqualTo(chatId)
    }
}
