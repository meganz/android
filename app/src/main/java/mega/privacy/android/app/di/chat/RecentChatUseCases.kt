package mega.privacy.android.app.di.chat

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetLastContactPermissionDismissedTime
import mega.privacy.android.domain.usecase.SetLastContactPermissionDismissedTime

/**
 * Recent chat use case module
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class RecentChatUseCases {
    companion object {
        /**
         * provide set last contact permission dismissed time
         */
        @Provides
        fun provideSetLastContactPermissionDismissedTime(repository: SettingsRepository) =
            SetLastContactPermissionDismissedTime(repository::setLastContactPermissionDismissedTime)

        /**
         * provide get last contact permission dismissed time
         */
        @Provides
        fun provideGetLastContactPermissionDismissedTime(repository: SettingsRepository) =
            GetLastContactPermissionDismissedTime(repository::getLastContactPermissionDismissedTime)
    }
}