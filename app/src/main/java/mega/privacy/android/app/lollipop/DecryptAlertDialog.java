package mega.privacy.android.app.lollipop;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.TextUtil;

import static mega.privacy.android.app.utils.Util.showKeyboardDelayed;

/**
 * A generic alert dialog for asking the user to input the passphrase and
 * trigger the decryption of links (e.g. password protected link, file/folder link without key)
 * The host of this dialog should implement its specific decryption procedure.
 * If the decrytion failed, an indication of wrong passphrase would show.
 */
public class DecryptAlertDialog extends DialogFragment {
    private Context mContext;
    private String mKey;
    private View mErrorView;
    private EditText mEdit;

    private DecryptDialogListener mListener;
    private String mTitle;
    private String mMessage;
    private int mPosStringId;
    private int mNegStringId;
    private int mErrorStringId;
    private boolean mShownPassword;

    public interface DecryptDialogListener {
        void onDialogPositiveClick(String key);
        void onDialogNegativeClick();
    }

    public static class Builder {
        private DecryptDialogListener mListener;
        private String mTitle;
        private String mMessage;
        private int mPosStringId;
        private int mNegStringId;
        private int mErrorStringId;
        private String mKey;
        private boolean mShownPassword;

        public Builder setListener(DecryptDialogListener listener) {
            mListener = listener;
            return this;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setMessage(String message) {
            mMessage = message;
            return this;
        }

        public Builder setPosText(int resId) {
            mPosStringId = resId;
            return this;
        }

        public Builder setNegText(int resId) {
            mNegStringId = resId;
            return this;
        }

        public Builder setErrorMessage(int resId) {
            mErrorStringId = resId;
            return this;
        }

        public Builder setKey(String key) {
            mKey = key;
            return this;
        }

        public Builder setShownPassword(Boolean value) {
            mShownPassword = value;
            return this;
        }

        public DecryptAlertDialog build() {
            DecryptAlertDialog dialog = new DecryptAlertDialog();
            dialog.mListener = mListener;
            dialog.mTitle = mTitle;
            dialog.mMessage = mMessage;
            dialog.mPosStringId = mPosStringId;
            dialog.mNegStringId = mNegStringId;
            dialog.mErrorStringId = mErrorStringId;
            dialog.mKey = mKey;
            dialog.mShownPassword = mShownPassword;

            return dialog;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mContext = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext,
                R.style.AppCompatAlertDialogStyle);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_error_hint, null);

        builder.setTitle(mTitle).setView(v)
                .setOnKeyListener((dialog, keyCode, event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP
                        && mListener != null) {
                        mListener.onDialogNegativeClick();
                        return true;
                    }
                    return false;
                })
                .setMessage(mMessage)
                .setPositiveButton(mPosStringId, null)
                .setNegativeButton(mNegStringId, null);

        mEdit = v.findViewById(R.id.text);
        mEdit.setSingleLine();
        if (mShownPassword) {
            mEdit.setInputType(mEdit.getInputType() | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        }

        mErrorView = v.findViewById(R.id.error);
        ((TextView)v.findViewById(R.id.error_text)).setText(mErrorStringId);

        if (TextUtil.isTextEmpty(mKey)) {
            mEdit.setHint(getString(R.string.password_text));
            mEdit.setTextColor(ContextCompat.getColor(mContext, R.color.text_secondary));
        } else {
            showErrorMessage();
        }

        mEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mEdit.setImeActionLabel(getString(R.string.general_ok),EditorInfo.IME_ACTION_DONE);
        mEdit.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validateInput() && mListener != null) {
                    mListener.onDialogPositiveClick(mKey);
                    dismiss();
                }
                return true;
            }
            return false;
        });

        mEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                hideErrorMessage();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // Set onClickListeners for buttons after showing the dialog would prevent
        // the dialog from dismissing automatically on clicking the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
            if (validateInput() && mListener != null) {
                mListener.onDialogPositiveClick(mKey);
                dismiss();
            }
        });
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener((view) -> {
            if (mListener != null) {
                mListener.onDialogNegativeClick();
            }
        });

        showKeyboardDelayed(mEdit);
        return dialog;
    }

    private void showErrorMessage() {
        if (mEdit == null || mErrorView == null) return;

        mEdit.setText(mKey);
        mEdit.setSelectAllOnFocus(true);
        mEdit.setTextColor(ContextCompat.getColor(mContext, R.color.dark_primary_color));
        mEdit.getBackground().mutate().clearColorFilter();
        mEdit.getBackground().mutate().setColorFilter(ContextCompat.getColor(
                mContext, R.color.dark_primary_color), PorterDuff.Mode.SRC_ATOP);

        mErrorView.setVisibility(View.VISIBLE);
    }

    private void hideErrorMessage() {
        if (mEdit == null || mErrorView == null) return;

        mErrorView.setVisibility(View.GONE);
        mEdit.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text));
        mEdit.getBackground().mutate().clearColorFilter();
        mEdit.getBackground().mutate().setColorFilter(ContextCompat.getColor(
                mContext, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
    }

    private boolean validateInput() {
        mKey = mEdit.getText().toString();
        if (TextUtil.isTextEmpty(mKey)) {
            mKey = "";
            showErrorMessage();
            return false;
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        showKeyboardDelayed(mEdit);
    }
}
