package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.twofactorauthentication.TwoFactorAuthenticationUseCases
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import org.mockito.kotlin.mock

@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [TwoFactorAuthenticationUseCases::class]
)
@Module
object TestTwoFactorAuthenticationUseCases {
    val enableMultiFactorAuth = mock<EnableMultiFactorAuth>()

    /**
     * Provides EnableMultiFactorAuth Use Case
     */
    @Provides
    fun providesEnableMultiFactorAuth(): EnableMultiFactorAuth =
        enableMultiFactorAuth
}