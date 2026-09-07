package mega.privacy.android.app

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.testing.HiltTestApplication
import timber.log.Timber

class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application? {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)

        // Firebase generates a registration token on its own and delivers it through
        // MegaMessageService, which cannot be created under HiltTestApplication and takes the app
        // process down with it - see FakeNotificationModule, which stops the app's own token
        // request. This stops Firebase's automatic one.
        runCatching { FirebaseMessaging.getInstance().isAutoInitEnabled = false }
            .onFailure { Timber.w(it, "Could not disable Firebase messaging auto-init") }
    }
}
