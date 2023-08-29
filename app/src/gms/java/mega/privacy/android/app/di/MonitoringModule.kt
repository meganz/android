package mega.privacy.android.app.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.monitoring.CrashReporter
import mega.privacy.android.app.monitoring.FirebaseCrashReporter
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MonitoringModule {

    @Provides
    fun provideFirebaseCrashlytics(@ApplicationContext context: Context): FirebaseCrashlytics {
        initializeFirebaseIfNeeded(context)
        return FirebaseCrashlytics.getInstance()
    }

    @Provides
    fun provideFirebasePerformance(): FirebasePerformance =
        FirebasePerformance.getInstance()

    @Singleton
    @Provides
    fun provideCrashReporter(firebaseCrashlytics: FirebaseCrashlytics): CrashReporter =
        FirebaseCrashReporter(firebaseCrashlytics)

    /**
     * If the default app was not initialized, FirebaseApp.getInstance() throws an [IllegalStateException].
     * Then if that happens, it is needed to initialize the app before getting the instance.
     *
     * @param context   Application context.
     */
    private fun initializeFirebaseIfNeeded(context: Context) {
        try {
            FirebaseApp.getInstance()
        } catch (ignored: Exception) {
            Timber.w(ignored)
            FirebaseApp.initializeApp(context)
        }
    }
}
