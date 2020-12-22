package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface.BUTTON_POSITIVE
import android.text.Editable
import android.text.TextWatcher
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.Constants.NODE_NAME_REGEX
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil.getCursorPositionOfName
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaNode
import java.util.regex.Pattern

class MegaNodeDialogUtil {

    companion object {
        private const val TYPE_RENAME = 0
        private const val TYPE_NEW_FOLDER = 1
        private const val TYPE_NEW_FILE = 2
        private const val TYPE_NEW_URL_FILE = 3

        /**
         * Creates and shows a TYPE_RENAME dialog to rename a node.
         *
         * @param activity Current activity.
         * @param node     A valid node.
         * @return The rename dialog.
         */
        @JvmStatic
        fun showRenameNodeDialog(activity: Activity, node: MegaNode): AlertDialog {
            val renameDialogBuilder =
                AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)

            renameDialogBuilder
                .setTitle(getString(R.string.rename_dialog_title, node.name))
                .setPositiveButton(R.string.context_rename, null)
                .setNegativeButton(R.string.general_cancel, null)

            return setFinalValuesAndShowDialog(
                activity,
                node,
                null,
                null,
                renameDialogBuilder,
                TYPE_RENAME
            )
        }

        /**
         * Creates and shows a TYPE_NEW_FOLDER dialog to create a new folder.
         *
         * @param activity Current activity.
         * @return The create new folder dialog.
         */
        @JvmStatic
        fun showNewFolderDialog(activity: Activity): AlertDialog {
            val newFolderDialogBuilder =
                AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)

            newFolderDialogBuilder
                .setTitle(R.string.menu_new_folder)
                .setPositiveButton(R.string.general_create, null)
                .setNegativeButton(R.string.general_cancel, null)

