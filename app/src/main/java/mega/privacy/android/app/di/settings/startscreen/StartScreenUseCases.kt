package mega.privacy.android.app.di.settings.startscreen

import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.presentation.settings.startscreen.StartScreenEventWrapper
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.model.mapStartScreenOption
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.SetStartScreenPreference

/**
 * Start screen use cases
 *
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class StartScreenUseCases {

    companion object {

        /**
         * Provide set start screen preference
         *
         * @param repository
         */
        @Provides
        fun provideSetStartScreenPreference(repository: SettingsRepository): SetStartScreenPreference =
            SetStartScreenPreference(repository::setPreferredStartScreen)

        /**
         * Provide start screen option mapper
         */
        @Provides
        fun provideStartScreenOptionMapper(): StartScreenOptionMapper = ::mapStartScreenOption

        //        TODO("Remove this injection once all code has been refactored to use monitor instead")
        /**
         * Provide live event bus
         */
        @Provides
        fun provideLiveEventBus(): StartScreenEventWrapper =
            LiveEventBus.get(EventConstants.EVENT_UPDATE_START_SCREEN, Int::class.java)::post

    }
}


/**
 * Temporary module to provide the start screen use case for the singleton component used in static class injection
 */
@Module
@InstallIn(SingletonComponent::class)
class TempStartScreenUseCaseStaticModule {
    /**
     * Provide monitor start screen preference
     *
     * @param repository
     */
    @Provides
    fun provideMonitorStartScreenPreference(repository: SettingsRepository): MonitorStartScreenPreference =
        MonitorStartScreenPreference(repository::monitorPreferredStartScreen)
}

/**
 * This method is to inject MonitorStartScreenPreference into static classes by Hilt
 */
fun getMonitorStartScreenPreference(): MonitorStartScreenPreference =
    EntryPointAccessors.fromApplication(
        MegaApplication.getInstance(),
        MonitorStartScreenPreferenceEntryPoint::class.java).monitorStartScreenPreference

/**
 * This interface is needed to inject DatabaseHandler by Hilt
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MonitorStartScreenPreferenceEntryPoint {
    var monitorStartScreenPreference: MonitorStartScreenPreference
}