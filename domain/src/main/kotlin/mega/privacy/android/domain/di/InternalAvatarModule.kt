package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.account.UpdateMyAvatarWithNewEmail

@Module
@DisableInstallInCheck
internal abstract class InternalAvatarModule {
    companion object {
        @Provides
        fun provideGetMyAvatarFile(repository: AvatarRepository): GetMyAvatarFile =
            GetMyAvatarFile(repository::getMyAvatarFile)

        @Provides
        fun provideGetMonitorMyAvatarFile(repository: AvatarRepository): MonitorMyAvatarFile =
            MonitorMyAvatarFile(repository::monitorMyAvatarFile)

        @Provides
        fun provideUpdateMyAvatarWithNewEmail(repository: AvatarRepository): UpdateMyAvatarWithNewEmail =
            UpdateMyAvatarWithNewEmail(repository::updateMyAvatarWithNewEmail)
    }
}