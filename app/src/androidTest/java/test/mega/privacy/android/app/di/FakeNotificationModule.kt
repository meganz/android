package mega.privacy.android.app.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.notification.NotificationModule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import javax.inject.Singleton

/**
 * Replaces [NotificationModule] so instrumented tests never ask Firebase for a push token.
 *
 * Every journey logs in, which runs `PushTokenPostLoginInitialiser` and requests a token.
 * Firebase answers by starting `MegaMessageService`, whose `@AndroidEntryPoint` injection
 * resolves the Hilt component — but under `HiltTestApplication` that component only exists while
 * a `HiltAndroidRule` holds it, so creating the service throws
 * `IllegalStateException: The component was not created` and kills the app process. Landing
 * between tests it is survivable; landing inside one it aborts the whole instrumentation run.
 *
 * Handing out a [FirebaseMessaging] that reports no token keeps the initialiser on its
 * "no need to register" path, so nothing ever starts the service.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NotificationModule::class],
)
object FakeNotificationModule {

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = mock {
        on { token } doReturn Tasks.forResult(null)
    }

    @Provides
    fun provideFirebaseAnalytics() = Firebase.analytics
}
