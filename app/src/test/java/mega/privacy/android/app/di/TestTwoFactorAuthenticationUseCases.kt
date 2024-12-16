package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.twofactorauthentication.TwoFactorAuthenticationUseCases
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import mega.privacy.android.domain.usecase.GetMultiFactorAuthCode
import org.mockito.kotlin.mock

@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [TwoFactorAuthenticationUseCases::class]
)
@Module
object TestTwoFactorAuthenticationUseCases {
    val enableMultiFactorAuth = mock<EnableMultiFactorAuth>()
    val getMultiFactorAuthCode = mock<GetMultiFactorAuthCode>()

    /**
     * Provides EnableMultiFactorAuth Use Case
     */
    @Provides
    fun providesEnableMultiFactorAuth(): EnableMultiFactorAuth =
        enableMultiFactorAuth

    /**
     * Provides GetMultiFactorAuthCode Use Case
     */
    @Provides
    fun providesGetMultiFactorAuthCode(): GetMultiFactorAuthCode =
        getMultiFactorAuthCode
}