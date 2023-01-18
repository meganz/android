package mega.privacy.android.app.di.upgradeaccount

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.CalculateCurrencyAmount
import mega.privacy.android.domain.usecase.DefaultCalculateCurrencyAmount
import mega.privacy.android.domain.usecase.DefaultGetAppSubscriptionOptions
import mega.privacy.android.domain.usecase.DefaultGetSubscriptions
import mega.privacy.android.domain.usecase.GetAppSubscriptionOptions
import mega.privacy.android.domain.usecase.GetCurrentPayment
import mega.privacy.android.domain.usecase.GetCurrentSubscriptionPlan
import mega.privacy.android.domain.usecase.GetLocalPricing
import mega.privacy.android.domain.usecase.GetSubscriptionOptions
import mega.privacy.android.domain.usecase.GetSubscriptions

/**
 * Upgrade account use cases
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UpgradeAccountUseCases {

    @Binds
    abstract fun provideGetSubscriptions(defaultGetSubscriptions: DefaultGetSubscriptions): GetSubscriptions

    companion object {
        @Provides
        fun provideGetSubscriptionPlans(repository: AccountRepository): GetSubscriptionOptions =
            GetSubscriptionOptions(repository::getSubscriptionOptions)

        @Provides
        fun provideGetCurrentSubscriptionPlan(repository: AccountRepository): GetCurrentSubscriptionPlan =
            GetCurrentSubscriptionPlan {
                repository::getUserAccount.invoke().accountTypeIdentifier
            }

        @Provides
        fun provideGetLocalPricing(repository: BillingRepository): GetLocalPricing =
            GetLocalPricing(repository::getLocalPricing)

        @Provides
        fun provideCalculateCurrencyAmount(): CalculateCurrencyAmount =
            DefaultCalculateCurrencyAmount()

        @Provides
        fun provideGetAppSubscriptionOptions(repository: AccountRepository): GetAppSubscriptionOptions =
            DefaultGetAppSubscriptionOptions(repository)

        @Provides
        fun provideGetCurrentPaymentMethod(repository: BillingRepository): GetCurrentPayment =
            GetCurrentPayment(repository::getCurrentPaymentMethod)
    }
}