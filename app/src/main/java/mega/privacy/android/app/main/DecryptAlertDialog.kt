package mega.privacy.android.app.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
    private var mKey: String? = null
    private var mListener: DecryptDialogListener? = null
    private var mTitle: String? = null
    private var mMessage: String? = null
    private var mPosStringId = 0
    private var mNegStringId = 0
    private var mErrorStringId = 0
    private var mShownPassword = false

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
        private var listener: DecryptDialogListener? = null
        private var title: String? = null
        private var message: String? = null
        private var posStringId = 0
        private var negStringId = 0
        private var errorStringId = 0
        private var key: String? = null
        private var shownPassword = false

        /**
         * Set the dialog listener.
         *
         * @param listener Listener can be null
         * @return the updated instance of Builder
         */
        fun setListener(listener: DecryptDialogListener?): Builder {
            this.listener = listener
            return this
        }

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
            mListener = listener
            mTitle = title
            mMessage = message
            mPosStringId = posStringId
            mNegStringId = negStringId
            mErrorStringId = errorStringId
            mKey = key
            mShownPassword = shownPassword
        }
    }

    /**
     * Overridden method to create the dialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(
            requireContext(), R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        binding = DialogErrorHintBinding.inflate(requireActivity().layoutInflater)
        builder
            .setTitle(mTitle)
            .setView(binding.root)
            .setOnKeyListener { _: DialogInterface?, keyCode: Int, event: KeyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    mListener?.let {
                        it.onDialogNegativeClick()
                        return@setOnKeyListener true
                    }
                }
                false
            }
            .setMessage(mMessage)
            .setPositiveButton(mPosStringId, null)
            .setNegativeButton(mNegStringId, null)
        binding.text.setSingleLine()

        val editor = binding.text
        if (mShownPassword) {
            editor.inputType =
                editor.inputType or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.errorText.setText(mErrorStringId)
        if (mKey.isNullOrEmpty()) {
            editor.hint = getString(R.string.password_text)
            editor.setTextColor(getThemeColor(requireContext(),
                android.R.attr.textColorPrimary))
        } else {
            showErrorMessage()
        }
        editor.imeOptions = EditorInfo.IME_ACTION_DONE
        editor.setImeActionLabel(getString(R.string.general_ok), EditorInfo.IME_ACTION_DONE)
        editor.setOnEditorActionListener(TextView.OnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validateInput()) {
                    mListener?.onDialogPositiveClick(mKey)
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
                mListener?.onDialogPositiveClick(mKey)
                dismiss()
            }
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            mListener?.onDialogNegativeClick()
        }
        Util.showKeyboardDelayed(binding.text)
        return dialog
    }

    private fun showErrorMessage() {
        with(binding.text) {
            setText(mKey)
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
        mKey = binding.text.text.toString()
        if (mKey.isNullOrEmpty()) {
            mKey = ""
            showErrorMessage()
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
}