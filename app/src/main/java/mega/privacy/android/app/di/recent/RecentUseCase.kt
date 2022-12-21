package mega.privacy.android.app.di.recent

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultGetRecentActionNodes
import mega.privacy.android.app.domain.usecase.DefaultIsPendingShare
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.app.domain.usecase.IsPendingShare
import mega.privacy.android.domain.usecase.DefaultGetRecentActions
import mega.privacy.android.domain.usecase.DefaultUpdateRecentAction
import mega.privacy.android.domain.usecase.GetRecentActions
import mega.privacy.android.domain.usecase.UpdateRecentAction

/**
 * Recent use case module
 *
 * Provides dependencies used by multiple screens in the recent package
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class RecentUseCase {

    /**
     * Provide [UpdateRecentAction] implementation
     *
     * @param useCase
     */
    @Binds
    abstract fun bindUpdateRecentAction(useCase: DefaultUpdateRecentAction): UpdateRecentAction

    /**
     * Provide [GetRecentActionNodes] implementation
     *
     * @param useCase
     */
    @Binds
    abstract fun bindGetNodes(useCase: DefaultGetRecentActionNodes): GetRecentActionNodes

    /**
     * Provide [IsPendingShare] implementation
     *
     * @param useCase
     */
    @Binds
    abstract fun bindIsPendingShare(useCase: DefaultIsPendingShare): IsPendingShare

    /**
     * Provide [GetRecentActions] implementation
     *
     * @param useCase
     */
    @Binds
    abstract fun bindGetRecentActions(useCase: DefaultGetRecentActions): GetRecentActions
}
