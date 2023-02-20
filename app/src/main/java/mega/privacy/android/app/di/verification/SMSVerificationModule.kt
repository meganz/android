package mega.privacy.android.app.di.verification

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.smsVerification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.app.smsVerification.model.mapper.SmsVerificationTextErrorMapperImpl
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.VerificationRepository
import mega.privacy.android.domain.usecase.AreAccountAchievementsEnabled
import mega.privacy.android.domain.usecase.GetCountryCallingCodes
import mega.privacy.android.domain.usecase.GetCurrentCountryCode
import mega.privacy.android.domain.usecase.IsSMSVerificationShown
import mega.privacy.android.domain.usecase.Logout
import mega.privacy.android.domain.usecase.SetSMSVerificationShown
import mega.privacy.android.domain.usecase.verification.DefaultMonitorVerifiedPhoneNumber
import mega.privacy.android.domain.usecase.verification.MonitorVerifiedPhoneNumber
import mega.privacy.android.domain.usecase.verification.ResetSMSVerifiedPhoneNumber
import mega.privacy.android.domain.usecase.verification.SendSMSVerificationCode
import mega.privacy.android.domain.usecase.verification.VerifyPhoneNumber

/**
 * SMS verification Module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SMSVerificationModule {

    @Binds
    abstract fun bindMonitorVerifiedPhoneNumber(implementation: DefaultMonitorVerifiedPhoneNumber): MonitorVerifiedPhoneNumber

    @Binds
    abstract fun bindSmsVerificationTextErrorMapper(implementation: SmsVerificationTextErrorMapperImpl): SmsVerificationTextErrorMapper

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

        /**
         * Provides the Use Case [SendSMSVerificationCode]
         */
        @Provides
        fun provideSendSMSVerificationCode(repository: VerificationRepository): SendSMSVerificationCode =
            SendSMSVerificationCode(repository::sendSMSVerificationCode)

        /**
         * Provides the Use Case [ResetSMSVerifiedPhoneNumber]
         */
        @Provides
        fun provideResetSMSVerifiedPhoneNumber(repository: VerificationRepository): ResetSMSVerifiedPhoneNumber =
            ResetSMSVerifiedPhoneNumber(repository::resetSMSVerifiedPhoneNumber)

        /**
         * Provides the Use Case [AreAccountAchievementsEnabled]
         */
        @Provides
        fun provideAreAccountAchievementsEnabled(repository: AccountRepository): AreAccountAchievementsEnabled =
            AreAccountAchievementsEnabled(repository::areAccountAchievementsEnabled)

        /**
         * Provides the Use Case [GetCurrentCountryCode]
         */
        @Provides
        fun provideGetCurrentCountryCode(repository: VerificationRepository): GetCurrentCountryCode =
            GetCurrentCountryCode(repository::getCurrentCountryCode)

        /**
         * Provides the Use Case [Logout]
         */
        @Provides
        fun provideLogout(repository: LoginRepository): Logout = Logout(repository::logout)

        @Provides
        fun provideVerifyPhoneNumber(repository: VerificationRepository): VerifyPhoneNumber =
            VerifyPhoneNumber(repository::verifyPhoneNumber)
    }
}
