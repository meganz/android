package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.GetCurrentTimeStringFromCalendar
import mega.privacy.android.domain.usecase.GetCurrentTimeString

/**
 * Logging module
 *
 * Provides logging specific dependencies
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Binds
    abstract fun bindGetCurrentTimeString(implementation: GetCurrentTimeStringFromCalendar): GetCurrentTimeString

}