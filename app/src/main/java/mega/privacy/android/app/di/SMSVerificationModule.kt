package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.SMSVerificationRepository
import mega.privacy.android.domain.usecase.IsSMSVerificationShown
import mega.privacy.android.domain.usecase.SetSMSVerificationShown

/**
 * SMS verification Module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SMSVerificationModule {

    /**
     * Provides the Use Case [SetSMSVerificationShown]
     */
    @Provides
    fun provideSetSMSVerificationShown(repository: SMSVerificationRepository): SetSMSVerificationShown =
        SetSMSVerificationShown(repository::setSMSVerificationShown)

    /**
     * Provides the Use Case [IsSMSVerificationShown]
     */
    @Provides
    fun provideIsSMSVerificationShown(repository: SMSVerificationRepository): IsSMSVerificationShown =
        IsSMSVerificationShown(repository::isSMSVerificationShown)
}
