package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.MoveListener
import mega.privacy.android.app.listeners.RemoveListener
import mega.privacy.android.app.listeners.RenameListener
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.textFileEditor.TextFileEditorActivity
import mega.privacy.android.app.textFileEditor.TextFileEditorActivity.Companion.FROM_HOME_PAGE
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.CREATE_MODE
import mega.privacy.android.app.textFileEditor.TextFileEditorViewModel.Companion.MODE
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.TXT_EXTENSION
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil.getCursorPositionOfName
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.*
import nz.mega.documentscanner.utils.ViewUtils.hideKeyboard
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.util.*

object MegaNodeDialogUtil {
    private const val TYPE_RENAME = 0
    private const val TYPE_NEW_FOLDER = 1
    private const val TYPE_NEW_FILE = 2
    private const val TYPE_NEW_URL_FILE = 3
    private const val TYPE_NEW_TXT_FILE = 4
    const val IS_NEW_TEXT_FILE_SHOWN = "IS_NEW_TEXT_FILE_SHOWN"
    const val NEW_TEXT_FILE_TEXT = "NEW_TEXT_FILE_TEXT"
    private const val ERROR_EMPTY_EXTENSION = "ERROR_EMPTY_EXTENSION"
    private const val ERROR_DIFFERENT_EXTENSION = "ERROR_DIFFERENT_EXTENSION"
    private const val NO_ERROR = "NO_ERROR"

    /**
     * Creates and shows a TYPE_RENAME dialog to rename a node.
     *
     * @param context            Current context.
     * @param node               A valid node.
     * @param snackbarShower interface to show snackbar.
     * @param actionNodeCallback Callback to finish the rename action if needed, null otherwise.
     * @return The rename dialog.
     */
    @JvmStatic
    fun showRenameNodeDialog(
        context: Context,
        node: MegaNode,
        snackbarShower: SnackbarShower?,
        actionNodeCallback: ActionNodeCallback?
    ): AlertDialog {
        val renameDialogBuilder = MaterialAlertDialogBuilder(context)

        renameDialogBuilder
            .setTitle(getString(R.string.context_rename))
            .setPositiveButton(R.string.context_rename, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, node, actionNodeCallback, snackbarShower,
            null, null, false, renameDialogBuilder, TYPE_RENAME
        )
    }

    /**
     * Creates and shows a TYPE_NEW_FOLDER dialog to create a new folder.
     *
     * @param context            Current context.
     * @param actionNodeCallback Callback to finish the create folder action if needed, null otherwise.
     * @return The create new folder dialog.
     */
    @JvmStatic
    fun showNewFolderDialog(
        context: Context,
        actionNodeCallback: ActionNodeCallback?
    ): AlertDialog {
        val newFolderDialogBuilder = MaterialAlertDialogBuilder(context)

        newFolderDialogBuilder
            .setTitle(R.string.menu_new_folder)
            .setPositiveButton(R.string.general_create, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, null, actionNodeCallback, null,
            null, null, false, newFolderDialogBuilder, TYPE_NEW_FOLDER
        )
    }

    /**
     * Creates and shows a TYPE_NEW_FILE dialog to create a new file.
     *
     * @param context Current context.
     * @param parent  A valid node. Specifically the parent in which the folder will be created.
     * @param data    Valid data. Specifically the content of the new file.
     * @return The create new file dialog.
     */
    @JvmStatic
    fun showNewFileDialog(context: Context, parent: MegaNode, data: String): AlertDialog {
        val newFileDialogBuilder = MaterialAlertDialogBuilder(context)

        newFileDialogBuilder
            .setTitle(R.string.context_new_file_name)
            .setPositiveButton(R.string.general_ok, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, parent, null, null,
            data, null, false, newFileDialogBuilder, TYPE_NEW_FILE
        )
    }

