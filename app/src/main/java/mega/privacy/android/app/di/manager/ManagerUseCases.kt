package mega.privacy.android.app.di.manager

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.di.GetNodeModule
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.DefaultGetBackupsChildrenNodes
import mega.privacy.android.app.domain.usecase.DefaultGetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultGetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.DefaultGetParentNodeHandle
import mega.privacy.android.app.domain.usecase.DefaultGetPublicLinks
import mega.privacy.android.app.domain.usecase.GetBackupsChildrenNodes
import mega.privacy.android.app.domain.usecase.GetBackupsNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetPublicLinks
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.MonitorUserAlertUpdates

/**
 * Manager module
 *
 * Provides dependencies used by multiple screens in the manager package
 */

@Module(includes = [GetNodeModule::class])
@InstallIn(ViewModelComponent::class, ServiceComponent::class)
abstract class ManagerUseCases {

    @Binds
    abstract fun bindGetParentNode(useCase: DefaultGetParentNodeHandle): GetParentNodeHandle

    @Binds
    abstract fun bindGetBackupsChildrenNodes(useCase: DefaultGetBackupsChildrenNodes): GetBackupsChildrenNodes

    @Binds
    abstract fun bindGetIncomingSharesChildrenNode(useCase: DefaultGetIncomingSharesChildrenNode): GetIncomingSharesChildrenNode

    @Binds
    abstract fun bindGetOutgoingSharesChildrenNode(useCase: DefaultGetOutgoingSharesChildrenNode): GetOutgoingSharesChildrenNode

    @Binds
    abstract fun bindGetPublicLinks(useCase: DefaultGetPublicLinks): GetPublicLinks

    companion object {

        @Provides
        fun provideGetRootFolder(megaNodeRepository: MegaNodeRepository): GetRootFolder =
            GetRootFolder(megaNodeRepository::getRootNode)

        @Provides
        fun provideHasBackupsChildren(megaNodeRepository: MegaNodeRepository): HasBackupsChildren =
            HasBackupsChildren(megaNodeRepository::hasBackupsChildren)

        @Provides
        fun provideMonitorUserAlerts(notificationsRepository: NotificationsRepository): MonitorUserAlertUpdates =
            MonitorUserAlertUpdates(notificationsRepository::monitorUserAlerts)

        @Provides
        fun provideAuthorizeNode(megaNodeRepository: MegaNodeRepository): AuthorizeNode =
            AuthorizeNode(megaNodeRepository::authorizeNode)

        @Provides
        fun provideGetBackupsNode(megaNodeRepository: MegaNodeRepository): GetBackupsNode =
            GetBackupsNode(megaNodeRepository::getBackupsNode)
    }
}
