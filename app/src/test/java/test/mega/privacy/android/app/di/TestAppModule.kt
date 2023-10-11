package test.mega.privacy.android.app.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.app.di.AppModule
import mega.privacy.android.data.database.LegacyDatabaseMigration
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {
    @MegaApi
    @Provides
    fun provideMegaApi(): MegaApiAndroid = mock()

    @MegaApiFolder
    @Provides
    fun provideMegaApiFolder(): MegaApiAndroid = mock()

    @Provides
    fun provideMegaChatApi(): MegaChatApiAndroid = mock()

    @Provides
    fun providePreferences(): SharedPreferences = mock()

    @Provides
    fun provideGetThemeModePreference(): GetThemeMode =
        mock { on { invoke() }.thenReturn(emptyFlow()) }

    @Provides
    fun provideAppNavigator(): MegaNavigator = mock()

    @Provides
    fun provideLegacyDatabaseMigration(): LegacyDatabaseMigration = mock()
}