package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.account.ChangePasswordUseCases
import mega.privacy.android.domain.usecase.ChangePassword
import mega.privacy.android.domain.usecase.GetPasswordStrength
import mega.privacy.android.domain.usecase.IsCurrentPassword
import mega.privacy.android.domain.usecase.ResetPassword
import org.mockito.kotlin.mock

@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [ChangePasswordUseCases::class]
)
@Module
object TestChangePasswordUseCases {
    val changePassword = mock<ChangePassword>()
    val getPasswordStrength = mock<GetPasswordStrength>()
    val isCurrentPassword = mock<IsCurrentPassword>()
    val resetPassword = mock<ResetPassword>()

    @Provides
    fun provideChangePassword(): ChangePassword = changePassword

    @Provides
    fun provideGetPasswordStrength(): GetPasswordStrength = getPasswordStrength

    @Provides
    fun provideIsCurrentPassword(): IsCurrentPassword = isCurrentPassword

    @Provides
    fun provideResetPassword(): ResetPassword = resetPassword
}