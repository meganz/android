package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.account.DefaultUpdateCurrentUserName
import mega.privacy.android.domain.usecase.account.UpdateCurrentUserName

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateCurrentUserNameModule {

    @Binds
    abstract fun bindUpdateCurrentUserName(implementation: DefaultUpdateCurrentUserName): UpdateCurrentUserName
}
