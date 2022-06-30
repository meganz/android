package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.logging.LegacyLoggingSettingsFacade

@Module
@InstallIn(SingletonComponent::class)
class LegacyLoggingModule {

    @Provides
    fun provideLegacyLoggingSettings(legacyLoggingSettingsFacade: LegacyLoggingSettingsFacade): LegacyLoggingSettings =
        legacyLoggingSettingsFacade


}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LegacyLoggingEntryPoint {
    var legacyLoggingSettings: LegacyLoggingSettings
}