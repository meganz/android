package mega.privacy.android.app.di

import mega.privacy.android.domain.di.BillingModule as DomainBillingModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.usecase.billing.LaunchPurchaseFlow
import mega.privacy.android.data.repository.AndroidBillingRepository

/**
 * Account module
 *
 */
@Module(includes = [DomainBillingModule::class])
@InstallIn(ViewModelComponent::class)
internal abstract class BillingModule {
    companion object {
        @Provides
        internal fun provideLaunchPurchaseFlow(repository: AndroidBillingRepository) =
            LaunchPurchaseFlow(repository::launchPurchaseFlow)
    }
}