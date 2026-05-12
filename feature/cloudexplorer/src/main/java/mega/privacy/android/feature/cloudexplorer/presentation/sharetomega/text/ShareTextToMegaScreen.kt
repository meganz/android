package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.text

import android.webkit.URLUtil
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaUpload
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.NewTextFileDialogNavKey
import mega.privacy.android.navigation.destination.NewURLFileDialogNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareTextToMegaScreen(
    uiState: ShareTextToMegaUiState,
    startNavKey: ShareTextToMegaNavKey,
    isProcessingAction: Boolean,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onFileUriConsumed: () -> Unit,
) {
    when (uiState) {
        is ShareTextToMegaUiState.Loading -> {
            //See if we need a loading view
        }

        is ShareTextToMegaUiState.Data -> {
            val isURL = URLUtil.isHttpUrl(startNavKey.text) || URLUtil.isHttpsUrl(startNavKey.text)
            val uploadUrisEventState = rememberUploadUrisEventState()
            var folderPickedIdLong by rememberSaveable { mutableLongStateOf(-1L) }
            val folderPickedId = NodeId(folderPickedIdLong)

            EventEffect(
                event = uiState.fileUri,
                onConsumed = onFileUriConsumed,
            ) { uri ->
                uploadUrisEventState.trigger(listOf(uri.toUri()))
            }

            ExplorerScreen(
                explorerMode = if (isURL) ExplorerMode.ShareURLToMega else ExplorerMode.ShareTextToMega,
                startNavKey = startNavKey,
                isInnerNavigation = false,
                nodeExplorerId = uiState.rootNodeId,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                onCloseExplorerScreen = onNavigateBack,
                onNavigateBack = onNavigateBack,
                onNavigate = onNavigate,
                isProcessingAction = isProcessingAction,
                onFolderPicked = { nodeId ->
                    folderPickedIdLong = nodeId.longValue
                    onNavigate(
                        if (isURL) {
                            NewURLFileDialogNavKey(parentNodeId = nodeId)
                        } else {
                            NewTextFileDialogNavKey(
                                parentNodeId = nodeId,
                                returnFileName = true,
                            )
                        }
                    )
                },
            )

            ShareToMegaUpload(
                parentNodeId = folderPickedId,
                uploadUrisEventState = uploadUrisEventState,
                onStartUpload = onStartUpload,
                onCloseExplorerScreen = onNavigateBack,
            )
        }
    }
}

/**
 * Bridges the New Text File / New URL File dialog results back into the share-text flow.
 *
 * The dialogs publish the chosen file name through [NavigationHandler] under the
 * [NewTextFileDialogNavKey.FILE_NAME_RESULT] / [NewURLFileDialogNavKey.FILE_NAME_RESULT]
 * keys. When a name is received this helper:
 *   1. Builds the file content from [startNavKey] (text body or `[InternetShortcut]` block),
 *      *only* when a name has actually arrived — see the `?.let { ... }` conditional
 *      composition below — so the strings are not assembled for every recomposition.
 *   2. Clears the matching result so it is not consumed twice.
 *   3. Invokes [createTextFile] with the chosen name and the built content.
 *
 * The result clearing happens before [createTextFile] so re-entering the destination
 * from back-navigation does not replay the previous file creation.
 */
@Composable
internal fun rememberNewFileNameResult(
    navigationHandler: NavigationHandler,
    startNavKey: ShareTextToMegaNavKey,
    createTextFile: (fileName: String, fileContent: String) -> Unit,
) {
    val textFileName by navigationHandler
        .monitorResult<String?>(NewTextFileDialogNavKey.FILE_NAME_RESULT)
        .collectAsStateWithLifecycle(null)
    val urlFileName by navigationHandler
        .monitorResult<String?>(NewURLFileDialogNavKey.FILE_NAME_RESULT)
        .collectAsStateWithLifecycle(null)
    val textFileContent = textFileName?.let { startNavKey.buildMessageContent() }
    val urlFileContent = urlFileName?.let { startNavKey.buildURLContent() }

    LaunchedEffect(textFileName, textFileContent) {
        val name = textFileName ?: return@LaunchedEffect
        val content = textFileContent ?: return@LaunchedEffect
        navigationHandler.clearResult(NewTextFileDialogNavKey.FILE_NAME_RESULT)
        createTextFile(name, content)
    }

    LaunchedEffect(urlFileName, urlFileContent) {
        val name = urlFileName ?: return@LaunchedEffect
        val content = urlFileContent ?: return@LaunchedEffect
        navigationHandler.clearResult(NewURLFileDialogNavKey.FILE_NAME_RESULT)
        createTextFile(name, content)
    }
}

/**
 * Builds the body of the `.url` Internet Shortcut file that will be uploaded when the user
 * shares an http(s) link. The format follows the `[InternetShortcut]` Windows shortcut
 * convention so Windows clients can open it; non-Windows clients still display it as text.
 *
 * Subject and email lines are appended only when present on the receiver.
 */
@Composable
internal fun ShareTextToMegaNavKey.buildURLContent(): String = buildString {
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

/**
 * Builds the body of the plain text file uploaded when the user shares non-URL text.
 * Prepends an "Email: ..." line when the receiver carries one, then the shared text.
 */
@Composable
internal fun ShareTextToMegaNavKey.buildMessageContent(): String =
    buildString {
        email?.let {
            append(stringResource(sharedR.string.new_file_email_when_uploading))
                .append(": ").append(it).append("\n\n")
        }
        append(text)
    }