    /**
     * Creates and shows a TYPE_NEW_URL_FILE dialog to create a new URL file.
     *
     * @param context        Current context.
     * @param parent         A valid node. Specifically the parent in which the folder will be created.
     * @param data           Valid data. Specifically the content of the new URL file.
     * @param defaultURLName Default name of the URL if has, null otherwise.
     * @return The create new URL file dialog.
     */
    @JvmStatic
    fun showNewURLFileDialog(
        context: Context,
        parent: MegaNode,
        data: String,
        defaultURLName: String?
    ): AlertDialog {
        val newURLFileDialogBuilder = MaterialAlertDialogBuilder(context)

        newURLFileDialogBuilder
            .setTitle(R.string.dialog_title_new_link)
            .setPositiveButton(R.string.general_ok, null)
            .setNegativeButton(R.string.general_cancel, null)

        return setFinalValuesAndShowDialog(
            context, parent, null, null,
            data, defaultURLName, false, newURLFileDialogBuilder, TYPE_NEW_URL_FILE
        )
    }

    /**
     * Creates and shows a TYPE_NEW_TXT_FILE dialog to create a new text file.
     *
     * @param context   Current context.
     * @param parent    A valid node. Specifically the parent in which the file will be created.
     * @param typedName The previous typed text.
     * @param fromHome  True if the text file will be created from Homepage, false otherwise.
     * @return The create new text file dialog.
     */
    @JvmStatic
    fun showNewTxtFileDialog(
        context: Context,
        parent: MegaNode,
        typedName: String?,
        fromHome: Boolean
    ): AlertDialog {
        val newTxtFileDialogBuilder = MaterialAlertDialogBuilder(context)

        newTxtFileDialogBuilder
            .setTitle(R.string.dialog_title_new_text_file)
            .setPositiveButton(R.string.general_create, null)
            .setNegativeButton(R.string.general_cancel, null)

        val dialog = setFinalValuesAndShowDialog(
            context,
            parent,
            null,
            null,
            null,
            null,
            fromHome,
            newTxtFileDialogBuilder,
            TYPE_NEW_TXT_FILE
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
     * @param snackbarShower interface to show snackbar.
     * @param data               Valid data if needed to confirm the action, null otherwise.
     * @param defaultURLName     The default URL name if the dialog is TYPE_NEW_URL_FILE.
     * @param fromHome           True if the text file will be created from Homepage, false otherwise.
     * @param builder            The AlertDialog.Builder to create and show the final dialog.
     * @param dialogType         Indicates the type of dialog. It can be:
     *                            - TYPE_RENAME:       Rename action.
     *                            - TYPE_NEW_FOLDER:   Create new folder action.
     *                            - TYPE_NEW_FILE:     Create new file action.
     *                            - TYPE_NEW_URL_FILE: Create new URL file action.
     * @return The created dialog.
     */
    private fun setFinalValuesAndShowDialog(
        context: Context,
        node: MegaNode?,
        actionNodeCallback: ActionNodeCallback?,
        snackbarShower: SnackbarShower?,
        data: String?,
        defaultURLName: String?,
        fromHome: Boolean,
        builder: AlertDialog.Builder,
        dialogType: Int
    ): AlertDialog {
        builder.setView(R.layout.dialog_create_rename_node)

        val dialog = builder.create()

        dialog.apply {
            setOnShowListener {
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
                            setHint(R.string.context_new_folder_name)
                        }
                        TYPE_NEW_FILE -> {
                            setHint(R.string.context_new_file_name_hint)
                        }
                        TYPE_NEW_URL_FILE -> {
                            if (isTextEmpty(defaultURLName)) setHint(R.string.context_new_link_name)
                            else {
                                setText(defaultURLName)
                                setSelection(0, getCursorPositionOfName(false, defaultURLName))
                            }
                        }
                        TYPE_NEW_TXT_FILE -> {
                            setHint(R.string.context_new_file_name)
                            setText(TXT_EXTENSION)
                            runDelay(SHOW_IM_DELAY.toLong()) { setSelection(0) }
                        }
                    }

                    doAfterTextChanged {
                        quitDialogError(typeText, errorText)
                    }

                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            checkActionDialogValue(
                                context, node, actionNodeCallback, snackbarShower,
                                typeText, data, errorText, fromHome, dialog, dialogType
                            )
                        }

                        false
                    }
                }

                quitDialogError(typeText, errorText)

