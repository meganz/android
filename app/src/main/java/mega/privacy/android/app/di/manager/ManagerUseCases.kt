package mega.privacy.android.app.di.manager

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.di.GetNodeModule
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.DefaultGetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultGetInboxChildrenNodes
import mega.privacy.android.app.domain.usecase.DefaultGetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultGetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultGetParentNodeHandle
import mega.privacy.android.app.domain.usecase.DefaultGetPublicLinks
import mega.privacy.android.app.domain.usecase.DefaultGetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.DefaultGetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultMonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetInboxChildrenNodes
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetPublicLinks
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildren
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.HasInboxChildren
import mega.privacy.android.domain.usecase.MonitorUserAlertUpdates

/**
 * Manager module
 *
 * Provides dependencies used by multiple screens in the manager package
 */

@Module(includes = [GetNodeModule::class])
@InstallIn(ViewModelComponent::class)
abstract class ManagerUseCases {

    @Binds
    abstract fun bindMonitorGlobalUpdates(useCase: DefaultMonitorGlobalUpdates): MonitorGlobalUpdates

    @Binds
    abstract fun bindRubbishBinChildrenNode(useCase: DefaultGetRubbishBinChildrenNode): GetRubbishBinChildrenNode

    @Binds
    abstract fun bindGetParentNode(useCase: DefaultGetParentNodeHandle): GetParentNodeHandle

    @Binds
    abstract fun bindGetBrowserChildrenNode(useCase: DefaultGetBrowserChildrenNode): GetBrowserChildrenNode

    @Binds
    abstract fun bindGetInboxChildrenNodes(useCase: DefaultGetInboxChildrenNodes): GetInboxChildrenNodes

    @Binds
    abstract fun bindGetIncomingSharesChildrenNode(useCase: DefaultGetIncomingSharesChildrenNode): GetIncomingSharesChildrenNode

    @Binds
    abstract fun bindGetOutgoingSharesChildrenNode(useCase: DefaultGetOutgoingSharesChildrenNode): GetOutgoingSharesChildrenNode

    @Binds
    abstract fun bindGetPublicLinks(useCase: DefaultGetPublicLinks): GetPublicLinks

    @Binds
    abstract fun bindRubbishBinChildren(useCase: DefaultGetRubbishBinChildren): GetRubbishBinChildren

    companion object {
        @Provides
        fun provideMonitorNodeUpdates(nodeRepository: NodeRepository): MonitorNodeUpdates =
            MonitorNodeUpdates(nodeRepository::monitorNodeUpdates)

        @Provides
        fun provideGetRootFolder(megaNodeRepository: MegaNodeRepository): GetRootFolder =
            GetRootFolder(megaNodeRepository::getRootNode)

        @Provides
        fun provideGetRubbishBinFolder(megaNodeRepository: MegaNodeRepository): GetRubbishBinFolder =
            GetRubbishBinFolder(megaNodeRepository::getRubbishBinNode)

        @Provides
        fun provideGetNumUnreadUserAlerts(accountRepository: AccountRepository): GetNumUnreadUserAlerts =
            GetNumUnreadUserAlerts(accountRepository::getNumUnreadUserAlerts)

        @Provides
        fun provideHasInboxChildren(megaNodeRepository: MegaNodeRepository): HasInboxChildren =
            HasInboxChildren(megaNodeRepository::hasInboxChildren)

        @Provides
        fun provideMonitorUserAlerts(notificationsRepository: NotificationsRepository): MonitorUserAlertUpdates =
            MonitorUserAlertUpdates(notificationsRepository::monitorUserAlerts)

        @Provides
        fun provideAuthorizeNode(megaNodeRepository: MegaNodeRepository): AuthorizeNode =
            AuthorizeNode(megaNodeRepository::authorizeNode)

        @Provides
        fun provideGetInboxNode(megaNodeRepository: MegaNodeRepository): GetInboxNode =
            GetInboxNode(megaNodeRepository::getInboxNode)
    }
}