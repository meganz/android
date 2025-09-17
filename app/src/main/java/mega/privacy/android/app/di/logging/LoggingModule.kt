package mega.privacy.android.app.di.logging

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.logging.Logger
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LoggingModule {
    @Provides
    @Singleton
    fun provideLogger(): Logger = object : Logger {
        override fun d(message: String) {
            Timber.d(message)
        }

        override fun i(message: String) {
            Timber.i(message)
        }

        override fun w(message: String) {
            Timber.w(message)
        }

        override fun e(throwable: Throwable?, message: String) {
            Timber.e(throwable, message)
        }
    }
}