package test.mega.privacy.android.app.di

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
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.CanDeleteAccount
import mega.privacy.android.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSetting
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetCallsSoundNotifications
import mega.privacy.android.domain.usecase.GetChatImageQuality
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.GetSupportEmail
import mega.privacy.android.domain.usecase.IsCameraSyncEnabled
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.PutPreference
import mega.privacy.android.domain.usecase.RefreshPasscodeLockPreference
import mega.privacy.android.domain.usecase.RequestAccountDeletion
import mega.privacy.android.domain.usecase.SetCallsSoundNotifications
import mega.privacy.android.domain.usecase.SetChatImageQuality
import mega.privacy.android.domain.usecase.SetChatLogsEnabled
import mega.privacy.android.domain.usecase.SetSdkLogsEnabled
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.TEST_USER_ACCOUNT

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
    val fetchAutoAcceptQRLinks =
        mock<FetchAutoAcceptQRLinks> { onBlocking { invoke() }.thenReturn(false) }
    val fetchMultiFactorAuthSetting =
        mock<FetchMultiFactorAuthSetting> { on { invoke() }.thenReturn(emptyFlow()) }
    val getAccountDetails =
        mock<GetAccountDetails> { onBlocking { invoke(any()) }.thenReturn(TEST_USER_ACCOUNT) }
    val getChatImageQuality = mock<GetChatImageQuality> { on { invoke() }.thenReturn(emptyFlow()) }
    val setChatImageQuality = mock<SetChatImageQuality>()
    val getOfflineThumbnailFileWrapper = mock<GetOfflineThumbnailFileWrapper>()

    val getCallsSoundNotifications =
        mock<GetCallsSoundNotifications> { on { invoke() }.thenReturn(emptyFlow()) }
    val setCallsSoundNotifications = mock<SetCallsSoundNotifications>()

    @Provides
    fun provideGetAccountDetails(): GetAccountDetails = getAccountDetails

    @Provides
    fun provideCanDeleteAccount(): CanDeleteAccount = canDeleteAccount

    @Provides
    fun provideRefreshPasscodeLockPreference(): RefreshPasscodeLockPreference =
        mock()

    @Provides
    fun provideIsLoggingEnabled(): AreSdkLogsEnabled =
        mock { on { invoke() }.thenReturn(flowOf(true)) }

    @Provides
    fun provideIsChatLoggingEnabled(): AreChatLogsEnabled =
        mock { on { invoke() }.thenReturn(flowOf(true)) }

    @Provides
    fun provideIsCameraSyncEnabled(): IsCameraSyncEnabled = mock()

    @Provides
    fun provideIsMultiFactorAuthAvailable(): IsMultiFactorAuthAvailable =
        isMultiFactorAuthAvailable


    @Provides
    fun provideFetchContactLinksOption(): FetchAutoAcceptQRLinks =
        fetchAutoAcceptQRLinks


    @Provides
    fun provideFetchMultiFactorAuthSetting(): FetchMultiFactorAuthSetting =
        fetchMultiFactorAuthSetting


    @Provides
    fun provideGetStartScreen(): MonitorStartScreenPreference = monitorStartScreenPreference

    @Provides
    fun provideToggleAutoAcceptQRLinks(): ToggleAutoAcceptQRLinks =
        mock()

    @Provides
    fun provideRequestAccountDeletion(): RequestAccountDeletion = mock()


    @Provides
    fun provideIsChatLoggedIn(): IsChatLoggedIn = mock { on { invoke() }.thenReturn(flowOf(true)) }

    @Provides
    fun provideSetLoggingEnabled(): SetSdkLogsEnabled = mock()

    @Provides
    fun provideSetChatLoggingEnabled(): SetChatLogsEnabled = mock()

    @Provides
    fun provide(): MonitorAutoAcceptQRLinks = mock {
        on { invoke() }.thenReturn(
            flowOf(true)
        )
    }

    @Provides
    fun provideGetChatImageQuality(): GetChatImageQuality = getChatImageQuality

    @Provides
    fun provideSetChatImageQuality(): SetChatImageQuality = setChatImageQuality

    @Provides
    fun provideGetCallsSoundNotifications(): GetCallsSoundNotifications = getCallsSoundNotifications

    @Provides
    fun provideSetCallsSoundNotifications(): SetCallsSoundNotifications = setCallsSoundNotifications

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
    fun provideGetSupportEmail(): GetSupportEmail = mock()

    @Provides
    @ElementsIntoSet
    fun providePreferenceResourceSet(): Set<@JvmSuppressWildcards PreferenceResource> = setOf()
}