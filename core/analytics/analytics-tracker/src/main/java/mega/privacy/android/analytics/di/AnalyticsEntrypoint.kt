package mega.privacy.android.analytics.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.analytics.tracker.AnalyticsTracker

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsEntrypoint {
    fun provideAnalyticsTracker(): AnalyticsTracker
}