package mega.privacy.android.app.di.upgradeaccount

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.GetCurrentSubscriptionPlan
import mega.privacy.android.domain.usecase.GetSubscriptionPlans

/**
 * Upgrade account use cases
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UpgradeAccountUseCases {
    companion object {
        @Provides
        fun provideGetSubscriptionPlans(repository: AccountRepository): GetSubscriptionPlans =
            GetSubscriptionPlans(repository::getSubscriptionPlans)

        @Provides
        fun provideGetCurrentSubscriptionPlan(repository: AccountRepository): GetCurrentSubscriptionPlan =
            GetCurrentSubscriptionPlan {
                repository::getUserAccount.invoke().accountTypeIdentifier
            }
    }
}