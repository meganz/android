package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.model.menuaction.ShareMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import java.util.UUID
import javax.inject.Inject

/**
 * Share menu item
 */
class ShareToolBarMenuItem @Inject constructor(
    override val menuAction: ShareMenuAction,
    @ApplicationScope private val scope: CoroutineScope,
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val exportNodesUseCase: ExportNodeUseCase,
    private val getFileUriUseCase: GetFileUriUseCase,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = selectedNodes.isNotEmpty() && noNodeTakenDown

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        val context = navController.context
        scope.launch {
            val fileNodeList = selectedNodes.filterIsInstance<TypedFileNode>()
            val localFiles = fileNodeList.mapNotNull {
                runCatching {
                    getLocalFilePathUseCase(it)
                }.getOrElse {
                    Timber.e(it)
                    null
                }
            }
            if (localFiles.size == selectedNodes.size) {
                val filesUri = localFiles.mapNotNull {
                    runCatching {
                        getLocalFileUri(it)?.let { filePath ->
                            Uri.parse(filePath)
                        }
                    }.getOrElse {
                        Timber.e(it)
                        null
                    }
                }
                val similarTypes = fileNodeList.groupBy {
                    it.type.mimeType
                }
                val intentType = if (similarTypes.size == 1) {
                    "${similarTypes.keys}"
                } else {
                    "*"
                }
                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "$intentType/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(filesUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        context.getString(R.string.context_share)
                    )
                )
            } else {
                val allExportedNodes = selectedNodes.all { it.exportedData != null }
                if (allExportedNodes) {
                    val uris = selectedNodes.mapNotNull {
                        it.exportedData?.publicLink
                    }
                    shareLinks(
                        context = context,
                        links = uris,
                        selectedNodes = selectedNodes
                    )
                } else {
                    val uris = selectedNodes.mapNotNull {
                        runCatching { exportNodesUseCase(nodeToExport = it.id) }
                            .getOrElse {
                                Timber.e(it)
                                null
                            }
                    }
                    shareLinks(
                        context = context,
                        links = uris,
                        selectedNodes = selectedNodes
                    )
                }
            }
            onDismiss()
        }
    }

    private fun startShareIntent(context: Context, link: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = Constants.TYPE_TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, link)
            putExtra(Intent.EXTRA_SUBJECT, "$title.url")
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.context_share)
            )
        )
    }

    private fun getTitle(selectedNodes: List<TypedNode>): String {
        return if (selectedNodes.size == 1) {
            selectedNodes.first().name
        } else {
            "${UUID.randomUUID()}"
        }
    }

    private suspend fun getLocalFileUri(filePath: String) = runCatching {
        getFileUriUseCase(File(filePath), Constants.AUTHORITY_STRING_FILE_PROVIDER)
    }.onFailure { Timber.e("Error getting local file uri: ${it.message}") }.getOrNull()

    private fun shareLinks(context: Context, links: List<String>, selectedNodes: List<TypedNode>) {
        if (links.isNotEmpty()) {
            val combinedLinks = links.joinToString(separator = "\n\n")
            val title = getTitle(selectedNodes)
            startShareIntent(context, combinedLinks, title)
        }
    }
}