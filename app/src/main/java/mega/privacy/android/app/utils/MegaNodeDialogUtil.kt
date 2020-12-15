package mega.privacy.android.app.utils

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
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.Constants.NODE_NAME_REGEX
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TextUtil.getCursorPositionOfName
import nz.mega.sdk.MegaNode
import java.util.regex.Pattern

class MegaNodeDialogUtil {

    companion object {
        @JvmStatic
        fun showRenameNodeDialog(activity: Activity, node: MegaNode): AlertDialog {
            val renameDialogBuilder =
                AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
            val view = activity.layoutInflater.inflate(R.layout.dialog_create_rename_node, null)

            renameDialogBuilder
                .setTitle(getString(R.string.rename_dialog_title, node.name))
                .setView(view)
                .setPositiveButton(R.string.context_rename, null)
                .setNegativeButton(R.string.general_cancel, null)

            val renameDialog = renameDialogBuilder.create()
            val typeText = view.findViewById<EditText>(R.id.type_text)
            val errorText = view.findViewById<TextView>(R.id.error_text)

            renameDialog.getButton(BUTTON_POSITIVE)
                .setOnClickListener {
                    checkRenameNodeDialogValue(
                        activity,
                        node,
                        typeText,
                        errorText,
                        renameDialog
                    )
                }

            typeText?.apply {
                setText(node.name)
                setSelection(0, getCursorPositionOfName(node.isFile, node.name))

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
                        quitDialogError(typeText, errorText)
                    }
                })

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        checkRenameNodeDialogValue(
                            activity,
                            node,
                            typeText,
                            errorText,
                            renameDialog
                        )
                        true
                    }

                    false
                }
            }

            quitDialogError(typeText, errorText)

            renameDialog.show()
            return renameDialog
        }

        private fun checkRenameNodeDialogValue(
            activity: Activity,
            node: MegaNode,
            typeText: EditText?,
            errorText: TextView?,
            dialog: AlertDialog
        ) {
            val typedString = typeText?.text.toString().trim()

            when {
                typedString.isEmpty() -> {
                    showDialogError(typeText, errorText, getString(R.string.invalid_string))
                }
                Pattern.compile(NODE_NAME_REGEX).matcher(typedString).find() -> {
                    showDialogError(typeText, errorText, getString(R.string.invalid_characters))
                }
                else -> {
                    NodeController(activity).renameNode(node, typedString)
                    dialog.dismiss()
                }
            }
        }

        private fun showDialogError(typeText: EditText?, errorText: TextView?, text: String) {
            typeText?.setBackgroundColor(
                ContextCompat.getColor(
                    MegaApplication.getInstance(),
                    R.color.new_dialog_error_color
                )
            )
            typeText?.requestFocus()

            errorText?.visibility = VISIBLE
            errorText?.text = text
        }

        private fun quitDialogError(typeText: EditText?, errorText: TextView?) {
            typeText?.setBackgroundColor(
                ContextCompat.getColor(
                    MegaApplication.getInstance(),
                    R.color.accentColor
                )
            )
            typeText?.requestFocus()

            errorText?.visibility = GONE
        }
    }
}