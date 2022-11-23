package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.CopyNode
import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle

/**
 * Get node module
 *
 * Provides use cases for manager activity and camera upload service
 */
@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class GetNodeModule {

    companion object {

        /**
         * Provides the [CopyNode] implementation
         *
         * @param filesRepository [FilesRepository]
         * @return [CopyNode]
         */
        @Provides
        fun provideCopyNode(filesRepository: FilesRepository): CopyNode =
            CopyNode(filesRepository::copyNode)

        /**
         * Provides the [GetChildrenNode] implementation
         *
         * @param filesRepository [FilesRepository]
         * @return [GetChildrenNode]
         */
        @Provides
        fun provideGetChildrenNode(filesRepository: FilesRepository): GetChildrenNode =
            GetChildrenNode(filesRepository::getChildrenNode)

        /**
         * Provides the [GetNodeByHandle] implementation
         *
         * @param filesRepository [FilesRepository]
         * @return [GetNodeByHandle]
         */
        @Provides
        fun provideGetNodeByHandle(filesRepository: FilesRepository): GetNodeByHandle =
            GetNodeByHandle(filesRepository::getNodeByHandle)
    }
}
