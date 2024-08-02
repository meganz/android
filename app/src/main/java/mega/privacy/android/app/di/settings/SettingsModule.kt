package mega.privacy.android.app.di.settings

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.presentation.settings.model.PreferenceResource
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.PutPreference

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
        @ElementsIntoSet
        fun providePreferenceResourceSet(): Set<@JvmSuppressWildcards PreferenceResource> = setOf()

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
