package mega.privacy.android.app.di.settings.twofactorauthentication

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.di.TwoFactorAuthenticationModule

/**
 * Dagger module for the Use Cases in TwoFactorAuthentication Activity
 */
@Module(includes = [TwoFactorAuthenticationModule::class])
@InstallIn(ViewModelComponent::class)
internal abstract class TwoFactorAuthenticationUseCases