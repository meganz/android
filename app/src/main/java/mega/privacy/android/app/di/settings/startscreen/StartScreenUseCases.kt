package mega.privacy.android.app.di.settings.startscreen

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.presentation.settings.startscreen.model.StartScreenOptionMapper
import mega.privacy.android.app.presentation.settings.startscreen.model.mapStartScreenOption
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.DefaultMonitorStartScreenPreference
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
     * @param useCase
     */
    @Provides
    fun provideMonitorStartScreenPreference(useCase: DefaultMonitorStartScreenPreference): MonitorStartScreenPreference =
        useCase
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