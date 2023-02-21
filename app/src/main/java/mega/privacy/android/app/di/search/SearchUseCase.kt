package mega.privacy.android.app.di.search

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.GetCloudExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.DefaultCloudExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.DefaultGetSearchInSharesNodes
import mega.privacy.android.app.domain.usecase.DefaultGetSearchLinkSharesNodes
import mega.privacy.android.app.domain.usecase.DefaultGetSearchOutSharesNodes
import mega.privacy.android.app.domain.usecase.DefaultIncomingExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.DefaultSearchFromMegaNodeParent
import mega.privacy.android.app.domain.usecase.DefaultSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.GetIncomingExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.GetSearchInSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchLinkSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchOutSharesNodes
import mega.privacy.android.app.domain.usecase.GetSearchFromMegaNodeParent
import mega.privacy.android.app.domain.usecase.SearchNodeUseCase

@Module
@InstallIn(ViewModelComponent::class)
abstract class SearchUseCase {
    @Binds
    abstract fun bindGetSearchInShare(defaultGetSearchInSharesNodes: DefaultGetSearchInSharesNodes): GetSearchInSharesNodes

    @Binds
    abstract fun bindGetSearchOutShare(defaultGetSearchOutSharesNodes: DefaultGetSearchOutSharesNodes): GetSearchOutSharesNodes

    @Binds
    abstract fun bindGetSearchLinkShare(defaultGetSearchLinkSharesNodes: DefaultGetSearchLinkSharesNodes): GetSearchLinkSharesNodes

    @Binds
    abstract fun bindSearchNode(defaultSearchNodeUseCase: DefaultSearchNodeUseCase): SearchNodeUseCase

    @Binds
    abstract fun bindSearchFromMegaNodeParent(defaultSearchFromMegaNodeParent: DefaultSearchFromMegaNodeParent): GetSearchFromMegaNodeParent

    @Binds
    abstract fun bindCloudSearchNode(defaultCloudExplorerSearchNodeUseCase: DefaultCloudExplorerSearchNodeUseCase): GetCloudExplorerSearchNodeUseCase

    @Binds
    abstract fun bindIncomingSharesExploreNode(defaultIncomingExplorerSearchNodeUseCase: DefaultIncomingExplorerSearchNodeUseCase): GetIncomingExplorerSearchNodeUseCase
}