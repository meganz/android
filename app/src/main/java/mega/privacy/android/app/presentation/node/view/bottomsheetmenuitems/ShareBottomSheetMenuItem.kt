package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.model.menuaction.ShareMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.mobile.analytics.event.SearchResultShareMenuItemEvent
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Share bottom sheet menu item
 *
 * @param menuAction [ShareMenuAction]
 */
class ShareBottomSheetMenuItem @Inject constructor(
    override val menuAction: ShareMenuAction,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val exportNodesUseCase: ExportNodeUseCase,
    private val getFileUriUseCase: GetFileUriUseCase,
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

    override val groupId = 7

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        val context = navController.context
        parentCoroutineScope.launch {
            withContext(NonCancellable) {
                Analytics.tracker.trackEvent(SearchResultShareMenuItemEvent)
                val path = runCatching {
                    getLocalFilePathUseCase(node)
                }.getOrElse {
                    Timber.e(it)
                    null
                }
                if (node is TypedFileNode && path != null) {
                    getLocalFileUri(path)?.let {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = node.type.mimeType
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                            putExtra(Intent.EXTRA_SUBJECT, node.name)
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        }
                        parentCoroutineScope.ensureActive()
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.context_share)
                            )
                        )
                    }
                } else {
                    val publicLink = node.exportedData?.publicLink
                    if (publicLink != null) {
                        parentCoroutineScope.ensureActive()
                        startShareIntent(
                            context = context,
                            path = publicLink,
                            name = node.name
                        )
                    } else {
                        val exportPath = exportNodesUseCase(node.id)
                        parentCoroutineScope.ensureActive()
                        startShareIntent(
                            context = context,
                            path = exportPath,
                            name = node.name
                        )
                    }
                }
            }
        }
        onDismiss()
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

    private suspend fun getLocalFileUri(filePath: String) = runCatching {
        getFileUriUseCase(File(filePath), Constants.AUTHORITY_STRING_FILE_PROVIDER)
    }.onFailure { Timber.e("Error getting local file uri: ${it.message}") }.getOrNull()
}