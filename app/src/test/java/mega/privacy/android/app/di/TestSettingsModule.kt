package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.multibindings.ElementsIntoSet
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.di.settings.SettingsModule
import mega.privacy.android.app.di.settings.SettingsUseCases
import mega.privacy.android.app.di.settings.startscreen.TempStartScreenUseCaseStaticModule
import mega.privacy.android.app.presentation.settings.model.PreferenceResource
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.GetSupportEmailUseCase
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.PutPreference
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.SetChatImageQuality
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.call.MonitorCallSoundEnabledUseCase
import mega.privacy.android.domain.usecase.call.SetCallsSoundEnabledStateUseCase
import mega.privacy.android.domain.usecase.setting.EnableFileVersionsOption
import mega.privacy.android.navigation.contract.settings.FeatureSettingEntryPoint
import mega.privacy.android.navigation.contract.settings.FeatureSettings
import mega.privacy.android.navigation.contract.settings.MoreSettingEntryPoint
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock

/**
 * Test settings module
 *
 * Provides test dependencies for Settings tests
 */
@TestInstallIn(
    replaces = [SettingsModule::class, SettingsUseCases::class, TempStartScreenUseCaseStaticModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestSettingsModule {
    val canDeleteAccount = mock<CanDeleteAccount> { on { invoke(any()) }.thenReturn(true) }
    val monitorStartScreenPreference =
        mock<MonitorStartScreenPreference> { on { invoke() }.thenReturn(emptyFlow()) }
    val isMultiFactorAuthAvailable =
        mock<IsMultiFactorAuthAvailable> { on { invoke() }.thenReturn(true) }
    val monitorMediaDiscoveryView =
        mock<MonitorMediaDiscoveryView> { on { invoke() }.thenReturn(emptyFlow()) }
    val getChatImageQuality = mock<GetChatImageQuality> { on { invoke() }.thenReturn(emptyFlow()) }
    val setChatImageQuality = mock<SetChatImageQuality>()
    val monitorCallSoundEnabledUseCase =
        mock<MonitorCallSoundEnabledUseCase> { on { invoke() }.thenReturn(emptyFlow()) }
    val setCallsSoundEnabledStateUseCase = mock<SetCallsSoundEnabledStateUseCase>()

    @Provides
    fun provideCanDeleteAccount(): CanDeleteAccount = canDeleteAccount

    @Provides
    fun provideIsMultiFactorAuthAvailable(): IsMultiFactorAuthAvailable =
        isMultiFactorAuthAvailable

    @Provides
    fun provideGetStartScreen(): MonitorStartScreenPreference = monitorStartScreenPreference

    @Provides
    fun provideMonitorMediaDiscoveryView(): MonitorMediaDiscoveryView = monitorMediaDiscoveryView

    @Provides
    fun provideSetMediaDiscoveryView(): SetMediaDiscoveryView = mock()

    @Provides
    fun provideRequestAccountDeletion(): RequestAccountDeletion = mock()


    @Provides
    fun provideIsChatLoggedIn(): IsChatLoggedIn = mock { on { invoke() }.thenReturn(flowOf(true)) }

    @Provides
    fun provideGetChatImageQuality(): GetChatImageQuality = getChatImageQuality

    @Provides
    fun provideSetChatImageQuality(): SetChatImageQuality = setChatImageQuality

    @Provides
    fun provideGetCallsSoundNotifications(): MonitorCallSoundEnabledUseCase = monitorCallSoundEnabledUseCase

    @Provides
    fun provideSetCallsSoundNotifications(): SetCallsSoundEnabledStateUseCase = setCallsSoundEnabledStateUseCase

    @Provides
    fun providePutStringPreference(): PutPreference<String> =
        mock()

    @Provides
    fun providePutStringSetPreference(): PutPreference<MutableSet<String>> =
        mock()

    @Provides
    fun providePutIntPreference(): PutPreference<Int> =
        mock()

    @Provides
    fun providePutLongPreference(): PutPreference<Long> =
        mock()

    @Provides
    fun providePutFloatPreference(): PutPreference<Float> =
        mock()

    @Provides
    fun providePutBooleanPreference(): PutPreference<Boolean> =
        mock()

    @Provides
    fun provideGetStringPreference(): GetPreference<String?> =
        mock { on { invoke(anyOrNull(), anyOrNull()) }.thenReturn(emptyFlow()) }

    @Provides
    fun provideGetStringSetPreference(): GetPreference<MutableSet<String>?> =
        mock { on { invoke(anyOrNull(), anyOrNull()) }.thenReturn(emptyFlow()) }

    @Provides
    fun provideGetIntPreference(): GetPreference<Int> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer { flowOf(it.arguments[1]) } }

    @Provides
    fun provideGetLongPreference(): GetPreference<Long> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer { flowOf(it.arguments[1]) } }

    @Provides
    fun provideGetFloatPreference(): GetPreference<Float> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer { flowOf(it.arguments[1]) } }

    @Provides
    fun provideGetBooleanPreference(): GetPreference<Boolean> =
        mock { on { invoke(anyOrNull(), any()) }.thenAnswer { flowOf(it.arguments[1]) } }

    @Provides
    fun provideGetSupportEmail(): GetSupportEmailUseCase = mock()

    @Provides
    @ElementsIntoSet
    fun providePreferenceResourceSet(): Set<@JvmSuppressWildcards PreferenceResource> = setOf()

    @Provides
    fun provideEnableFileVersionsOption(): EnableFileVersionsOption = mock()

    @Provides
    @ElementsIntoSet
    fun provideFeatureSettingsSet(): Set<@JvmSuppressWildcards FeatureSettings> = setOf()

    @Provides
    @ElementsIntoSet
    fun provideAppFeatureSettingEntryPoints(): Set<@JvmSuppressWildcards FeatureSettingEntryPoint> =
        setOf()

    @Provides
    @ElementsIntoSet
    fun provideAddMoreSettingsEntryPoints(): Set<@JvmSuppressWildcards MoreSettingEntryPoint> =
        setOf()
}
