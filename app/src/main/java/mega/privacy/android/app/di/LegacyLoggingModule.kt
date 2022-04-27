package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.featuretoggle.PurgeLogsToggle
import mega.privacy.android.app.logging.*

@Module
@InstallIn(SingletonComponent::class)
class LegacyLoggingModule {

    @Provides
    fun provideLegacyLoggingSettings(legacyLoggingSettingsFacade: LegacyLoggingSettingsFacade): LegacyLoggingSettings =
        if (PurgeLogsToggle.enabled) {
            legacyLoggingSettingsFacade
        } else {
            LegacyLogUtil()
        }

    @Provides
    fun provideLegacyLog(): LegacyLog =
        if (PurgeLogsToggle.enabled) {
            TimberLegacyLog()
        } else {
            LegacyLogUtil()
        }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LegacyLoggingEntryPoint {
    var legacyLoggingSettings: LegacyLoggingSettings
    var legacyLog: LegacyLog
}