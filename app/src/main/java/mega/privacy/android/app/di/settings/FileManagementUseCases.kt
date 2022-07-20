package mega.privacy.android.app.di.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultGetFolderVersionInfo
import mega.privacy.android.domain.usecase.GetFolderVersionInfo

@Module
@InstallIn(ViewModelComponent::class)
abstract class FileManagementUseCases {
    @Binds
    abstract fun bindGetFolderVersionInfo(implementation: DefaultGetFolderVersionInfo): GetFolderVersionInfo

}