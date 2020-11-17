package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.OverDiskQuotaPaywallActivity
import mega.privacy.android.app.components.EditTextCursorWatcher
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import java.util.regex.Pattern

class AlertsAndWarnings {

    companion object {
        private const val REMOVE_LINK_DIALOG_TEXT_MARGIN_LEFT = 25
        private const val REMOVE_LINK_DIALOG_TEXT_MARGIN_TOP = 20
        private const val REMOVE_LINK_DIALOG_TEXT_MARGIN_RIGHT = 10
        private const val REMOVE_LINK_DIALOG_TEXT_SIZE = 15

        private const val RENAME_DIALOG_TEXT_MARGIN_LEFT = 20
        private const val RENAME_DIALOG_TEXT_MARGIN_TOP = 20
        private const val RENAME_DIALOG_TEXT_MARGIN_RIGHT = 17
        private const val RENAME_DIALOG_ERROR_TEXT_MARGIN_LEFT = 3

        private const val RENAME_REGEX = "[*|\\?:\"<>\\\\\\\\/]"

        /**
         * Shows the ODQ Paywall warning. It will not be shown if it is already being displayed or if
         * the user is upgrading to PRO (i.e. in any place of the checkout process).
         */
        @JvmStatic
        fun showOverDiskQuotaPaywallWarning() {
            showOverDiskQuotaPaywallWarning(false)
        }

        /**
         * Shows the ODQ Paywall warning. It will not be shown if it is already being displayed or if
         * the user is upgrading to PRO (i.e. in any place of the checkout process).
         *
         * @param loginFinished Indicates if the login process has already finished.
         */
        @JvmStatic
        fun showOverDiskQuotaPaywallWarning(loginFinished: Boolean) {
            val app = MegaApplication.getInstance()

            // If app is doing login, the ODQ will be displayed at login finish
            if (app.currentActivity is LoginActivityLollipop && !loginFinished) {
                return;
            }

            if (app.currentActivity is OverDiskQuotaPaywallActivity) {
                return
            }

            if (app.currentActivity is ManagerActivityLollipop) {
                val upAFL = (app.currentActivity as ManagerActivityLollipop).upgradeAccountFragment
                if (upAFL != null && upAFL.isVisible) {
                    return
                }
            }

            val intent = Intent(app.applicationContext, OverDiskQuotaPaywallActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            app.startActivity(intent)
        }

        /**
         * Shows a resume transfers warning.
         * It will be displayed if the queue of transfers is paused and a new chat upload starts.
         *
         * @param context current Context.
         */
        @JvmStatic
        fun showResumeTransfersWarning(context: Context) {
            if (context is BaseActivity
                && context.resumeTransfersWarning != null
                && context.resumeTransfersWarning.isShowing
            ) {
                return
            }

            val resumeTransfersDialogBuilder =
                AlertDialog.Builder(context, R.style.ResumeTransfersWarning)

            resumeTransfersDialogBuilder.setTitle(R.string.warning_resume_transfers)
                .setMessage(R.string.warning_message_resume_transfers)
                .setCancelable(false)
                .setPositiveButton(R.string.button_resume_individual_transfer) { dialog, which ->
                    MegaApplication.getInstance().megaApi.pauseTransfers(false)

                    if (context is ChatActivityLollipop) {
                        context.updatePausedUploadingMessages()
                    }

                    dialog.dismiss()
                }
                .setNegativeButton(R.string.general_cancel) { dialog, which ->
                    dialog.dismiss()
                }.setOnDismissListener { dialog ->
                    if (context is BaseActivity) {
                        context.setIsResumeTransfersWarningShown(false)
                    }
                }

            if (context is BaseActivity) {
                context.setIsResumeTransfersWarningShown(true)
                context.resumeTransfersWarning = resumeTransfersDialogBuilder.create()
            }

            resumeTransfersDialogBuilder.show()
        }

        /**
         * Shows a confirm remove link alert dialog.
         *
         * @param context current Context
         * @param onPositive callback when positive button is clicked
         */
        @JvmStatic
        fun showConfirmRemoveLinkDialog(context: Context, onPositive: () -> Unit) {
            val builder = MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialogStyle)

            val dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_link, null)

            dialogLayout.findViewById<TextView>(R.id.dialog_link_link_url).isVisible = false
            dialogLayout.findViewById<TextView>(R.id.dialog_link_link_key).isVisible = false
            dialogLayout.findViewById<TextView>(R.id.dialog_link_symbol).isVisible = false

            val displayMetrics = context.resources.displayMetrics

            val removeText = dialogLayout.findViewById<TextView>(R.id.dialog_link_text_remove)
            (removeText.layoutParams as RelativeLayout.LayoutParams).setMargins(
                Util.scaleWidthPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_LEFT, displayMetrics),
                Util.scaleHeightPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_TOP, displayMetrics),
                Util.scaleWidthPx(REMOVE_LINK_DIALOG_TEXT_MARGIN_RIGHT, displayMetrics),
                0
            )
            removeText.visibility = View.VISIBLE
            removeText.text = context.getString(R.string.context_remove_link_warning_text)

            val scaleW = Util.getScaleW(displayMetrics, displayMetrics.density)
            removeText.setTextSize(
                TypedValue.COMPLEX_UNIT_SP, REMOVE_LINK_DIALOG_TEXT_SIZE * scaleW
            )

            builder.setView(dialogLayout)
                .setPositiveButton(R.string.context_remove) { _, _ ->
                    onPositive()
                }
                .setNegativeButton(R.string.general_cancel, null)
                .create()
                .show()
        }

        /**
         * Shows a rename node alert dialog.
         *
         * @param context current Context
         * @param nodeName the old node name
         * @param isFolder if is the node is a folder
         * @param rename callback when valid new name is set
         */
        @JvmStatic
        fun showRenameDialog(
            context: Context,
            nodeName: String,
            isFolder: Boolean,
            rename: (String) -> Unit
        ) {
            val displayMetrics = context.resources.displayMetrics

            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL

            val input = EditTextCursorWatcher(context, isFolder)
            input.setSingleLine()
            input.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            input.imeOptions = EditorInfo.IME_ACTION_DONE

            input.setImeActionLabel(
                context.getString(R.string.context_rename),
                EditorInfo.IME_ACTION_DONE
            )
            input.setText(nodeName)

            input.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    if (isFolder) {
                        input.setSelection(0, input.text.length)
                    } else {
                        val s = nodeName.split("\\.".toRegex()).toTypedArray()
                        val numParts = s.size
                        var lastSelectedPos = 0
                        if (numParts == 1) {
                            input.setSelection(0, input.text.length)
                        } else if (numParts > 1) {
                            for (i in 0 until numParts - 1) {
                                lastSelectedPos += s[i].length
                                lastSelectedPos++
                            }
                            lastSelectedPos-- // The last point should not be selected)
                            input.setSelection(0, lastSelectedPos)
                        }
                        showKeyboardDelayed(v)
                    }
                }
            }

            val paramsInput = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            paramsInput.setMargins(
                Util.scaleWidthPx(RENAME_DIALOG_TEXT_MARGIN_LEFT, displayMetrics),
                Util.scaleHeightPx(RENAME_DIALOG_TEXT_MARGIN_TOP, displayMetrics),
                Util.scaleWidthPx(RENAME_DIALOG_TEXT_MARGIN_RIGHT, displayMetrics),
                0
            )
            layout.addView(input, paramsInput)

            val paramsErrorLayout = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            paramsErrorLayout.setMargins(
                Util.scaleWidthPx(RENAME_DIALOG_TEXT_MARGIN_LEFT, displayMetrics),
                0,
                Util.scaleWidthPx(RENAME_DIALOG_TEXT_MARGIN_RIGHT, displayMetrics),
                0
            )

            val errorLayout = RelativeLayout(context)
            layout.addView(errorLayout, paramsErrorLayout)

            val errorIcon = ImageView(context)
            errorIcon.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_input_warning)
            )
            errorLayout.addView(errorIcon)
            val paramsIcon = errorIcon.layoutParams as RelativeLayout.LayoutParams

            paramsIcon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            errorIcon.layoutParams = paramsIcon

            errorIcon.setColorFilter(ContextCompat.getColor(context, R.color.login_warning))

            val textError = TextView(context)
            errorLayout.addView(textError)
            val paramsTextError = textError.layoutParams as RelativeLayout.LayoutParams
            paramsTextError.height = ViewGroup.LayoutParams.WRAP_CONTENT
            paramsTextError.width = ViewGroup.LayoutParams.WRAP_CONTENT
            paramsTextError.addRule(RelativeLayout.CENTER_VERTICAL)
            paramsTextError.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            paramsTextError.setMargins(
                Util.scaleWidthPx(RENAME_DIALOG_ERROR_TEXT_MARGIN_LEFT, displayMetrics), 0, 0, 0
            )
            textError.layoutParams = paramsTextError

            textError.setTextColor(ContextCompat.getColor(context, R.color.login_warning))

            errorLayout.visibility = View.GONE

            input.background.mutate().clearColorFilter()
            input.background.mutate().colorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(context, R.color.accentColor), PorterDuff.Mode.SRC_ATOP
            )
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    charSequence: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(editable: Editable) {
                    if (errorLayout.visibility == View.VISIBLE) {
                        errorLayout.visibility = View.GONE
                        input.background.mutate().clearColorFilter()
                        input.background.mutate().colorFilter = PorterDuffColorFilter(
                            ContextCompat.getColor(context, R.color.accentColor),
                            PorterDuff.Mode.SRC_ATOP
                        )
                    }
                }
            })

            var renameDialog: AlertDialog? = null
            val checkInput: (String) -> Unit = { text ->
                val value = text.trim { it <= ' ' }
                if (value.isEmpty()) {
                    input.background.mutate().colorFilter = PorterDuffColorFilter(
                        ContextCompat.getColor(context, R.color.login_warning),
                        PorterDuff.Mode.SRC_ATOP
                    )
                    textError.setText(R.string.invalid_string)
                    errorLayout.visibility = View.VISIBLE
                    input.requestFocus()
                } else {
                    if (Pattern.compile(RENAME_REGEX).matcher(value).find()) {
                        input.background.mutate().colorFilter = PorterDuffColorFilter(
                            ContextCompat.getColor(context, R.color.login_warning),
                            PorterDuff.Mode.SRC_ATOP
                        )
                        textError.setText(R.string.invalid_characters)
                        errorLayout.visibility = View.VISIBLE
                        input.requestFocus()
                    } else {
                        renameDialog?.dismiss()
                        rename(value)
                    }
                }
            }

            input.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    checkInput(input.text.toString())
                    return@OnEditorActionListener true
                }
                false
            })

            renameDialog = MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialogStyle)
                .setTitle(context.getString(R.string.context_rename) + " " + nodeName)
                .setPositiveButton(R.string.context_rename) { _, _ ->
                    checkInput(input.text.toString())
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    input.background.clearColorFilter()
                }
                .setView(layout)
                .show()
        }
    }
}
