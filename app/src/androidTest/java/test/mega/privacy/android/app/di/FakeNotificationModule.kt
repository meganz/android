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
 * Replaces [NotificationModule] so instrumented tests never request a real push token.
 *
 * Every journey logs in, which runs `PushTokenPostLoginInitialiser` and asks Firebase for an FCM
 * token. Handing out a [FirebaseMessaging] that reports none keeps the initialiser on its
 * "no need to register" path, so tests neither register a device against the API nor depend on
 * Play services being reachable.
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
