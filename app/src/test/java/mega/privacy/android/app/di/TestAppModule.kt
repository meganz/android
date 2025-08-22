package mega.privacy.android.app.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.multibindings.ElementsIntoSet
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.app.appstate.global.event.AppDialogsEventQueueReceiver
import mega.privacy.android.data.database.LegacyDatabaseMigration
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.navigation.MegaActivityResultContract
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.dialog.AppDialogsEventQueue
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
    fun provideGetThemeModePreference(): MonitorThemeModeUseCase =
        mock { on { invoke() }.thenReturn(emptyFlow()) }

    @Provides
    fun provideAppNavigator(): MegaNavigator = mock()

    @Provides
    fun provideMegaActivityResultContract(): MegaActivityResultContract = mock()

    @Provides
    fun provideLegacyDatabaseMigration(): LegacyDatabaseMigration = mock()

    @Provides
    @ElementsIntoSet
    fun provideMainNavItems(): Set<@JvmSuppressWildcards MainNavItem> =
        emptySet<MainNavItem>()

    @Provides
    @ElementsIntoSet
    fun provideFeatureDestinations(): Set<@JvmSuppressWildcards FeatureDestination> =
        emptySet<FeatureDestination>()

    @Provides
    fun provideDisableChatApiUseCase(): DisableChatApiUseCase = mock()

    @Provides
    fun provideAppDialogsEventQueue(): AppDialogsEventQueue = mock()

    @Provides
    fun provideAppDialogsEventQueueReceiver(): AppDialogsEventQueueReceiver = mock()
}