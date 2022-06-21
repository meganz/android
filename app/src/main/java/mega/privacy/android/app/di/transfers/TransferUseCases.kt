package mega.privacy.android.app.di.transfers

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.repository.GlobalStatesRepository
import mega.privacy.android.app.domain.usecase.AreTransfersPaused

/**
 * Use cases to check on transfer status
 */
@Module
@InstallIn(ViewModelComponent::class)
class TransferUseCases {

    @Provides
    fun provideAreTransfersPaused(globalStatesRepository: GlobalStatesRepository):
            AreTransfersPaused = AreTransfersPaused(globalStatesRepository::areTransfersPaused)
}
