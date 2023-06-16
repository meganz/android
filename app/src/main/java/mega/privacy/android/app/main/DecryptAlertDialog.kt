package mega.privacy.android.app.main

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.DialogErrorHintBinding
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Util

/**
 * A generic alert dialog for asking the user to input the passphrase and
 * trigger the decryption of links (e.g. password protected link, file/folder link without key)
 * The host of this dialog should implement its specific decryption procedure.
 * If the decryption failed, an indication of wrong passphrase would show.
 */
class DecryptAlertDialog : DialogFragment() {

    private lateinit var binding: DialogErrorHintBinding

    /**
     * Listener to handle positive or negative operation by user
     */
    interface DecryptDialogListener {

        /**
         * Handle when user clicks positive button.
         *
         * @param key of user input
         */
        fun onDialogPositiveClick(key: String?)

        /**
         * Handle when user clicks negative button
         */
        fun onDialogNegativeClick()
    }

    /**
     * Builder class to build a dialog
     */
    class Builder {
        private var title: String? = null
        private var message: String? = null
        private var posStringId = 0
        private var negStringId = 0
        private var errorStringId = 0
        private var key: String? = null
        private var shownPassword = false

        /**
         * Set title of dialog
         *
         * @param title
         */
        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        /**
         * set message of dialog
         *
         * @param message
         */
        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        /**
         * Set text of positive button
         *
         * @param resId  String res ID
         */
        fun setPosText(resId: Int): Builder {
            posStringId = resId
            return this
        }

        /**
         * Set text of negative button
         *
         * @param resId  String res ID
         */
        fun setNegText(resId: Int): Builder {
            negStringId = resId
            return this
        }

        /**
         * Set text of error message
         *
         * @param resId  String res ID
         */
        fun setErrorMessage(resId: Int): Builder {
            errorStringId = resId
            return this
        }

        /**
         * The key user have input
         *
         * @param key
         */
        fun setKey(key: String?): Builder {
            this.key = key
            return this
        }

        /**
         * Set whether to show the password
         *
         * @param shown
         */
        fun setShownPassword(shown: Boolean): Builder {
            shownPassword = shown
            return this
        }

        /**
         * Create the instance of dialog
         *
         * @return instance of the dialog
         */
        fun build(): DecryptAlertDialog = DecryptAlertDialog().apply {
            arguments = bundleOf(
                EXTRA_KEY to key,
                EXTRA_MESSAGE to message,
                EXTRA_TITLE to title,
                EXTRA_ERROR_STRING_ID to errorStringId,
                EXTRA_SHOW_PASSWORD to shownPassword,
                EXTRA_NEGATIVE_STRING_ID to negStringId,
                EXTRA_POSITIVE_STRING_ID to posStringId
            )
        }
    }

    private lateinit var listener: DecryptDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = (activity as? DecryptDialogListener)
            ?: throw NullPointerException("Host activity need implement DecryptDialogListener")
    }

    /**
     * Overridden method to create the dialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(
            requireContext(), R.style.ThemeOverlay_Mega_MaterialAlertDialog
        )
        binding = DialogErrorHintBinding.inflate(requireActivity().layoutInflater)
        val title = requireArguments().getString(EXTRA_TITLE)
        val message = requireArguments().getString(EXTRA_MESSAGE)
        val key = requireArguments().getString(EXTRA_KEY)
        val isShowPassword = requireArguments().getBoolean(EXTRA_SHOW_PASSWORD)
        val positiveStringId = requireArguments().getInt(EXTRA_POSITIVE_STRING_ID)
        val negativeStringId = requireArguments().getInt(EXTRA_NEGATIVE_STRING_ID)
        val errorStringId = requireArguments().getInt(EXTRA_ERROR_STRING_ID)
        builder
            .setTitle(title)
            .setView(binding.root)
            .setOnKeyListener { _: DialogInterface?, keyCode: Int, event: KeyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    listener.let {
                        it.onDialogNegativeClick()
                        return@setOnKeyListener true
                    }
                }
                false
            }
            .setMessage(message)
            .setPositiveButton(positiveStringId, null)
            .setNegativeButton(negativeStringId, null)
        binding.text.setSingleLine()

        val editor = binding.text
        if (isShowPassword) {
            editor.inputType =
                editor.inputType or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.errorText.setText(errorStringId)
        if (key.isNullOrEmpty()) {
            editor.hint = getString(R.string.password_text)
            editor.setTextColor(
                getThemeColor(
                    requireContext(),
                    android.R.attr.textColorPrimary
                )
            )
        } else {
            showErrorMessage(key)
        }
        editor.imeOptions = EditorInfo.IME_ACTION_DONE
        editor.setImeActionLabel(getString(R.string.general_ok), EditorInfo.IME_ACTION_DONE)
        editor.setOnEditorActionListener(TextView.OnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validateInput()) {
                    listener.onDialogPositiveClick(key)
                }
                dismiss()
                return@OnEditorActionListener true
            }
            false
        })
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                hideErrorMessage()
            }
        })
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        // Set onClickListeners for buttons after showing the dialog would prevent
        // the dialog from dismissing automatically on clicking the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (validateInput()) {
                listener.onDialogPositiveClick(binding.text.text.toString())
                dismiss()
            }
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            listener.onDialogNegativeClick()
        }
        Util.showKeyboardDelayed(binding.text)
        return dialog
    }

    private fun showErrorMessage(key: String) {
        with(binding.text) {
            setText(key)
            setSelectAllOnFocus(true)
            setErrorAwareInputAppearance(this, true)
        }
        binding.error.visibility = View.VISIBLE
    }

    private fun hideErrorMessage() {
        binding.error.visibility = View.GONE
        setErrorAwareInputAppearance(binding.text, false)
    }

    private fun validateInput(): Boolean {
        val key = binding.text.text.toString()
        if (key.isEmpty()) {
            showErrorMessage(key)
            return false
        }
        return true
    }

    /**
     * OnResume
     */
    override fun onResume() {
        super.onResume()
        Util.showKeyboardDelayed(binding.text)
    }

    companion object {
        private const val EXTRA_KEY = "key"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_POSITIVE_STRING_ID = "positive_string_id"
        private const val EXTRA_NEGATIVE_STRING_ID = "negative_string_id"
        private const val EXTRA_ERROR_STRING_ID = "error_string_id"
        private const val EXTRA_SHOW_PASSWORD = "show_password"
    }
}