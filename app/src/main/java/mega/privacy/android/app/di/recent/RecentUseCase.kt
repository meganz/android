package mega.privacy.android.app.di.recent

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.repository.RecentActionsRepository
import mega.privacy.android.app.domain.usecase.GetRecentActions

/**
 * Recent use case module
 *
 * Provides dependencies used by multiple screens in the recent package
 */

@Module
@InstallIn(ViewModelComponent::class)
abstract class RecentUseCase {

    companion object {
        /**
         * Provide [GetRecentActions]
         */
        @Provides
        fun provideRecentActions(recentActionsRepository: RecentActionsRepository): GetRecentActions =
            GetRecentActions(recentActionsRepository::getRecentActions)
    }

}