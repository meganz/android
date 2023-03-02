package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.settings.twofactorauthentication.TwoFactorAuthenticationUseCases
import mega.privacy.android.domain.usecase.EnableMultiFactorAuth
import mega.privacy.android.domain.usecase.GetMultiFactorAuthCode
import mega.privacy.android.domain.usecase.IsMasterKeyExported
import org.mockito.kotlin.mock

@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [TwoFactorAuthenticationUseCases::class]
)
@Module
object TestTwoFactorAuthenticationUseCases {

    val isMasterKeyExported = mock<IsMasterKeyExported>()
    val enableMultiFactorAuth = mock<EnableMultiFactorAuth>()
    val getMultiFactorAuthCode = mock<GetMultiFactorAuthCode>()

    /**
     * Provides IsMasterKeyExported Use Case
     */
    @Provides
    fun providesIsMasterKeyExported(): IsMasterKeyExported = isMasterKeyExported

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