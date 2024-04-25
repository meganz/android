package mega.privacy.android.app.di.verification

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.verification.model.mapper.SMSVerificationTextMapper
import mega.privacy.android.app.presentation.verification.model.mapper.SMSVerificationTextMapperImpl
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapperImpl
import mega.privacy.android.domain.repository.VerificationRepository
import mega.privacy.android.domain.usecase.IsSMSVerificationShown
import mega.privacy.android.domain.usecase.verification.DefaultMonitorVerificationStatus
import mega.privacy.android.domain.usecase.verification.DefaultMonitorVerifiedPhoneNumber
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import mega.privacy.android.domain.usecase.verification.MonitorVerifiedPhoneNumber
import mega.privacy.android.domain.usecase.verification.VerifyPhoneNumber

/**
 * SMS verification Module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SMSVerificationModule {

    /**
     * bind [DefaultMonitorVerifiedPhoneNumber]
     */
    @Binds
    abstract fun bindMonitorVerifiedPhoneNumber(implementation: DefaultMonitorVerifiedPhoneNumber): MonitorVerifiedPhoneNumber

    /**
     * bind [SmsVerificationTextErrorMapperImpl]
     */
    @Binds
    abstract fun bindSmsVerificationTextErrorMapper(implementation: SmsVerificationTextErrorMapperImpl): SmsVerificationTextErrorMapper

    /**
     * bind [DefaultMonitorVerificationStatus]
     */
    @Binds
    abstract fun bindMonitorVerificationStatus(implementation: DefaultMonitorVerificationStatus): MonitorVerificationStatus

    /**
     * bind [SMSVerificationTextMapper]
     */
    @Binds
    abstract fun bindSMSVerificationTextMapper(implementation: SMSVerificationTextMapperImpl): SMSVerificationTextMapper

    companion object {
        /**
         * Provides the Use Case [IsSMSVerificationShown]
         */
        @Provides
        fun provideIsSMSVerificationShown(repository: VerificationRepository): IsSMSVerificationShown =
            IsSMSVerificationShown(repository::isSMSVerificationShown)

        /**
         * Provides the use case [VerifyPhoneNumber]
         */
        @Provides
        fun provideVerifyPhoneNumber(repository: VerificationRepository): VerifyPhoneNumber =
            VerifyPhoneNumber(repository::verifyPhoneNumber)
    }
}
