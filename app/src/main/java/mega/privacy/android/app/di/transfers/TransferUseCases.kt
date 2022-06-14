package mega.privacy.android.app.di.transfers

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.AreTransfersPaused
import mega.privacy.android.app.domain.usecase.DefaultAreTransfersPaused

/**
 * Use cases to check on transfer status
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class TransferUseCases {

    @Binds
    abstract fun bindAreTransfersPaused(useCase: DefaultAreTransfersPaused): AreTransfersPaused
}
