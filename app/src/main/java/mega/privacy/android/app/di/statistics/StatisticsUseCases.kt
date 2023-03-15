package mega.privacy.android.app.di.statistics

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.StatisticsRepository
import mega.privacy.android.domain.usecase.DefaultSendStatisticsMediaDiscovery
import mega.privacy.android.domain.usecase.DefaultSendStatisticsMeetings
import mega.privacy.android.domain.usecase.SendStatisticsEvent
import mega.privacy.android.domain.usecase.SendStatisticsMediaDiscovery
import mega.privacy.android.domain.usecase.SendStatisticsMeetings

@Module
@InstallIn(ViewModelComponent::class)
abstract class StatisticsUseCases {

    @Binds
    abstract fun bindSendStatisticsMediaDiscovery(implementation: DefaultSendStatisticsMediaDiscovery): SendStatisticsMediaDiscovery

    @Binds
    abstract fun bindSendStatisticsMeetings(implementation: DefaultSendStatisticsMeetings): SendStatisticsMeetings

    companion object {
        @Provides
        fun provideSendStatisticsEvent(statisticsRepository: StatisticsRepository): SendStatisticsEvent =
            SendStatisticsEvent(statisticsRepository::sendEvent)
    }
}