                dialog.getButton(BUTTON_POSITIVE)
                    .setOnClickListener {
                        checkActionDialogValue(
                            context, node, actionNodeCallback, snackbarShower,
                            typeText, data, errorText, fromHome, dialog, dialogType
                        )
                    }

                showKeyboardDelayed(typeText)
            }
        }.show()

        return dialog
    }

    /**
     * Checks, after user's confirmation, if the typed value is valid.
     * - If so, confirms the action.
     * - If not, shows the error in question.
     *
     * @param context           Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     * @param snackbarShower interface to show snackbar.
     * @param typeText           The input text field.
     * @param data               Valid data if needed to confirm the action, null otherwise.
     * @param errorText          The text field to show the error.
     * @param fromHome           True if the text file will be created from Homepage, false otherwise.
     * @param dialog             The AlertDialog to check.
     * @param dialogType         Indicates the type of dialog. It can be:
     *                           - TYPE_RENAME:       Rename action.
     *                            - TYPE_NEW_FOLDER:   Create new folder action.
     *                           - TYPE_NEW_FILE:     Create new file action.
     *                           - TYPE_NEW_URL_FILE: Create new URL file action.
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
        dialogType: Int
    ) {
        val typedString = typeText?.text.toString().trim()

        when {
            typedString.isEmpty() -> {
                showDialogError(
                    typeText,
                    errorText,
                    getString(R.string.invalid_string)
                )
            }
            NODE_NAME_REGEX.matcher(typedString).find() -> {
                showDialogError(
                    typeText,
                    errorText,
                    getString(R.string.invalid_characters_defined)
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

                            when (if (node.isFolder) NO_ERROR else isValidRenameDialogValue(
                                oldMimeType,
                                newExtension
                            )) {
                                ERROR_EMPTY_EXTENSION -> {
                                    typeText?.hideKeyboard()

                                    showDialogError(
                                        typeText,
                                        errorText,
                                        getString(
                                            R.string.file_without_extension,
                                            oldMimeType.extension
                                        )
                                    )

                                    snackbarShower?.showSnackbar(
                                        SNACKBAR_TYPE,
                                        getString(R.string.file_without_extension_warning),
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
                                        oldMimeType.extension,
                                        newExtension,
                                        snackbarShower,
                                        actionNodeCallback
                                    )
                                }
                                NO_ERROR -> {
                                    confirmRenameAction(
                                        context,
                                        node,
                                        typedString,
                                        snackbarShower,
                                        actionNodeCallback
                                    )
                                }
                            }
                        }
                    }
                    TYPE_NEW_FOLDER -> {
                        actionNodeCallback?.createFolder(typedString)
                    }
                    TYPE_NEW_FILE -> {
                        if (context is FileExplorerActivityLollipop) {
                            context.createFile(typedString, data, node, false)
                        }
                    }
                    TYPE_NEW_URL_FILE -> {
                        if (context is FileExplorerActivityLollipop) {
                            context.createFile(typedString, data, node, true)
                        }
                    }
                    TYPE_NEW_TXT_FILE -> {
                        val textFileEditor = Intent(context, TextFileEditorActivity::class.java)
                            .putExtra(MODE, CREATE_MODE)
                            .putExtra(INTENT_EXTRA_KEY_FILE_NAME, typedString)
                            .putExtra(INTENT_EXTRA_KEY_HANDLE, node?.handle)
                            .putExtra(FROM_HOME_PAGE, fromHome)

                        context.startActivity(textFileEditor)
                    }
                }

                dialog.dismiss()
            }
        }
    }

    /**
     * Confirms the rename action.
     *
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param typedString        Typed name.
     * @param snackbarShower     Interface to show snackbar.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     */
    private fun confirmRenameAction(
        context: Context,
        node: MegaNode,
        typedString: String,
        snackbarShower: SnackbarShower?,
        actionNodeCallback: ActionNodeCallback?
    ) {
        val megaApi = MegaApplication.getInstance().megaApi

        megaApi.renameNode(
            node,
            typedString,
            RenameListener(
                snackbarShower,
                context,
                showSnackbar = true,
                isMyChatFilesFolder = false,
                actionNodeCallback = actionNodeCallback
            )
        )

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
     * @param context            Current context.
     * @param node               A valid node if needed to confirm the action, null otherwise.
     * @param typedString        Typed name.
     * @param oldExtension       Current file extension.
     * @param newExtension       New file extension.
     * @param snackbarShower     Interface to show snackbar.
     * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
     */
    private fun showFileExtensionWarning(
        context: Context,
        node: MegaNode,
        typedString: String,
        oldExtension: String,
        newExtension: String,
        snackbarShower: SnackbarShower?,
        actionNodeCallback: ActionNodeCallback?
    ) {
        val keepExtension = if (oldExtension == node.name) "" else oldExtension

        val typedOldExt =
            if (keepExtension.isEmpty()) typedString.substring(0, typedString.lastIndexOf("."))
            else typedString.substring(0, typedString.lastIndexOf(".") + 1) + oldExtension

        val message = if (keepExtension.isEmpty() && newExtension.isNotEmpty()) {
            getString(R.string.file_extension_change_warning_old_empty, newExtension)
        } else if (keepExtension.isNotEmpty() && newExtension.isEmpty()) {
            getString(R.string.file_extension_change_warning_new_empty, keepExtension)
        } else {
            getString(R.string.file_extension_change_warning, keepExtension, newExtension)
        }

        val useButton = if (newExtension.isEmpty()) {
            getString(R.string.action_use_empty_new_extension)
        } else {
            getString(R.string.action_use_new_extension, newExtension)
        }

        val keepButton = if (keepExtension.isEmpty()) {
            getString(R.string.action_keep_empty_old_extension)
        } else {
            getString(R.string.action_keep_old_extension, keepExtension)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.file_extension_change_title))
            .setMessage(message)
            .setPositiveButton(keepButton) { _, _ ->
                if (typedOldExt == node.name) {
                    return@setPositiveButton
                }

                confirmRenameAction(context, node, typedOldExt, snackbarShower, actionNodeCallback)
            }
            .setNegativeButton(useButton) { _, _ ->
                confirmRenameAction(context, node, typedString, snackbarShower, actionNodeCallback)
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
     * Move a node into rubbish bin, or remove it if it's already moved into rubbish bin.
     *
     * @param handle handle of the node
     * @param activity Android activity
     * @param snackbarShower interface to show snackbar
     */
    @JvmStatic
    @Suppress("DEPRECATION")
    fun moveToRubbishOrRemove(
        handle: Long,
        activity: Activity,
        snackbarShower: SnackbarShower
    ) {
        val megaApi = MegaApplication.getInstance().megaApi

        if (!isOnline(activity)) {
            snackbarShower.showSnackbar(getString(R.string.error_server_connection_problem))
            return
        }

        val node = megaApi.getNodeByHandle(handle) ?: return
        val rubbishNode = megaApi.rubbishNode

        if (rubbishNode.handle != getRootParentNode(node).handle) {
            MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setMessage(getString(R.string.confirmation_move_to_rubbish))
                .setPositiveButton(getString(R.string.general_move)) { _, _ ->
                    val progress = android.app.ProgressDialog(activity)
                    progress.setMessage(getString(R.string.context_move_to_trash))

                    megaApi.moveNode(
                        node, rubbishNode,
                        MoveListener {
                            progress.dismiss()

                            if (it) {
                                activity.finish()
                            } else {
                                snackbarShower.showSnackbar(getString(R.string.context_no_moved))
                            }
                        })

                    progress.show()
                }
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show()
        } else {
            MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setMessage(getString(R.string.confirmation_delete_from_mega))
                .setPositiveButton(getString(R.string.general_remove)) { _, _ ->
                    val progress = android.app.ProgressDialog(activity)
                    progress.setMessage(getString(R.string.context_delete_from_mega))

                    megaApi.remove(node, RemoveListener {
                        progress.dismiss()

                        if (it) {
                            snackbarShower.showSnackbar(getString(R.string.context_correctly_removed))
                            activity.finish()
                        } else {
                            snackbarShower.showSnackbar(getString(R.string.context_no_removed))
                        }
                    })

                    progress.show()
                }
                .setNegativeButton(getString(R.string.general_cancel), null)
                .show()
        }
    }
}
