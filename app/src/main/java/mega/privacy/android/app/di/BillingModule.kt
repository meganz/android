package mega.privacy.android.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.di.BillingModule as DomainBillingModule

@Module(includes = [DomainBillingModule::class])
@InstallIn(ViewModelComponent::class)
internal abstract class BillingModule