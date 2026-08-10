package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.interfaces.ActionBackupNodeCallback
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Constants.NODE_NAME_REGEX
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.FileUtil.TXT_EXTENSION
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.TextUtil.getCursorPositionOfName
import mega.privacy.android.app.utils.Util.SHOW_IM_DELAY
import mega.privacy.android.app.utils.Util.isOffline
import mega.privacy.android.app.utils.ViewUtils.hideKeyboard
import mega.privacy.android.app.utils.ViewUtils.showSoftKeyboardDelayed
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDotName
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDoubleDotName
import mega.privacy.android.domain.usecase.node.NodeExistsInCurrentLocationUseCase
import mega.privacy.android.navigation.MegaNavigatorEntryPoint
import mega.privacy.android.navigation.OpenTextEditorParams
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.thirdpartylib.twemoji.EmojiEditText
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.util.Locale

object MegaNodeDialogUtil {
    private const val TYPE_RENAME = 0
    private const val TYPE_NEW_FOLDER = 1
    private const val TYPE_NEW_FILE = 2
    private const val TYPE_NEW_URL_FILE = 3
    private const val TYPE_NEW_TXT_FILE = 4
    const val IS_NEW_FOLDER_DIALOG_SHOWN = "IS_NEW_FOLDER_DIALOG_SHOWN"
    const val NEW_FOLDER_DIALOG_TEXT = "NEW_FOLDER_DIALOG_TEXT"
    const val IS_NEW_TEXT_FILE_SHOWN = "IS_NEW_TEXT_FILE_SHOWN"
    const val NEW_TEXT_FILE_TEXT = "NEW_TEXT_FILE_TEXT"
    private const val ERROR_EMPTY_EXTENSION = "ERROR_EMPTY_EXTENSION"
    private const val ERROR_DIFFERENT_EXTENSION = "ERROR_DIFFERENT_EXTENSION"
    private const val NO_ERROR = "NO_ERROR"

    // Backup warning dialog
    const val BACKUP_HANDLED_ITEM: String = "BackupHandleItem"
    const val BACKUP_HANDLED_NODE = "BackupHandleNode"
    const val BACKUP_NODE_TYPE = "BackupNodeType"
    const val BACKUP_ACTION_TYPE = "BackupActionType"
    const val BACKUP_DIALOG_WARN = "BackupDialogWarn"

    // Backup node type
    const val BACKUP_NONE = -1 // The folder is not belong to the MyBackup
    const val BACKUP_ROOT = 0 // MyBackup folder
    const val BACKUP_DEVICE = 1 // Device folder
    const val BACKUP_FOLDER = 2 // Backup folders underneath device folders
    const val BACKUP_FOLDER_CHILD = 3 // All backups underneath BACKUP_FOLDER

    // For backup node actions
    const val ACTION_BACKUP_SHARE_FOLDER = 6
    const val ACTION_MENU_BACKUP_SHARE_FOLDER = 7

    /**
     * Creates and shows a TYPE_RENAME dialog to rename a node.
     *
     * The dialog handles validation only; the actual SDK rename call is delegated to the
     * [onRenameConfirmed] callback so that callers can drive it through a Hilt-injected
     * `RenameNodeUseCase`.
     *
     * @param context            Current context.
     * @param node               A valid node.
     * @param snackbarShower     Interface to show snackbar.
     * @param actionNodeCallback Callback to finish the rename action if needed, null otherwise.
     * @param onRenameConfirmed  Invoked with `(nodeHandle, newName)` when the user confirms a
     *                           valid new name. The caller is responsible for performing the
     *                           rename via `RenameNodeUseCase` and surfacing any result UI.
     * @param getRootNodeUseCase
     * @param nodeExistsInCurrentLocationUseCase
     * @return The rename dialog.
     */
    @JvmStatic
    fun showRenameNodeDialog(
        context: Context,
        node: MegaNode?,
        snackbarShower: SnackbarShower?,
        actionNodeCallback: ActionNodeCallback?,
        onRenameConfirmed: (nodeHandle: Long, newName: String) -> Unit,
        getRootNodeUseCase: GetRootNodeUseCase,
        nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    ): AlertDialog {
        val renameDialogBuilder = MaterialAlertDialogBuilder(context)

        renameDialogBuilder
            .setTitle(context.getString(sharedR.string.context_rename))
            .setPositiveButton(sharedR.string.context_rename, null)
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, null)

