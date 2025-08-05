package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveFavouriteMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.UpdateNodeFavoriteUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Remove favourite bottom sheet menu action
 *
 * @param menuAction [RemoveFavouriteMenuAction]
 */
class RemoveFavouriteBottomSheetMenuItem @Inject constructor(
    override val menuAction: RemoveFavouriteMenuAction,
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
            && node.isFavourite

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
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

    override val groupId: Int
        get() = 3
}