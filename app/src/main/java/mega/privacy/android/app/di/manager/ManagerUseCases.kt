package mega.privacy.android.app.di.manager

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultGetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultMonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.HasInboxChildren
import mega.privacy.android.domain.usecase.MonitorUserAlertUpdates

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
    abstract fun bindRubbishBinChildrenNode(useCase: DefaultGetRubbishBinChildrenNode): GetRubbishBinChildrenNode

    @Binds
    abstract fun bindBrowserChildrenNode(useCase: DefaultGetBrowserChildrenNode): GetBrowserChildrenNode

    companion object {
        @Provides
        fun provideMonitorNodeUpdates(filesRepository: FilesRepository): MonitorNodeUpdates =
            MonitorNodeUpdates(filesRepository::monitorNodeUpdates)

        @Provides
        fun provideGetRootFolder(filesRepository: FilesRepository): GetRootFolder =
            GetRootFolder(filesRepository::getRootNode)

        @Provides
        fun provideGetRubbishBinFolder(filesRepository: FilesRepository): GetRubbishBinFolder =
            GetRubbishBinFolder(filesRepository::getRubbishBinNode)

        @Provides
        fun provideGetChildrenNode(filesRepository: FilesRepository): GetChildrenNode =
            GetChildrenNode(filesRepository::getChildrenNode)

        @Provides
        fun provideGetNodeByHandle(filesRepository: FilesRepository): GetNodeByHandle =
            GetNodeByHandle(filesRepository::getNodeByHandle)

        @Provides
        fun provideGetNumUnreadUserAlerts(accountRepository: AccountRepository): GetNumUnreadUserAlerts =
            GetNumUnreadUserAlerts(accountRepository::getNumUnreadUserAlerts)

        @Provides
        fun provideHasInboxChildren(filesRepository: FilesRepository): HasInboxChildren =
            HasInboxChildren(filesRepository::hasInboxChildren)

        @Provides
        fun provideMonitorUserAlerts(notificationsRepository: NotificationsRepository): MonitorUserAlertUpdates =
            MonitorUserAlertUpdates(notificationsRepository::monitorUserAlerts)
    }
}