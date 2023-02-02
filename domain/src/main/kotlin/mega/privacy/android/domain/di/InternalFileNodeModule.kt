package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.filenode.GetFileHistoryNumVersions

@Module
@DisableInstallInCheck
object InternalFileNodeModule {
    @Provides
    fun providesGetFileHistoryNumVersions(nodeRepository: NodeRepository): GetFileHistoryNumVersions =
        GetFileHistoryNumVersions(nodeRepository::getNodeHistoryNumVersions)
}