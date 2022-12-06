package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.VerificationRepository
import mega.privacy.android.domain.usecase.GetCountryCallingCodes
import mega.privacy.android.domain.usecase.IsSMSVerificationShown
import mega.privacy.android.domain.usecase.SetSMSVerificationShown

/**
 * SMS verification Module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SMSVerificationModule {

    companion object {
        /**
         * Provides the Use Case [SetSMSVerificationShown]
         */
        @Provides
        fun provideSetSMSVerificationShown(repository: VerificationRepository): SetSMSVerificationShown =
            SetSMSVerificationShown(repository::setSMSVerificationShown)

        /**
         * Provides the Use Case [IsSMSVerificationShown]
         */
        @Provides
        fun provideIsSMSVerificationShown(repository: VerificationRepository): IsSMSVerificationShown =
            IsSMSVerificationShown(repository::isSMSVerificationShown)

        /**
         * Provides the Use Case [GetCountryCallingCodes]
         */
        @Provides
        fun provideGetCountryCallingCodes(repository: VerificationRepository): GetCountryCallingCodes =
            GetCountryCallingCodes(repository::getCountryCallingCodes)
    }
}
