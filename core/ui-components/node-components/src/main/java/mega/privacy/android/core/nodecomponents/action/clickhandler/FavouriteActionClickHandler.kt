package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.FavouriteMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.UpdateNodeFavoriteUseCase
import timber.log.Timber
import javax.inject.Inject

class FavouriteActionClickHandler @Inject constructor(
    private val updateNodeFavoriteUseCase: UpdateNodeFavoriteUseCase,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is FavouriteMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    updateNodeFavoriteUseCase(nodeId = node.id, isFavorite = node.isFavourite.not())
                }.onFailure { Timber.e("Error updating favourite node $it") }
                    .onSuccess {
                        provider.viewModel.dismiss()
                    }
            }
        }
    }
}
