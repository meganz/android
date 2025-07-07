package mega.privacy.android.app.di.appstate

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import mega.privacy.android.app.appstate.initialisation.initialisers.AppStartInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.appstate.initialisation.initialisers.PreLoginInitialiser
import mega.privacy.android.domain.usecase.environment.GetHistoricalProcessExitReasonsUseCase
import mega.privacy.android.domain.usecase.login.InitialiseMegaChatUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase

@Module
@InstallIn(SingletonComponent::class)
class InitialisersModule {

    @Provides
    @IntoSet
    fun provideHistoricalProcessExitReasonsUseCaseInitialiser(getHistoricalProcessExitReasonsUseCase: GetHistoricalProcessExitReasonsUseCase): AppStartInitialiser =
        AppStartInitialiser { getHistoricalProcessExitReasonsUseCase() }

    @Provides
    @IntoSet
    fun provideResetChatSettingsUseCaseInitialiser(resetChatSettingsUseCase: ResetChatSettingsUseCase): AppStartInitialiser =
        AppStartInitialiser { resetChatSettingsUseCase() }

    @Provides
    @ElementsIntoSet
    fun providePreLoginInitialisers(): Set<PreLoginInitialiser> = emptySet()

    @Provides
    @IntoSet
    fun provideChatPostLoginInitialisers(useCase: InitialiseMegaChatUseCase): PostLoginInitialiser =
        PostLoginInitialiser { session ->
            useCase(session)
        }

}