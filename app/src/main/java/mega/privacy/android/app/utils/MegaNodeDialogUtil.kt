package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface.BUTTON_POSITIVE
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Constants.NODE_NAME_REGEX
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil.getCursorPositionOfName
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaNode

class MegaNodeDialogUtil {

    companion object {
        private const val TYPE_RENAME = 0
        private const val TYPE_NEW_FOLDER = 1
        private const val TYPE_NEW_FILE = 2
        private const val TYPE_NEW_URL_FILE = 3

        /**
         * Creates and shows a TYPE_RENAME dialog to rename a node.
         *
         * @param context            Current context.
         * @param node               A valid node.
         * @param actionNodeCallback Callback to finish the rename action if needed, null otherwise.
         * @return The rename dialog.
         */
        @JvmStatic
        fun showRenameNodeDialog(
            context: Context,
            node: MegaNode,
            actionNodeCallback: ActionNodeCallback?
        ): AlertDialog {
            val renameDialogBuilder = MaterialAlertDialogBuilder(context)

            renameDialogBuilder
                .setTitle(getString(R.string.rename_dialog_title, node.name))
                .setPositiveButton(R.string.context_rename, null)
                .setNegativeButton(R.string.general_cancel, null)

            return setFinalValuesAndShowDialog(
                context,
                node,
                actionNodeCallback,
                null,
                null,
                renameDialogBuilder,
                TYPE_RENAME
            )
        }

        /**
         * Creates and shows a TYPE_NEW_FOLDER dialog to create a new folder.
         *
         * @param context           Current context.
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
                context,
                null,
                actionNodeCallback,
                null,
                null,
                newFolderDialogBuilder,
                TYPE_NEW_FOLDER
            )
        }

        /**
         * Creates and shows a TYPE_NEW_FILE dialog to create a new file.
         *
         * @param context Current context.
         * @param parent   A valid node. Specifically the parent in which the folder will be created.
         * @param data     Valid data. Specifically the content of the new file.
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
                context,
                parent,
                null,
                data,
                null,
                newFileDialogBuilder,
                TYPE_NEW_FILE
            )
        }

        /**
         * Creates and shows a TYPE_NEW_URL_FILE dialog to create a new URL file.
         *
         * @param context       Current context.
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
                context,
                parent,
                null,
                data,
                defaultURLName,
                newURLFileDialogBuilder,
                TYPE_NEW_URL_FILE
            )
        }

        /**
         * Finish the initialization of the dialog and shows it.
         *
         * @param context           Current context.
         * @param node               A valid node if needed to confirm the action, null otherwise.
         * @param actionNodeCallback Callback to finish the node action if needed, null otherwise.
         * @param data               Valid data if needed to confirm the action, null otherwise.
         * @param defaultURLName     The default URL name if the dialog is TYPE_NEW_URL_FILE.
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
            data: String?,
            defaultURLName: String?,
            builder: AlertDialog.Builder,
            dialogType: Int
        ): AlertDialog {
            builder.setView(R.layout.dialog_create_rename_node)

            var typeText: EmojiEditText? = null
            var errorText: TextView? = null

            val dialog = builder.create()

            dialog.apply {
                setOnShowListener {
                    typeText = findViewById<EmojiEditText>(R.id.type_text)
                    errorText = findViewById<TextView>(R.id.error_text)

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
                        }

                        doAfterTextChanged { quitDialogError(typeText, errorText) }

                        setOnEditorActionListener { _, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                checkActionDialogValue(
                                    context,
                                    node,
                                    actionNodeCallback,
                                    typeText,
                                    data,
                                    errorText,
                                    dialog,
                                    dialogType
                                )
                            }

                            false
                        }
                    }

                    quitDialogError(typeText, errorText)
                    showKeyboardDelayed(typeText)
                }
            }.show()

            dialog.getButton(BUTTON_POSITIVE)
                .setOnClickListener {
                    checkActionDialogValue(
                        context,
                        node,
                        actionNodeCallback,
                        typeText,
                        data,
                        errorText,
                        dialog,
                        dialogType
                    )
                }

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
         * @param typeText           The input text field.
         * @param data               Valid data if needed to confirm the action, null otherwise.
         * @param errorText          The text field to show the error.
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
            typeText: EditText?,
            data: String?,
            errorText: TextView?,
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
                                NodeController(context).renameNode(
                                    node,
                                    typedString,
                                    actionNodeCallback
                                )

                                actionNodeCallback?.actionConfirmed()
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
                    }

                    dialog.dismiss()
                }
            }
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
    }
}