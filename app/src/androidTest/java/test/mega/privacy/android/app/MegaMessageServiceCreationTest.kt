package mega.privacy.android.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.service.push.MegaMessageService
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression cover for the push service being created without a Hilt component.
 *
 * Google Play services binds [MegaMessageService] on its own schedule, which is not synchronised
 * with test boundaries. Under `HiltTestApplication` the singleton component exists only for the
 * duration of a test case, so a binding that lands outside one used to fail the service's
 * `@AndroidEntryPoint` injection and take the whole app process down — aborting entire
 * instrumentation runs rather than failing a single test.
 *
 * This test deliberately carries no `@HiltAndroidTest` and no `HiltAndroidRule`, so no component
 * exists while it runs. With the test orchestrator giving every test method its own process, that
 * window is reproduced on demand instead of being waited for.
 */
class MegaMessageServiceCreationTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun serviceIsCreatedWhenNoHiltComponentExists() {
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) =
                connected.countDown()

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }

        val bound = context.bindService(
            Intent(context, MegaMessageService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        assertThat(bound).isTrue()

        try {
            assertThat(connected.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val BIND_TIMEOUT_SECONDS = 10L
    }
}
