package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.shares.DefaultGetOutShares
import mega.privacy.android.app.domain.usecase.shares.GetOutShares

/**
 * binds use-cases related to shared nodes
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class SharesModule {

    /**
     * binds default implementation
     */
    @Binds
    abstract fun bindGetOutShares(outSharesImplementation: DefaultGetOutShares): GetOutShares
}