        return setFinalValuesAndShowDialog(
            context = context,
            node = node,
            actionNodeCallback = actionNodeCallback,
            snackbarShower = snackbarShower,
            data = null,
            defaultURLName = null,
            fromHome = false,
            builder = renameDialogBuilder,
            dialogType = TYPE_RENAME,
            onRenameConfirmed = onRenameConfirmed,
            getRootNodeUseCase = getRootNodeUseCase,
            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
        )
    }

    /**
     * Creates and shows a TYPE_NEW_FOLDER dialog to create a new folder.
     *
     * @param context            Current context.
     * @param actionNodeCallback Callback to finish the create folder action if needed, null otherwise.
     * @param parentNode         Required parent node for checking if already exist a folder with that name.
     * @param typedText          Typed text if the dialog has to be shown after a screen rotation.
     * @param getRootNodeUseCase
     * @param nodeExistsInCurrentLocationUseCase
     * @return The create new folder dialog.
     */
    @JvmStatic
    fun showNewFolderDialog(
        context: Context,
        actionNodeCallback: ActionNodeCallback?,
        parentNode: MegaNode?,
        typedText: String? = null,
        getRootNodeUseCase: GetRootNodeUseCase,
        nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    ): AlertDialog {
        val newFolderDialogBuilder = MaterialAlertDialogBuilder(context)

        newFolderDialogBuilder
            .setTitle(sharedR.string.general_new_folder)
            .setPositiveButton(sharedR.string.general_create_label, null)
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, null)

        val dialog = setFinalValuesAndShowDialog(
            context = context,
            node = parentNode,
            actionNodeCallback = actionNodeCallback,
            snackbarShower = null,
            data = null,
            defaultURLName = null,
            fromHome = false,
            builder = newFolderDialogBuilder,
            dialogType = TYPE_NEW_FOLDER,
            getRootNodeUseCase = getRootNodeUseCase,
            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
        )

        if (!typedText.isNullOrEmpty()) {
            dialog.findViewById<EmojiEditText>(R.id.type_text)?.setText(typedText)
        }

        return dialog
    }

    /**
     * Creates and shows a TYPE_NEW_TXT_FILE dialog to create a new text file.
     *
     * @param context   Current context.
     * @param parent    A valid node. Specifically the parent in which the file will be created.
     * @param typedName The previous typed text.
     * @param fromHome  True if the text file will be created from Homepage, false otherwise.
     * @param getRootNodeUseCase
     * @param nodeExistsInCurrentLocationUseCase
     * @return The create new text file dialog.
     */
    @JvmStatic
    fun showNewTxtFileDialog(
        context: Context,
        parent: MegaNode,
        typedName: String?,
        fromHome: Boolean,
        getRootNodeUseCase: GetRootNodeUseCase,
        nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    ): AlertDialog {
        val newTxtFileDialogBuilder = MaterialAlertDialogBuilder(context)

        newTxtFileDialogBuilder
            .setTitle(sharedR.string.general_new_text_file)
            .setPositiveButton(sharedR.string.general_create_label, null)
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, null)

        val dialog = setFinalValuesAndShowDialog(
            context = context,
            node = parent,
            actionNodeCallback = null,
            snackbarShower = null,
            data = null,
            defaultURLName = null,
            fromHome = fromHome,
            builder = newTxtFileDialogBuilder,
            dialogType = TYPE_NEW_TXT_FILE,
            getRootNodeUseCase = getRootNodeUseCase,
            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
        )

        if (typedName != null && typedName != TXT_EXTENSION) {
            dialog.findViewById<EmojiEditText>(R.id.type_text)?.setText(typedName)
        }

        return dialog
    }

    /**
     * Finishes the initialization of the dialog and shows it.
     *
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     * @param snackbarShower     Interface to show snackbar.
     * @param data               Valid data if needed to confirm the action, null otherwise.
     * @param defaultURLName     The default URL name if the dialog is TYPE_NEW_URL_FILE.
     * @param fromHome           True if the text file will be created from Homepage, false otherwise.
     * @param builder            The AlertDialog.Builder to create and show the final dialog.
     * @param dialogType         Indicates the type of dialog. It can be:
     *                              - TYPE_RENAME:       Rename action.
     *                              - TYPE_NEW_FOLDER:   Create new folder action.
     *                              - TYPE_NEW_FILE:     Create new file action.
     *                              - TYPE_NEW_URL_FILE: Create new URL file action.
     * @param getRootNodeUseCase
     * @param nodeExistsInCurrentLocationUseCase
     * @return The created dialog.
     */
    @Suppress("DEPRECATION")
    private fun setFinalValuesAndShowDialog(
        context: Context,
        node: MegaNode?,
        actionNodeCallback: ActionNodeCallback?,
        snackbarShower: SnackbarShower?,
        data: String?,
        defaultURLName: String?,
        fromHome: Boolean,
        builder: AlertDialog.Builder,
        dialogType: Int,
        onRenameConfirmed: ((nodeHandle: Long, newName: String) -> Unit)? = null,
        getRootNodeUseCase: GetRootNodeUseCase,
        nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    ): AlertDialog {
        builder.setView(R.layout.dialog_create_rename_node)

        val dialog = builder.create()

        dialog.apply {
            setOnShowListener {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

                val typeText = findViewById<EmojiEditText>(R.id.type_text)
                val errorText = findViewById<TextView>(R.id.error_text)

                typeText?.apply {
                    when (dialogType) {
                        TYPE_RENAME -> {
                            if (node != null) {
                                setText(node.name)
                                setSelection(0, getCursorPositionOfName(node.isFile, node.name))
                            }
                        }

                        TYPE_NEW_FOLDER -> {
                            setHint(sharedR.string.create_new_folder_dialog_hint_text)
                        }

                        TYPE_NEW_FILE -> {
                            setHint(R.string.context_new_file_name_hint)
                        }

                        TYPE_NEW_URL_FILE -> {
                            if (defaultURLName.isNullOrBlank()) setHint(R.string.context_new_link_name)
                            else {
                                setText(defaultURLName)
                                setSelection(0, getCursorPositionOfName(false, defaultURLName))
                            }
                        }

                        TYPE_NEW_TXT_FILE -> {
                            setHint(R.string.context_new_file_name)
                            setText(TXT_EXTENSION)
                            runDelay(SHOW_IM_DELAY) { setSelection(0) }
                        }
                    }

                    doAfterTextChanged {
                        quitDialogError(typeText, errorText)
                    }

                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            checkActionDialogValue(
                                context = context,
                                node = node,
                                actionNodeCallback = actionNodeCallback,
                                snackbarShower = snackbarShower,
                                typeText = typeText,
                                data = data,
                                errorText = errorText,
                                fromHome = fromHome,
                                dialog = dialog,
                                dialogType = dialogType,
                                onRenameConfirmed = onRenameConfirmed,
                                getRootNodeUseCase = getRootNodeUseCase,
                                nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
                            )
                        }

                        false
                    }
                }

                quitDialogError(typeText, errorText)

                dialog.getButton(BUTTON_POSITIVE)
                    .setOnClickListener {
                        checkActionDialogValue(
                            context = context,
                            node = node,
                            actionNodeCallback = actionNodeCallback,
                            snackbarShower = snackbarShower,
                            typeText = typeText,
                            data = data,
                            errorText = errorText,
                            fromHome = fromHome,
                            dialog = dialog,
                            dialogType = dialogType,
                            onRenameConfirmed = onRenameConfirmed,
                            getRootNodeUseCase = getRootNodeUseCase,
                            nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
                        )
                    }

                typeText?.requestFocus()
                typeText?.showSoftKeyboardDelayed()
            }
        }.show()

        return dialog
    }

    /**
     * Checks, after user's confirmation, if the typed value is valid.
     * - If so, confirms the action.
     * - If not, shows the error in question.
     *
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     * @param snackbarShower     Interface to show snackbar.
     * @param typeText           The input text field.
     * @param data               Valid data if needed to confirm the action, null otherwise.
     * @param errorText          The text field to show the error.
     * @param fromHome           True if the text file will be created from Homepage, false otherwise.
     * @param dialog             The AlertDialog to check.
     * @param dialogType         Indicates the type of dialog. It can be:
     *                              - TYPE_RENAME:       Rename action.
     *                              - TYPE_NEW_FOLDER:   Create new folder action.
     *                              - TYPE_NEW_FILE:     Create new file action.
     *                              - TYPE_NEW_URL_FILE: Create new URL file action.
     * @param getRootNodeUseCase
     * @param nodeExistsInCurrentLocationUseCase
     */
    private fun checkActionDialogValue(
        context: Context,
        node: MegaNode?,
        actionNodeCallback: ActionNodeCallback?,
        snackbarShower: SnackbarShower?,
        typeText: EditText?,
        data: String?,
        errorText: TextView?,
        fromHome: Boolean,
        dialog: AlertDialog,
        dialogType: Int,
        onRenameConfirmed: ((nodeHandle: Long, newName: String) -> Unit)? = null,
        getRootNodeUseCase: GetRootNodeUseCase,
        nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    ) {
        val typedString = typeText?.text.toString().trim()

        when {
            typedString.isEmpty() -> {
                showDialogError(
                    typeText,
                    errorText,
                    context.getString(sharedR.string.general_invalid_string)
                )
            }

            typedString.isInvalidDotName() -> {
                showDialogError(
                    typeText,
                    errorText,
                    context.getString(sharedR.string.general_invalid_dot_name_warning),
                )
            }

            typedString.isInvalidDoubleDotName() -> {
                showDialogError(
                    typeText,
                    errorText,
                    context.getString(sharedR.string.general_invalid_double_dot_name_warning),
                )
            }

            NODE_NAME_REGEX.matcher(typedString).find() -> {
                showDialogError(
                    typeText,
                    errorText,
                    context.getString(
                        sharedR.string.general_invalid_characters_defined,
                        INVALID_CHARACTERS
                    )
                )
            }

            nameAlreadyExists(
                typedString = typedString,
                isRenameAction = dialogType == TYPE_RENAME,
                node = node,
                getRootNodeUseCase = getRootNodeUseCase,
                nodeExistsInCurrentLocationUseCase = nodeExistsInCurrentLocationUseCase,
            ) -> {
                showDialogError(
                    typeText,
                    errorText,
                    context.getString(
                        if (dialogType == TYPE_RENAME || dialogType == TYPE_NEW_FOLDER) R.string.same_item_name_warning
                        else R.string.same_file_name_warning
                    )
                )
            }

            else -> {
                when (dialogType) {
                    TYPE_RENAME -> {
                        if (node != null && typedString != node.name) {
                            if (isOffline(context)) {
                                return
                            }

                            val oldMimeType = MimeTypeList.typeForName(node.name)
                            var newExtension = MimeTypeList.typeForName(typedString).extension
                            if (newExtension == typedString.lowercase(Locale.ROOT)) newExtension =
                                ""

                            when (if (node.isFolder) NO_ERROR else isValidRenameDialogValue(
                                oldMimeType,
                                newExtension
                            )) {
                                ERROR_EMPTY_EXTENSION -> {
                                    typeText?.hideKeyboard()

                                    showDialogError(
                                        typeText,
                                        errorText,
                                        context.getString(
                                            R.string.file_without_extension,
                                            oldMimeType.extension
                                        )
                                    )

                                    snackbarShower?.showSnackbar(
                                        SNACKBAR_TYPE,
                                        context.getString(R.string.file_without_extension_warning),
                                        MEGACHAT_INVALID_HANDLE
                                    )

                                    return
                                }

                                ERROR_DIFFERENT_EXTENSION -> {
                                    typeText?.hideKeyboard()

                                    showFileExtensionWarning(
                                        context,
                                        node,
                                        typedString,
                                        actionNodeCallback,
                                        onRenameConfirmed,
                                    )
                                }

                                NO_ERROR -> {
                                    confirmRenameAction(
                                        node,
                                        typedString,
                                        actionNodeCallback,
                                        onRenameConfirmed,
                                    )
                                }
                            }
                        }
                    }

                    TYPE_NEW_FOLDER -> {
                        actionNodeCallback?.createFolder(typedString)
                    }

                    TYPE_NEW_FILE -> {
                        if (context is FileExplorerActivity) {
                            context.createFile(typedString, data, node, false)
                        }
                    }

                    TYPE_NEW_URL_FILE -> {
                        if (context is FileExplorerActivity) {
                            context.createFile(typedString, data, node, true)
                        }
                    }

                    TYPE_NEW_TXT_FILE -> {
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            MegaNavigatorEntryPoint::class.java,
                        ).megaNavigator.openTextEditor(
                            context = context,
                            params = OpenTextEditorParams.CloudNode(
                                nodeId = NodeId(node?.handle ?: MegaApiJava.INVALID_HANDLE),
                                nodeSourceType = null,
                                mode = TextEditorMode.Create,
                                fileName = typedString,
                                fromHome = fromHome,
                            ),
                        )
                    }
                }

                dialog.dismiss()
            }
        }
    }

    /**
     * Confirms the rename action by delegating the SDK call to the caller.
     *
     * @param node              A valid node to be renamed.
     * @param typedString       Typed name.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     * @param onRenameConfirmed Invoked with `(nodeHandle, newName)` so the caller can run the
     *                          rename via its Hilt-injected `RenameNodeUseCase`.
     */
    private fun confirmRenameAction(
        node: MegaNode,
        typedString: String,
        actionNodeCallback: ActionNodeCallback?,
        onRenameConfirmed: ((nodeHandle: Long, newName: String) -> Unit)?,
    ) {
        onRenameConfirmed?.invoke(node.handle, typedString)
        actionNodeCallback?.actionConfirmed()
    }

    /**
     * Checks if should allow the rename action:
     * - Should allow it if the new file name has the same extension than the old one.
     * - Should not allow it and show the corresponding error if the new file name has:
     *      * An empty extension and is not a text file.
     *      * A different extension than the old name.
     *
     * @param oldMimeType   Current mimeType of the file.
     * @param newExtension  New typed extension name for the file.
     * @return The corresponding error to show or not the corresponding warning.
     */
    private fun isValidRenameDialogValue(oldMimeType: MimeTypeList, newExtension: String): String {
        return when {
            newExtension.isEmpty() && !oldMimeType.isValidTextFileType -> ERROR_EMPTY_EXTENSION
            oldMimeType.extension != newExtension -> ERROR_DIFFERENT_EXTENSION
            else -> NO_ERROR
        }
    }

    /**
     * Shows a warning dialog informing the file extension changed after rename a file.
     *
     * @param context           Current context.
     * @param node              A valid node to be renamed.
     * @param typedString       Typed name.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     * @param onRenameConfirmed Invoked with `(nodeHandle, newName)` so the caller can run the
     *                          rename via its Hilt-injected `RenameNodeUseCase`.
     */
    private fun showFileExtensionWarning(
        context: Context,
        node: MegaNode,
        typedString: String,
        actionNodeCallback: ActionNodeCallback?,
        onRenameConfirmed: ((nodeHandle: Long, newName: String) -> Unit)?,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.file_extension_change_title))
            .setMessage(context.getString(R.string.file_extension_change_warning))
            .setPositiveButton(context.getString(sharedR.string.general_dialog_cancel_button), null)
            .setNegativeButton(context.getString(R.string.action_change_anyway)) { _, _ ->
                confirmRenameAction(node, typedString, actionNodeCallback, onRenameConfirmed)
            }
            .show()
    }

    /**
     * Shows an error in a dialog and updates the input text field UI in consequence.
     *
     * @param typeText  The input text field.
     * @param errorText The text field to show the error.
     * @param error     Text to show as error.
     */
    @SuppressLint("UseCompatLoadingForColorStateLists")
    private fun showDialogError(typeText: EditText?, errorText: TextView?, error: String) {
        if (typeText != null) {
            setErrorAwareInputAppearance(typeText, true)
        }

        typeText?.requestFocus()

        errorText?.apply {
            visibility = VISIBLE
            text = error
        }
    }

    /**
     * Hides an error from a dialog and updates the input text field UI in consequence.
     *
     * @param typeText  The input text field.
     * @param errorText The text field to hide the error.
     */
    @SuppressLint("UseCompatLoadingForColorStateLists")
    private fun quitDialogError(typeText: EditText?, errorText: TextView?) {
        if (typeText != null) {
            setErrorAwareInputAppearance(typeText, false)
        }

        typeText?.requestFocus()
        errorText?.visibility = GONE
    }

    /**
     * Checks if the newTextFileDialog is shown. If so, saves it's state on outState.
     *
     * @param newTextFileDialog The dialog to check.
     * @param outState          Bundle where the state of the dialog will be save.
     */
    @JvmStatic
    fun checkNewTextFileDialogState(newTextFileDialog: AlertDialog?, outState: Bundle) {
        val isNewTextFileDialogShown = newTextFileDialog != null && newTextFileDialog.isShowing

        if (isNewTextFileDialogShown) {
            outState.putBoolean(IS_NEW_TEXT_FILE_SHOWN, true)
            val typeText = newTextFileDialog?.findViewById<EmojiEditText>(R.id.type_text)

            if (typeText != null) {
                outState.putString(NEW_TEXT_FILE_TEXT, typeText.text.toString())
            }
        }
    }

    /**
     * Checks if a node with [typedString] already exists within the same parent folder:
     * - If the action is rename, the parent is the parent of [node].
     * - If not:
     *      * If [node] is null, the parent is the cloud root.
     *      * If not, [node] itself is the parent.
     *
     * @param typedString    Typed text to set as the new node name.
     * @param isRenameAction True if the action is rename, false otherwise.
     * @param node           Node to rename, or the parent where the new file/folder
     *                       should be created.
     */
    @JvmStatic
    fun nameAlreadyExists(
        typedString: String,
        isRenameAction: Boolean,
        node: MegaNode?,
        getRootNodeUseCase: GetRootNodeUseCase,
        nodeExistsInCurrentLocationUseCase: NodeExistsInCurrentLocationUseCase,
    ): Boolean {

        return runBlocking {
            val parentId = when {
                node == null -> getRootNodeUseCase()?.id
                isRenameAction -> NodeId(node.parentHandle)
                else -> NodeId(node.handle)
            } ?: return@runBlocking false

            nodeExistsInCurrentLocationUseCase(parentId, typedString)
        }
    }

    /**
     * Creates an [AlertDialog], warning Users that sharing Backups Folders can only be in read-only mode
     *
     * @param activity The Activity reference
     * @param actionBackupNodeCallback The [ActionBackupNodeCallback]
     * @param handleList The list of selected Node Handles
     * @param megaNode The Backup Node to be shared
     * @param nodeType The Backup Node type - BACKUP_NONE / BACKUP_ROOT / BACKUP_DEVICE / BACKUP_FOLDER / BACKUP_FOLDER_CHILD
     * @param actionType Indicates the Backup Node action type - ACTION_MENU_BACKUP_SHARE_FOLDER / ACTION_BACKUP_SHARE_FOLDER
     *
     * @return The [AlertDialog]
     */
    @JvmStatic
    fun createBackupsWarningDialog(
        activity: Activity,
        actionBackupNodeCallback: ActionBackupNodeCallback,
        handleList: ArrayList<Long>?,
        megaNode: MegaNode?,
        nodeType: Int,
        actionType: Int,
    ): AlertDialog {
        val dialogClickListener =
            DialogInterface.OnClickListener { dialog: DialogInterface?, buttonType: Int ->
                when (buttonType) {
                    BUTTON_POSITIVE -> {
                        actionBackupNodeCallback.actionExecute(
                            handleList = handleList,
                            megaNode = megaNode,
                            nodeType = nodeType,
                            actionType = actionType,
                        )
                    }

                    BUTTON_NEGATIVE -> actionBackupNodeCallback.actionCancel(dialog, actionType)
                }
            }
        val layout: LayoutInflater = activity.layoutInflater
        val view = layout.inflate(R.layout.dialog_backup_operate_tip, null)
        val tvTitle = view.findViewById<TextView>(R.id.title)
        val tvContent = view.findViewById<TextView>(R.id.backup_tip_content)

        tvTitle.setText(R.string.backup_share_permission_title)
        tvContent.setText(R.string.backup_share_permission_text)
        handleList?.let { nonNullHandleList ->
            tvContent.setText(
                if (nonNullHandleList.size > 1) {
                    R.string.backup_share_with_root_permission_text
                } else {
                    R.string.backup_multi_share_permission_text
                }
            )
        }
        val builder = MaterialAlertDialogBuilder(activity)
            .setView(view)
        if (handleList != null) {
            if (handleList.size > 1 && nodeType == BACKUP_ROOT) {
                builder.setPositiveButton(
                    activity.getString(R.string.general_positive_button),
                    dialogClickListener
                )
                builder.setNegativeButton(
                    activity.getString(sharedR.string.general_dialog_cancel_button),
                    dialogClickListener
                )
            } else {
                builder.setPositiveButton(
                    activity.getString(R.string.button_permission_info),
                    dialogClickListener
                )
            }
        } else {
            builder.setPositiveButton(
                activity.getString(R.string.button_permission_info),
                dialogClickListener
            )
        }
        val dialog = builder.show()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    /**
     * Checks if the newFolderDialog is shown. If so, saves it's state on outState.
     *
     * @param outState          Bundle where the state of the dialog will be save.
     */
    @JvmStatic
    fun AlertDialog?.checkNewFolderDialogState(outState: Bundle) {
        if (this == null || !isShowing) {
            return
        }

        outState.putBoolean(IS_NEW_FOLDER_DIALOG_SHOWN, true)
        val typeText = findViewById<EmojiEditText>(R.id.type_text)

        if (typeText != null) {
            outState.putString(NEW_FOLDER_DIALOG_TEXT, typeText.text.toString())
        }
    }
}
