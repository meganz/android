package mega.privacy.android.core.nodecomponents.action.clickhandler

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetLocalFilePathUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ShareActionClickHandler @Inject constructor(
    private val getLocalFilePathUseCase: GetLocalFilePathUseCase,
    private val exportNodesUseCase: ExportNodeUseCase,
    private val getFileUriUseCase: GetFileUriUseCase,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ShareMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                val path = runCatching {
                    getLocalFilePathUseCase(node)
                }.getOrElse {
                    Timber.e(it)
                    null
                }
                if (node is TypedFileNode && path != null) {
                    getLocalFileUri(provider.context, path)?.let {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = node.type.mimeType
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                            putExtra(Intent.EXTRA_SUBJECT, node.name)
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        }
                        provider.coroutineScope.ensureActive()
                        provider.context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                provider.context.getString(sharedResR.string.general_share)
                            )
                        )
                    }
                } else {
                    val publicLink = node.exportedData?.publicLink
                    if (publicLink != null) {
                        provider.coroutineScope.ensureActive()
                        startShareIntent(
                            context = provider.context,
                            path = publicLink,
                            name = node.name
                        )
                    } else {
                        val exportPath = exportNodesUseCase(node.id)
                        provider.coroutineScope.ensureActive()
                        startShareIntent(
                            context = provider.context,
                            path = exportPath,
                            name = node.name
                        )
                    }
                }
            }
        }
    }

    private fun startShareIntent(context: Context, path: String, name: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TEXT, path)
            type = "text/plain"
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(sharedResR.string.general_share)
            )
        )
    }

    private suspend fun getLocalFileUri(context: Context, filePath: String) = runCatching {
        val fileProviderAuthority = context.packageName + ".providers.fileprovider"
        getFileUriUseCase(File(filePath), fileProviderAuthority)
    }.onFailure { Timber.e("Error getting local file uri: ${it.message}") }.getOrNull()
}
