package mega.privacy.android.analytics.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.analytics.EventSenderImpl
import mega.privacy.android.analytics.ViewIdProviderImpl
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.analytics.tracker.AnalyticsTrackerImpl
import mega.privacy.mobile.analytics.event.tracking.Tracker

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalyticsTracker(implementation: AnalyticsTrackerImpl): AnalyticsTracker

    companion object {
        @Provides
        fun provideTracker(
            viewIdProvider: ViewIdProviderImpl,
            eventSender: EventSenderImpl,
        ): Tracker = Tracker(viewIdProvider, eventSender)
    }
}