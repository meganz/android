package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.FilesRepository
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
         * Provide the GetChildrenNode implementation
         */
        @Provides
        fun provideGetChildrenNode(filesRepository: FilesRepository): GetChildrenNode =
            GetChildrenNode(filesRepository::getChildrenNode)

        /**
         * Provide the GetNodeByHandle implementation
         */
        @Provides
        fun provideGetNodeByHandle(filesRepository: FilesRepository): GetNodeByHandle =
            GetNodeByHandle(filesRepository::getNodeByHandle)
    }
}
