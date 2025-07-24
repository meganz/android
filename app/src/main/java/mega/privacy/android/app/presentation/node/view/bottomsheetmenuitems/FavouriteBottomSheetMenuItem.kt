package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.node.model.menuaction.FavouriteMenuAction
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.UpdateNodeFavoriteUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Favourite bottom sheet menu action
 *
 * @param menuAction [FavouriteMenuAction]
 */
class FavouriteBottomSheetMenuItem @Inject constructor(
    override val menuAction: FavouriteMenuAction,
    private val updateNodeFavoriteUseCase: UpdateNodeFavoriteUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && isNodeInRubbish.not()
            && accessPermission == AccessPermission.OWNER
            && node.isFavourite.not()
            && isInBackups.not()


    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        parentCoroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    updateNodeFavoriteUseCase(nodeId = node.id, isFavorite = node.isFavourite.not())
                }.onFailure { Timber.e("Error updating favourite node $it") }
            }
        }
    }

    override val groupId = 3
}