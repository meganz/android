package mega.privacy.android.app.di.search

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.GetCloudExplorerSearchNode
import mega.privacy.android.app.domain.usecase.DefaultCloudExplorerSearchNode
import mega.privacy.android.app.domain.usecase.DefaultGetSearchInSharesNodes
import mega.privacy.android.app.domain.usecase.DefaultGetSearchLinkSharesNodes
import mega.privacy.android.app.domain.usecase.DefaultGetSearchOutSharesNodes
import mega.privacy.android.app.domain.usecase.DefaultIncomingExplorerSearchNode
import mega.privacy.android.app.domain.usecase.DefaultSearchFromMegaNodeParent
import mega.privacy.android.app.domain.usecase.GetIncomingExplorerSearchNode
import mega.privacy.android.app.domain.usecase.GetSearchInSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchLinkSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchOutSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchFromMegaNodeParent

@Module
@InstallIn(ViewModelComponent::class)
abstract class SearchUseCases {
    @Binds
    abstract fun bindGetSearchInShare(defaultGetSearchInSharesNodes: DefaultGetSearchInSharesNodes): GetSearchInSharesNodes

    @Binds
    abstract fun bindGetSearchOutShare(defaultGetSearchOutSharesNodes: DefaultGetSearchOutSharesNodes): GetSearchOutSharesNodes

    @Binds
    abstract fun bindGetSearchLinkShare(defaultGetSearchLinkSharesNodes: DefaultGetSearchLinkSharesNodes): GetSearchLinkSharesNodes

    @Binds
    abstract fun bindSearchFromMegaNodeParent(defaultSearchFromMegaNodeParent: DefaultSearchFromMegaNodeParent): GetSearchFromMegaNodeParent

    @Binds
    abstract fun bindCloudSearchNode(defaultCloudExplorerSearchNode: DefaultCloudExplorerSearchNode): GetCloudExplorerSearchNode

    @Binds
    abstract fun bindIncomingSharesExploreNode(defaultIncomingExplorerSearchNode: DefaultIncomingExplorerSearchNode): GetIncomingExplorerSearchNode
}