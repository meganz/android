package mega.privacy.android.app.di.settings

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.presentation.settings.model.PreferenceResource
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.SupportRepository
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.GetSupportEmail
import mega.privacy.android.domain.usecase.PutPreference
import mega.privacy.android.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.domain.usecase.SetSdkLogsEnabled

/**
 * Settings module
 *
 * Provides dependencies used by multiple screens in the settings package
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    companion object {
        @Provides
        fun provideGetSupportEmail(supportRepository: SupportRepository): GetSupportEmail =
            GetSupportEmail(supportRepository::getSupportEmail)

        @Provides
        @ElementsIntoSet
        fun providePreferenceResourceSet(): Set<@JvmSuppressWildcards PreferenceResource> = setOf()

        @Provides
        fun provideAreChatLogsEnabled(repository: LoggingRepository): AreChatLogsEnabled =
            AreChatLogsEnabled(repository::isChatLoggingEnabled)

        @Provides
        fun provideAreSdkLogsEnabled(repository: LoggingRepository): AreSdkLogsEnabled =
            AreSdkLogsEnabled(repository::isSdkLoggingEnabled)

        @Provides
        fun provideSetSdkLogsEnabled(repository: LoggingRepository): SetSdkLogsEnabled =
            SetSdkLogsEnabled(repository::setSdkLoggingEnabled)

        @Provides
        fun provideSetChatLogsEnabled(repository: LoggingRepository): SetChatLogsEnabled =
            SetChatLogsEnabled(repository::setChatLoggingEnabled)

        @Provides
        fun providePutStringPreference(settingsRepository: SettingsRepository): PutPreference<String> =
            PutPreference(settingsRepository::setStringPreference)

        @Provides
        fun providePutStringSetPreference(settingsRepository: SettingsRepository): PutPreference<MutableSet<String>> =
            PutPreference(settingsRepository::setStringSetPreference)

        @Provides
        fun providePutIntPreference(settingsRepository: SettingsRepository): PutPreference<Int> =
            PutPreference(settingsRepository::setIntPreference)

        @Provides
        fun providePutLongPreference(settingsRepository: SettingsRepository): PutPreference<Long> =
            PutPreference(settingsRepository::setLongPreference)

        @Provides
        fun providePutFloatPreference(settingsRepository: SettingsRepository): PutPreference<Float> =
            PutPreference(settingsRepository::setFloatPreference)

        @Provides
        fun providePutBooleanPreference(settingsRepository: SettingsRepository): PutPreference<Boolean> =
            PutPreference(settingsRepository::setBooleanPreference)

        @Provides
        fun provideGetStringPreference(settingsRepository: SettingsRepository): GetPreference<String?> =
            GetPreference(settingsRepository::monitorStringPreference)

        @Provides
        fun provideGetStringSetPreference(settingsRepository: SettingsRepository): GetPreference<MutableSet<String>?> =
            GetPreference(settingsRepository::monitorStringSetPreference)

        @Provides
        fun provideGetIntPreference(settingsRepository: SettingsRepository): GetPreference<Int> =
            GetPreference(settingsRepository::monitorIntPreference)

        @Provides
        fun provideGetLongPreference(settingsRepository: SettingsRepository): GetPreference<Long> =
            GetPreference(settingsRepository::monitorLongPreference)

        @Provides
        fun provideGetFloatPreference(settingsRepository: SettingsRepository): GetPreference<Float> =
            GetPreference(settingsRepository::monitorFloatPreference)

        @Provides
        fun provideGetBooleanPreference(settingsRepository: SettingsRepository): GetPreference<Boolean> =
            GetPreference(settingsRepository::monitorBooleanPreference)
    }

}
