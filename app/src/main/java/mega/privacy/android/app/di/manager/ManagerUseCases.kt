package mega.privacy.android.app.di.manager

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

/**
 * Manager module
 *
 * Provides dependencies used by multiple screens in the manager package
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class ManagerUseCases {

    @Binds
    abstract fun bindMonitorGlobalUpdates(useCase: DefaultMonitorGlobalUpdates): MonitorGlobalUpdates

    @Binds
    abstract fun bindMonitorNodeUpdates(useCase: DefaultMonitorNodeUpdates): MonitorNodeUpdates

    @Binds
    abstract fun bindRubbishBinChildrenNode(useCase: DefaultGetRubbishBinChildrenNode): GetRubbishBinChildrenNode

    @Binds
    abstract fun bindBrowserChildrenNode(useCase: DefaultGetBrowserChildrenNode): GetBrowserChildrenNode

    @Binds
    abstract fun bindGetRootFolder(useCase: DefaultGetRootFolder): GetRootFolder

    @Binds
    abstract fun bindGetRubbishBinFolder(useCase: DefaultGetRubbishBinFolder): GetRubbishBinFolder

    @Binds
    abstract fun bindGetChildrenNode(useCase: DefaultGetChildrenNode): GetChildrenNode

    @Binds
    abstract fun bindGetNodeByHandle(useCase: DefaultGetNodeByHandle): GetNodeByHandle

    @Binds
    abstract fun bindGetInboxNode(useCase: DefaultGetInboxNode): GetInboxNode

    @Binds
    abstract fun bindHasChildren(useCase: DefaultHasChildren): HasChildren
}