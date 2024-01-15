package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Context
import android.content.Intent
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.model.menuaction.ShareMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.mobile.analytics.event.SearchResultShareMenuItemEvent
import javax.inject.Inject

/**
 * Share bottom sheet menu item
 *
 * @param menuAction [ShareMenuAction]
 */
class ShareBottomSheetMenuItem @Inject constructor(
    override val menuAction: ShareMenuAction,
    @ApplicationScope private val scope: CoroutineScope,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val exportNodesUseCase: ExportNodeUseCase,
    private val getFileByPathUseCase: GetFileByPathUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && accessPermission == AccessPermission.OWNER
            && isNodeInRubbish.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        scope.launch {
            Analytics.tracker.trackEvent(SearchResultShareMenuItemEvent)
            getLocalFilePathUseCase(node)?.let {
                if (it.isNotBlank()) {
                    getFileByPathUseCase(it)?.let { file ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = MimeTypeList.typeForName(file.name).type + "/*"
                            putExtra(Intent.EXTRA_STREAM, it)
                            putExtra(Intent.EXTRA_SUBJECT, file.name)
                            flags =
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        }
                        navController.context.startActivity(
                            Intent.createChooser(
                                shareIntent, navController.context.getString(
                                    R.string.context_share
                                )
                            )
                        )
                    }
                }
            } ?: run {
                val publicLink = node.exportedData?.publicLink
                if (publicLink != null) {
                    startShareIntent(
                        context = navController.context,
                        path = publicLink,
                        name = node.name
                    )
                } else {
                    val exportPath = exportNodesUseCase(node.id)
                    startShareIntent(
                        context = navController.context,
                        path = exportPath,
                        name = node.name
                    )
                }
            }
        }
    }


    private fun startShareIntent(context: Context, path: String, name: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TEXT, path)
            type = Constants.TYPE_TEXT_PLAIN
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.context_share)
            )
        )
    }

    override val groupId = 7
}