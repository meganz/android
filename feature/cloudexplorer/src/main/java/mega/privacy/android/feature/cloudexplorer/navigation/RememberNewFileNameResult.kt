package mega.privacy.android.feature.cloudexplorer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.navigation.destination.NewTextFileDialogNavKey
import mega.privacy.android.navigation.destination.NewURLFileDialogNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Listens for New Text / URL File dialog results and, on each arrival, clears the result
 * before invoking [createTextFile] so back-navigation doesn't replay the previous creation.
 * Emissions are safe-cast via `as? String` to avoid runtime `ClassCastException` on the
 * untyped `NavigationHandler.monitorResult` channel.
 */
@Composable
internal fun rememberNewFileNameResult(
    monitorResult: (String) -> Flow<Any?>,
    clearResult: (String) -> Unit,
    startNavKey: ShareTextToMegaNavKey,
    createTextFile: (fileName: String, fileContent: String) -> Unit,
) {
    val textFileResult by monitorResult(NewTextFileDialogNavKey.FILE_NAME_RESULT)
        .collectAsStateWithLifecycle(null)
    val urlFileResult by monitorResult(NewURLFileDialogNavKey.FILE_NAME_RESULT)
        .collectAsStateWithLifecycle(null)
    val textFileName = textFileResult as? String
    val urlFileName = urlFileResult as? String
    val textFileContent = textFileName?.let { startNavKey.buildMessageContent() }
    val urlFileContent = urlFileName?.let { startNavKey.buildURLContent() }

    LaunchedEffect(textFileName, textFileContent) {
        val name = textFileName ?: return@LaunchedEffect
        val content = textFileContent ?: return@LaunchedEffect
        clearResult(NewTextFileDialogNavKey.FILE_NAME_RESULT)
        createTextFile(name, content)
    }

    LaunchedEffect(urlFileName, urlFileContent) {
        val name = urlFileName ?: return@LaunchedEffect
        val content = urlFileContent ?: return@LaunchedEffect
        clearResult(NewURLFileDialogNavKey.FILE_NAME_RESULT)
        createTextFile(name, content)
    }
}

/** `[InternetShortcut]` body so Windows clients can open the shared URL; others see it as text. */
@Composable
private fun ShareTextToMegaNavKey.buildURLContent(): String = buildString {
    append("[InternetShortcut]\n")
    append("URL=").append(text).append("\n\n")
    subject?.let {
        append(stringResource(sharedR.string.new_file_subject_when_uploading))
            .append(": ").append(it).append("\n")
    }
    email?.let {
        append(stringResource(sharedR.string.new_file_email_when_uploading))
            .append(": ").append(it)
    }
}

/** Plain-text body for non-URL shares; prepends an "Email:" line when the share carries one. */
@Composable
private fun ShareTextToMegaNavKey.buildMessageContent(): String =
    buildString {
        email?.let {
            append(stringResource(sharedR.string.new_file_email_when_uploading))
                .append(": ").append(it).append("\n\n")
        }
        append(text)
    }