            return setFinalValuesAndShowDialog(
                activity,
                null,
                null,
                null,
                newFolderDialogBuilder,
                TYPE_NEW_FOLDER
            )
        }

        /**
         * Creates and shows a TYPE_NEW_FILE dialog to create a new file.
         *
         * @param activity Current activity.
         * @param parent   A valid node. Specifically the parent in which the folder will be created.
         * @param data     Valid data. Specifically the content of the new file.
         * @return The create new file dialog.
         */
        @JvmStatic
        fun showNewFileDialog(activity: Activity, parent: MegaNode, data: String): AlertDialog {
            val newFileDialogBuilder =
                AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)

            newFileDialogBuilder
                .setTitle(R.string.context_new_file_name)
                .setPositiveButton(R.string.general_ok, null)
                .setNegativeButton(R.string.general_cancel, null)

            return setFinalValuesAndShowDialog(
                activity,
                parent,
                data,
                null,
                newFileDialogBuilder,
                TYPE_NEW_FILE
            )
        }

        /**
         * Creates and shows a TYPE_NEW_URL_FILE dialog to create a new URL file.
         *
         * @param activity       Current activity.
         * @param parent         A valid node. Specifically the parent in which the folder will be created.
         * @param data           Valid data. Specifically the content of the new URL file.
         * @param defaultURLName Default name of the URL if has, null otherwise.
         * @return The create new URL file dialog.
         */
        @JvmStatic
        fun showNewURLFileDialog(
            activity: Activity,
            parent: MegaNode,
            data: String,
            defaultURLName: String?
        ): AlertDialog {
            val newURLFileDialogBuilder =
                AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)

            newURLFileDialogBuilder
                .setTitle(R.string.dialog_title_new_link)
                .setPositiveButton(R.string.general_ok, null)
                .setNegativeButton(R.string.general_cancel, null)

            return setFinalValuesAndShowDialog(
                activity,
                parent,
                data,
                defaultURLName,
                newURLFileDialogBuilder,
                TYPE_NEW_URL_FILE
            )
        }

        /**
         * Finish the initialization of the dialog and shows it.
         *
         * @param activity       Current activity.
         * @param node           A valid node if needed to confirm the action, null otherwise.
         * @param data           Valid data if needed to confirm the action, null otherwise.
         * @param defaultURLName The default URL name if the dialog is TYPE_NEW_URL_FILE.
         * @param builder        The AlertDialog.Builder to create and show the final dialog.
         * @param dialogType     Indicates the type of dialog. It can be:
         *                        - TYPE_RENAME:       Rename action.
         *                        - TYPE_NEW_FOLDER:   Create new folder action.
         *                        - TYPE_NEW_FILE:     Create new file action.
         *                        - TYPE_NEW_URL_FILE: Create new URL file action.
         * @return The created dialog.
         */
        private fun setFinalValuesAndShowDialog(
            activity: Activity,
            node: MegaNode?,
            data: String?,
            defaultURLName: String?,
            builder: AlertDialog.Builder,
            dialogType: Int
        ): AlertDialog {
            val view = activity.layoutInflater.inflate(R.layout.dialog_create_rename_node, null)
            builder.setView(view)

            val dialog = builder.create()
            val typeText = view.findViewById<EditText>(R.id.type_text)
            val errorText = view.findViewById<TextView>(R.id.error_text)

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

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        quitDialogError(activity, typeText, errorText)
                    }
                })

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        checkActionDialogValue(
                            activity,
                            node,
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

            quitDialogError(activity, typeText, errorText)

            dialog.show()

            dialog.getButton(BUTTON_POSITIVE)
                .setOnClickListener {
                    checkActionDialogValue(
                        activity,
                        node,
                        typeText,
                        data,
                        errorText,
                        dialog,
                        dialogType
                    )
                }

            showKeyboardDelayed(typeText)

            return dialog
        }

        /**
         * Checks, after user's confirmation, if the typed value is valid.
         * - If so, confirms the action.
         * - If not, shows the error in question.
         *
         * @param activity   Current activity.
         * @param node       A valid node if needed to confirm the action, null otherwise.
         * @param typeText   The input text field.
         * @param data       Valid data if needed to confirm the action, null otherwise.
         * @param errorText  The text field to show the error.
         * @param dialog     The AlertDialog to check.
         * @param dialogType Indicates the type of dialog. It can be:
         *                   - TYPE_RENAME:       Rename action.
         *                   - TYPE_NEW_FOLDER:   Create new folder action.
         *                   - TYPE_NEW_FILE:     Create new file action.
         *                   - TYPE_NEW_URL_FILE: Create new URL file action.
         */
        private fun checkActionDialogValue(
            activity: Activity,
            node: MegaNode?,
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
                        activity,
                        typeText,
                        errorText,
                        getString(R.string.invalid_string)
                    )
                }
                Pattern.compile(NODE_NAME_REGEX).matcher(typedString).find() -> {
                    showDialogError(
                        activity,
                        typeText,
                        errorText,
                        getString(R.string.invalid_characters)
                    )
                }
                else -> {
                    when (dialogType) {
                        TYPE_RENAME -> {
                            if (node != null && typedString != node.name) {
                                NodeController(activity).renameNode(node, typedString)
                            }
                        }
                        TYPE_NEW_FOLDER -> {
                            when (activity) {
                                is FileExplorerActivityLollipop -> {
                                    activity.createFolder(typedString)
                                }
                                is ManagerActivityLollipop -> {
                                    activity.createFolder(typedString)
                                }
                                is FileStorageActivityLollipop -> {
                                    activity.createFolder(typedString)
                                }
                            }
                        }
                        TYPE_NEW_FILE -> {
                            if (activity is FileExplorerActivityLollipop) {
                                activity.createFile(typedString, data, node, false)
                            }
                        }
                        TYPE_NEW_URL_FILE -> {
                            if (activity is FileExplorerActivityLollipop) {
                                activity.createFile(typedString, data, node, true)
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
         * @param activity  Current activity.
         * @param typeText  The input text field.
         * @param errorText The text field to show the error.
         * @param error     Text to show as error.
         */
        @SuppressLint("UseCompatLoadingForColorStateLists")
        private fun showDialogError(
            activity: Activity,
            typeText: EditText?,
            errorText: TextView?,
            error: String
        ) {
            typeText?.apply {
                backgroundTintList =
                    activity.resources.getColorStateList(R.color.background_error_input_text)
                setTextColor(ContextCompat.getColor(activity, R.color.dark_primary_color))
                requestFocus()
            }

            errorText?.apply {
                visibility = VISIBLE
                text = error
            }
        }

        /**
         * Hides an error from a dialog and updates the input text field UI in consequence.
         *
         * @param activity  Current activity.
         * @param typeText  The input text field.
         * @param errorText The text field to hide the error.
         */
        @SuppressLint("UseCompatLoadingForColorStateLists")
        private fun quitDialogError(
            activity: Activity, typeText: EditText?, errorText: TextView?
        ) {
            typeText?.apply {
                backgroundTintList =
                    activity.resources.getColorStateList(R.color.background_right_input_text)
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                requestFocus()
            }

            errorText?.visibility = GONE
        }
    }
}