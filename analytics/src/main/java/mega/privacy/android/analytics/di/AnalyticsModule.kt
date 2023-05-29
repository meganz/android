package mega.privacy.android.analytics.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.analytics.tracker.AnalyticsTrackerImpl

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalyticsTracker(implementation: AnalyticsTrackerImpl): AnalyticsTracker
}