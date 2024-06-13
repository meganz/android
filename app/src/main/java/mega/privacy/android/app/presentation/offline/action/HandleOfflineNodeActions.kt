package mega.privacy.android.app.presentation.offline.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import java.io.File
import java.util.UUID

/**
 * Composable function to handle offline node share action clicks
 */
@Composable
fun HandleOfflineNodeActions(
    viewModel: OfflineNodeActionsViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EventEffect(
        event = uiState.shareFilesEvent,
        onConsumed = viewModel::onShareFilesEventConsumed
    ) {
        startShareFilesIntent(context = context, files = it)
    }

    EventEffect(
        event = uiState.sharesNodeLinksEvent,
        onConsumed = viewModel::onShareNodeLinksEventConsumed
    ) {
        startShareLinksIntent(context = context, title = it.first, links = it.second)
    }
}

private fun startShareFilesIntent(context: Context, files: List<File>) {
    var intentType: String? = null
    for (file in files) {
        val type = typeForName(file.getName()).type
        if (intentType == null) {
            intentType = type
        } else if (!TextUtils.equals(intentType, type)) {
            intentType = "*"
            break
        }
    }
    val uris = ArrayList<Uri>()
    for (file in files) {
        uris.add(FileUtil.getUriForFile(context, file))
    }
    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
    shareIntent.setType("$intentType/*")
    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.context_share))
    )
}

private fun startShareLinksIntent(context: Context, title: String?, links: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, links)
        title?.let {
            putExtra(Intent.EXTRA_SUBJECT, it)
        } ?: run {
            val uniqueId = UUID.randomUUID()
            putExtra(Intent.EXTRA_SUBJECT, "${uniqueId}.url")
        }
        type = Constants.TYPE_TEXT_PLAIN
    }
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.context_share)
        )
    )
}
