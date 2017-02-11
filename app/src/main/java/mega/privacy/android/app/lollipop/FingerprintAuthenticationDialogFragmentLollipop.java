package mega.privacy.android.app.lollipop;

/**
 * Created by Loza on 08/02/2017.
 */


import android.app.Activity;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import mega.privacy.android.app.R;

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class FingerprintAuthenticationDialogFragmentLollipop extends DialogFragment
        implements FingerprintUiHelper.Callback {

    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private PinLockActivityLollipop mActivity;


    public static FingerprintAuthenticationDialogFragmentLollipop newInstance(FingerprintManager.CryptoObject cryptoObject) {
        FingerprintAuthenticationDialogFragmentLollipop myDialogFragment = new FingerprintAuthenticationDialogFragmentLollipop();
        myDialogFragment.setCryptoObject(cryptoObject);
        return myDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(getString(R.string.fingerprint_dialog_title));
        View v = inflater.inflate(R.layout.dialog_fingerprint_container, container, false);

        getDialog().setCanceledOnTouchOutside(false);

        Button mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        View mFingerprintContent = v.findViewById(R.id.fingerprint_container);

        mFingerprintUiHelper = new FingerprintUiHelper(
                mActivity.getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);

        mCancelButton.setText(R.string.general_cancel);
        mFingerprintContent.setVisibility(View.VISIBLE);

        // If fingerprint authentication is not available, switch immediately to the backup
        // (password) screen.
        if (!mFingerprintUiHelper.isFingerprintAuthAvailable()) {
            finishUsingFingerprint();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(mCryptoObject);
    }


    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (PinLockActivityLollipop) activity;
    }

    /**
     * Sets the crypto object to be passed in when authenticating with fingerprint.
     */
    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    private void finishUsingFingerprint() {
        // Fingerprint is not used anymore. Stop listening for it.
        mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAuthenticated() {
        // Callback from FingerprintUiHelper. Let the activity know that authentication was
        // successful.
        mFingerprintUiHelper.stopListening();
        ((FingerprintAuthenticationListerner)getActivity()).onSuccess();
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();

    }

    @Override
    public void onError() {
        dismiss();
    }

    interface FingerprintAuthenticationListerner {

        void onSuccess();

    }

}