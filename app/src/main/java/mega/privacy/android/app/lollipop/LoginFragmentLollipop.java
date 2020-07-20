package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import com.google.android.material.textfield.TextInputLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.EditTextPIN;
import mega.privacy.android.app.listeners.ChatLogoutListener;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.providers.FileProviderActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;

public class LoginFragmentLollipop extends Fragment implements View.OnClickListener, MegaRequestListenerInterface, MegaChatListenerInterface, View.OnFocusChangeListener, View.OnLongClickListener {

    private static final int READ_MEDIA_PERMISSION = 109;
    private Context context;
    private AlertDialog insertMailDialog;
    private AlertDialog insertMKDialog;

    private LoginFragmentLollipop loginFragment = this;

    private TextView loginTitle;
    private TextView newToMega;
    private TextInputLayout et_userLayout;
    private AppCompatEditText et_user;
    private ImageView et_userError;
    private TextInputLayout et_passwordLayout;
    private AppCompatEditText et_password;
    private ImageView et_passwordError;
    private TextView bRegister;
    private Button bLogin;
    private TextView bForgotPass;
    private LinearLayout loginLogin;
    private LinearLayout loginLoggingIn;
    private LinearLayout loginCreateAccount;
    private ProgressBar loginProgressBar;
    private ProgressBar loginFetchNodesProgressBar;
    private TextView generatingKeysText;
    private TextView queryingSignupLinkText;
    private TextView confirmingAccountText;
    private TextView loggingInText;
    private TextView fetchingNodesText;
    private TextView prepareNodesText;
    private TextView serversBusyText;
    private ScrollView scrollView;

    private RelativeLayout forgotPassLayout;
    private TextView forgotPassTitle;
    private TextView forgotPassFirstP;
    private TextView forgotPassSecondP;
    private TextView forgotPassAction;
    private Button yesMK;
    private Button noMK;

    private RelativeLayout parkAccountLayout;
    private TextView parkAccountTitle;
    private TextView parkAccountFirstP;
    private TextView parkAccountSecondP;
    private Button parkAccountButton;

    private ProgressBar loginInProgressPb;
    private TextView loginInProgressInfo;

    private CountDownTimer timer;
    private boolean firstRequestUpdate = true;

    private float scaleH, scaleW;
    private float density;
    private DisplayMetrics outMetrics;
    private Display display;

    private DatabaseHandler dbH;
    private Handler handler = new Handler();
    private ChatSettings chatSettings;

    private String lastEmail;
    private String lastPassword;
    private String gSession;
    private boolean resumeSesion = false;

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private String confirmLink;

    int numberOfClicksKarere = 0;
    int numberOfClicksSDK = 0;

    private boolean firstTime = true;

    private boolean backWhileLogin;
    private boolean loginClicked = false;

    private Intent intentReceived = null;
    private Bundle extras = null;
    private Uri uriData = null;
    private String action = null;
    private String url = null;
    private long parentHandle = -1;
    private long idChatToJoin = -1;

    private String emailTemp = null;
    private String passwdTemp = null;

    Toolbar tB;
    LinearLayout loginVerificationLayout;
    InputMethodManager imm;
    private EditTextPIN firstPin;
    private EditTextPIN secondPin;
    private EditTextPIN thirdPin;
    private EditTextPIN fourthPin;
    private EditTextPIN fifthPin;
    private EditTextPIN sixthPin;
    private StringBuilder sb = new StringBuilder();
    private String pin = null;
    private TextView pinError;
    private RelativeLayout lostYourDeviceButton;
    private ProgressBar verify2faProgressBar;

    private boolean isFirstTime = true;
    private boolean isErrorShown = false;
    private boolean is2FAEnabled = false;
    private boolean accountConfirmed = false;
    private boolean pinLongClick = false;

    private boolean twoFA = false;
    public static final String NAME_USER_LOCKED = "NAME_USER_LOCKED";
    private Intent receivedIntent;
    private ArrayList<ShareInfo> shareInfos;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            logWarning("context is null");
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        is2FAEnabled = false;
        accountConfirmed = false;

        loginClicked = false;
        backWhileLogin = false;

        UserCredentials credentials = dbH.getCredentials();
        if (credentials != null) {
            logDebug("Credentials NOT null");
            firstTime = false;
        }
        else{
            firstTime = true;
        }

        chatSettings = dbH.getChatSettings();
        if(chatSettings==null){
            logDebug("chatSettings is null --> enable chat by default");
            chatSettings = new ChatSettings();
            dbH.setChatSettings(chatSettings);
        }

        display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        scaleW = getScaleW(outMetrics, density);
        scaleH = getScaleH(outMetrics, density);

        View v = inflater.inflate(R.layout.fragment_login, container, false);

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_login);

        loginTitle = (TextView) v.findViewById(R.id.login_text_view);

        loginTitle.setText(R.string.login_to_mega);
        loginTitle.setOnClickListener(this);

        et_userLayout = v.findViewById(R.id.login_email_text_layout);
        et_user = v.findViewById(R.id.login_email_text);
        et_userError = v.findViewById(R.id.login_email_text_error_icon);
        et_userError.setVisibility(View.GONE);

        et_user.setCursorVisible(true);
        et_user.getBackground().clearColorFilter();
        et_user.requestFocus();

        et_user.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(et_user);
            }
        });

        et_passwordLayout = v.findViewById(R.id.login_password_text_layout);
        et_password = v.findViewById(R.id.login_password_text);
        et_passwordError = v.findViewById(R.id.login_password_text_error_icon);
        et_passwordError.setVisibility(View.GONE);

        et_password.setCursorVisible(true);
        et_password.getBackground().clearColorFilter();

        et_password.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitForm();
                    return true;
                }
                return false;
            }
        });

        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                quitError(et_password);
            }
        });

        et_password.setOnFocusChangeListener((v1, hasFocus) -> setPasswordToggle(et_passwordLayout, hasFocus));

        bLogin = (Button) v.findViewById(R.id.button_login_login);
        bLogin.setText(getString(R.string.login_text).toUpperCase(Locale.getDefault()));
        bLogin.setOnClickListener(this);

        loginInProgressPb = v.findViewById(R.id.pb_login_in_progress);
        loginInProgressInfo = v.findViewById(R.id.text_login_tip);

        bForgotPass = (TextView) v.findViewById(R.id.button_forgot_pass);
        bForgotPass.setText(getString(R.string.forgot_pass).toUpperCase(Locale.getDefault()));
        bForgotPass.setOnClickListener(this);

        loginCreateAccount = (LinearLayout) v.findViewById(R.id.login_create_account_layout);

        newToMega = (TextView) v.findViewById(R.id.text_newToMega);
        newToMega.setOnClickListener(this);

        bRegister = (TextView) v.findViewById(R.id.button_create_account_login);

        bRegister.setText(getString(R.string.create_account).toUpperCase(Locale.getDefault()));
        bRegister.setOnClickListener(this);

        loginLogin = (LinearLayout) v.findViewById(R.id.login_login_layout);
        loginLoggingIn = (LinearLayout) v.findViewById(R.id.login_logging_in_layout);
        loginProgressBar = (ProgressBar) v.findViewById(R.id.login_progress_bar);
        loginFetchNodesProgressBar = (ProgressBar) v.findViewById(R.id.login_fetching_nodes_bar);
        generatingKeysText = (TextView) v.findViewById(R.id.login_generating_keys_text);
        queryingSignupLinkText = (TextView) v.findViewById(R.id.login_query_signup_link_text);
        confirmingAccountText = (TextView) v.findViewById(R.id.login_confirm_account_text);
        loggingInText = (TextView) v.findViewById(R.id.login_logging_in_text);
        fetchingNodesText = (TextView) v.findViewById(R.id.login_fetch_nodes_text);
        prepareNodesText = (TextView) v.findViewById(R.id.login_prepare_nodes_text);
        serversBusyText = (TextView) v.findViewById(R.id.login_servers_busy_text);

        loginLogin.setVisibility(View.VISIBLE);
        loginCreateAccount.setVisibility(View.VISIBLE);
        loginLoggingIn.setVisibility(View.GONE);
        generatingKeysText.setVisibility(View.GONE);
        loggingInText.setVisibility(View.GONE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        loginProgressBar.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);

        forgotPassLayout = (RelativeLayout) v.findViewById(R.id.forgot_pass_full_layout);
        forgotPassTitle = (TextView) v.findViewById(R.id.title_forgot_pass_layout);
        RelativeLayout.LayoutParams forgotPassTitleParams = (RelativeLayout.LayoutParams)forgotPassTitle.getLayoutParams();
        forgotPassTitleParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(70, outMetrics), scaleWidthPx(24, outMetrics), 0);
        forgotPassTitle.setLayoutParams(forgotPassTitleParams);

        forgotPassFirstP = (TextView) v.findViewById(R.id.first_par_forgot_pass_layout);
        RelativeLayout.LayoutParams firstParParams = (RelativeLayout.LayoutParams)forgotPassFirstP.getLayoutParams();
        firstParParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(24, outMetrics), 0);
        forgotPassFirstP.setLayoutParams(firstParParams);

        forgotPassSecondP = (TextView) v.findViewById(R.id.second_par_forgot_pass_layout);
        RelativeLayout.LayoutParams secondParParams = (RelativeLayout.LayoutParams)forgotPassSecondP.getLayoutParams();
        secondParParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(24, outMetrics), 0);
        forgotPassSecondP.setLayoutParams(secondParParams);

        forgotPassAction = (TextView) v.findViewById(R.id.action_forgot_pass_layout);
        RelativeLayout.LayoutParams actionParams = (RelativeLayout.LayoutParams)forgotPassAction.getLayoutParams();
        actionParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(25, outMetrics), scaleWidthPx(24, outMetrics), 0);
        forgotPassAction.setLayoutParams(actionParams);

        yesMK = (Button) v.findViewById(R.id.yes_MK_button);
        LinearLayout.LayoutParams yesMKParams = (LinearLayout.LayoutParams)yesMK.getLayoutParams();
        yesMKParams.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(25, outMetrics), 0, 0);
        yesMK.setLayoutParams(yesMKParams);
        yesMK.setOnClickListener(this);
        yesMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));

        noMK = (Button) v.findViewById(R.id.no_MK_button);
        LinearLayout.LayoutParams noMKParams = (LinearLayout.LayoutParams)noMK.getLayoutParams();
        noMKParams.setMargins(scaleWidthPx(16, outMetrics), scaleHeightPx(25, outMetrics), 0, 0);
        noMK.setLayoutParams(noMKParams);
        noMK.setOnClickListener(this);
        noMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));

        parkAccountLayout = (RelativeLayout) v.findViewById(R.id.park_account_layout);
        parkAccountTitle = (TextView) v.findViewById(R.id.title_park_account_layout);
        RelativeLayout.LayoutParams parkAccountTitleParams = (RelativeLayout.LayoutParams)parkAccountTitle.getLayoutParams();
        parkAccountTitleParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(70, outMetrics), scaleWidthPx(24, outMetrics), 0);
        parkAccountTitle.setLayoutParams(parkAccountTitleParams);

        parkAccountFirstP = (TextView) v.findViewById(R.id.first_par_park_account_layout);
        RelativeLayout.LayoutParams parkAccountFParams = (RelativeLayout.LayoutParams)parkAccountFirstP.getLayoutParams();
        parkAccountFParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(24, outMetrics), 0);
        parkAccountFirstP.setLayoutParams(parkAccountFParams);

        parkAccountSecondP = (TextView) v.findViewById(R.id.second_par_park_account_layout);
        RelativeLayout.LayoutParams parkAccountSParams = (RelativeLayout.LayoutParams)parkAccountSecondP.getLayoutParams();
        parkAccountSParams.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(24, outMetrics), 0);
        parkAccountSecondP.setLayoutParams(parkAccountSParams);

        parkAccountButton = (Button) v.findViewById(R.id.park_account_button);
        RelativeLayout.LayoutParams parkButtonParams = (RelativeLayout.LayoutParams)parkAccountButton.getLayoutParams();
        parkButtonParams.setMargins(0, scaleHeightPx(25, outMetrics),  scaleWidthPx(24, outMetrics), 0);
        parkAccountButton.setLayoutParams(parkButtonParams);
        parkAccountButton.setOnClickListener(this);

        tB  =(Toolbar) v.findViewById(R.id.toolbar_login);
        loginVerificationLayout = (LinearLayout) v.findViewById(R.id.login_2fa);
        loginVerificationLayout.setVisibility(View.GONE);
        lostYourDeviceButton = (RelativeLayout) v.findViewById(R.id.lost_authentication_device);
        lostYourDeviceButton.setOnClickListener(this);
        pinError = (TextView) v.findViewById(R.id.pin_2fa_error_login);
        pinError.setVisibility(View.GONE);
        verify2faProgressBar = (ProgressBar) v.findViewById(R.id.progressbar_verify_2fa);

        imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);

        firstPin = (EditTextPIN) v.findViewById(R.id.pin_first_login);
        firstPin.setOnLongClickListener(this);
        firstPin.setOnFocusChangeListener(this);
        imm.showSoftInput(firstPin, InputMethodManager.SHOW_FORCED);
        firstPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(firstPin.length() != 0){
                    secondPin.requestFocus();
                    secondPin.setCursorVisible(true);

                    if (isFirstTime && !pinLongClick){
                        secondPin.setText("");
                        thirdPin.setText("");
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
                    }
                    else if (pinLongClick) {
                        pasteClipboard();
                    }
                    else  {
                        permitVerify();
                    }
                }
                else {
                    if (isErrorShown){
                        verifyQuitError();
                    }
                }
            }
        });

        secondPin = (EditTextPIN) v.findViewById(R.id.pin_second_login);
        secondPin.setOnLongClickListener(this);
        secondPin.setOnFocusChangeListener(this);
        imm.showSoftInput(secondPin, InputMethodManager.SHOW_FORCED);
        secondPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (secondPin.length() != 0){
                    thirdPin.requestFocus();
                    thirdPin.setCursorVisible(true);

                    if (isFirstTime && !pinLongClick) {
                        thirdPin.setText("");
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
                    }
                    else if (pinLongClick) {
                        pasteClipboard();
                    }
                    else  {
                        permitVerify();
                    }
                }
                else {
                    if (isErrorShown){
                        verifyQuitError();
                    }
                }
            }
        });

        thirdPin = (EditTextPIN) v.findViewById(R.id.pin_third_login);
        thirdPin.setOnLongClickListener(this);
        thirdPin.setOnFocusChangeListener(this);
        imm.showSoftInput(thirdPin, InputMethodManager.SHOW_FORCED);
        thirdPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (thirdPin.length()!= 0){
                    fourthPin.requestFocus();
                    fourthPin.setCursorVisible(true);

                    if (isFirstTime && !pinLongClick) {
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
                    }
                    else if (pinLongClick) {
                        pasteClipboard();
                    }
                    else  {
                        permitVerify();
                    }
                }
                else {
                    if (isErrorShown){
                        verifyQuitError();
                    }
                }
            }
        });

        fourthPin = (EditTextPIN) v.findViewById(R.id.pin_fouth_login);
        fourthPin.setOnLongClickListener(this);
        fourthPin.setOnFocusChangeListener(this);
        imm.showSoftInput(fourthPin, InputMethodManager.SHOW_FORCED);
        fourthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (fourthPin.length()!=0){
                    fifthPin.requestFocus();
                    fifthPin.setCursorVisible(true);

                    if (isFirstTime && !pinLongClick) {
                        fifthPin.setText("");
                        sixthPin.setText("");
                    }
                    else if (pinLongClick) {
                        pasteClipboard();
                    }
                    else  {
                        permitVerify();
                    }
                }
                else {
                    if (isErrorShown){
                        verifyQuitError();
                    }
                }
            }
        });

        fifthPin = (EditTextPIN) v.findViewById(R.id.pin_fifth_login);
        fifthPin.setOnLongClickListener(this);
        fifthPin.setOnFocusChangeListener(this);
        imm.showSoftInput(fifthPin, InputMethodManager.SHOW_FORCED);
        fifthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (fifthPin.length()!=0){
                    sixthPin.requestFocus();
                    sixthPin.setCursorVisible(true);

                    if (isFirstTime && !pinLongClick) {
                        sixthPin.setText("");
                    }
                    else if (pinLongClick) {
                        pasteClipboard();
                    }
                    else  {
                        permitVerify();
                    }
                }
                else {
                    if (isErrorShown){
                        verifyQuitError();
                    }
                }
            }
        });

        sixthPin = (EditTextPIN) v.findViewById(R.id.pin_sixth_login);
        sixthPin.setOnLongClickListener(this);
        sixthPin.setOnFocusChangeListener(this);
        imm.showSoftInput(sixthPin, InputMethodManager.SHOW_FORCED);
        sixthPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (sixthPin.length()!=0){
                    sixthPin.setCursorVisible(true);
                    hideKeyboard((LoginActivityLollipop)context, 0);

                    if (pinLongClick) {
                        pasteClipboard();
                    }
                    else {
                        permitVerify();
                    }
                }
                else {
                    if (isErrorShown){
                        verifyQuitError();
                    }
                }
            }
        });
        ((LoginActivityLollipop) context).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        firstPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb1 = firstPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb1.width = scaleWidthPx(42, outMetrics);
        }
        else {
            paramsb1.width = scaleWidthPx(25, outMetrics);
        }
        firstPin.setLayoutParams(paramsb1);
        LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)firstPin.getLayoutParams();
        textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
        firstPin.setLayoutParams(textParams);

        secondPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb2 = secondPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb2.width = scaleWidthPx(42, outMetrics);
        }
        else {
            paramsb2.width = scaleWidthPx(25, outMetrics);
        }
        secondPin.setLayoutParams(paramsb2);
        textParams = (LinearLayout.LayoutParams)secondPin.getLayoutParams();
        textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
        secondPin.setLayoutParams(textParams);
        secondPin.setEt(firstPin);

        thirdPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb3 = thirdPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb3.width = scaleWidthPx(42, outMetrics);
        }
        else {
            paramsb3.width = scaleWidthPx(25, outMetrics);
        }
        thirdPin.setLayoutParams(paramsb3);
        textParams = (LinearLayout.LayoutParams)thirdPin.getLayoutParams();
        textParams.setMargins(0, 0, scaleWidthPx(25, outMetrics), 0);
        thirdPin.setLayoutParams(textParams);
        thirdPin.setEt(secondPin);

        fourthPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb4 = fourthPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb4.width = scaleWidthPx(42, outMetrics);
        }
        else {
            paramsb4.width = scaleWidthPx(25, outMetrics);
        }
        fourthPin.setLayoutParams(paramsb4);
        textParams = (LinearLayout.LayoutParams)fourthPin.getLayoutParams();
        textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
        fourthPin.setLayoutParams(textParams);
        fourthPin.setEt(thirdPin);

        fifthPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb5 = fifthPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb5.width = scaleWidthPx(42, outMetrics);
        }
        else {
            paramsb5.width = scaleWidthPx(25, outMetrics);
        }
        fifthPin.setLayoutParams(paramsb5);
        textParams = (LinearLayout.LayoutParams)fifthPin.getLayoutParams();
        textParams.setMargins(0, 0, scaleWidthPx(8, outMetrics), 0);
        fifthPin.setLayoutParams(textParams);
        fifthPin.setEt(fourthPin);

        sixthPin.setGravity(Gravity.CENTER_HORIZONTAL);
        android.view.ViewGroup.LayoutParams paramsb6 = sixthPin.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            paramsb6.width = scaleWidthPx(42, outMetrics);
        }
        else {
            paramsb6.width = scaleWidthPx(25, outMetrics);
        }
        sixthPin.setLayoutParams(paramsb6);
        textParams = (LinearLayout.LayoutParams)sixthPin.getLayoutParams();
        textParams.setMargins(0, 0, 0, 0);
        sixthPin.setLayoutParams(textParams);
        sixthPin.setEt(fifthPin);

        intentReceived = ((LoginActivityLollipop) context).getIntent();
        if(intentReceived!=null){
            action = intentReceived.getAction();
            if(action!=null) {
                logDebug("action is: " + action);
                if (ACTION_CONFIRM.equals(action)) {
                    handleConfirmationIntent(intentReceived);
                    return v;
                } else if (action.equals(ACTION_RESET_PASS)) {
                    String link = intentReceived.getDataString();
                    if (link != null) {
                        logDebug("Link to resetPass: " + link);
                        showDialogInsertMKToChangePass(link);
                        return v;
                    }
                } else if (action.equals(ACTION_PASS_CHANGED)) {
                    int result = intentReceived.getIntExtra(RESULT, MegaError.API_OK);
                    switch (result) {
                        case MegaError.API_OK:
                            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.pass_changed_alert));
                            break;

                        case MegaError.API_EKEY:
                            ((LoginActivityLollipop)context).showAlertIncorrectRK();
                            break;

                        case MegaError.API_EBLOCKED:
                            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_reset_account_blocked));
                            break;

                        default:
                            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.general_text_error));
                    }

                    return v;
                } else if (action.equals(ACTION_PARK_ACCOUNT)) {
                    String link = intentReceived.getDataString();
                    if (link != null) {
                        logDebug("Link to parkAccount: " + link);
                        showConfirmationParkAccount(link);
                        return v;
                    } else {
                        logError("Error when parking account - show error message");
                        showAlert(context, getString(R.string.general_text_error), getString(R.string.general_error_word));
                        return v;
                    }
                }
                else if (action.equals(ACTION_CANCEL_DOWNLOAD)) {
                    ((LoginActivityLollipop)context).showConfirmationCancelAllTransfers();

                } else if (action.equals(ACTION_SHOW_WARNING_ACCOUNT_BLOCKED)) {
                    String accountBlockedString = intentReceived.getStringExtra(ACCOUNT_BLOCKED_STRING);
                    if (!isTextEmpty(accountBlockedString)) {
                        showErrorAlertDialog(accountBlockedString, false, getActivity());
                    }
                }
            }
            else{
                logWarning("ACTION NULL");
            }
        }
        else{
            logWarning("No INTENT");
        }

        logDebug("et_user.getText(): " + et_user.getText());
        if (credentials != null && !((LoginActivityLollipop) context).isBackFromLoginPage){
            logDebug("Credentials NOT null");
            if ((intentReceived != null) && (action != null)){
                if (action.equals(ACTION_REFRESH)){
                    MegaApplication.setLoggingIn(true);
                    parentHandle = intentReceived.getLongExtra("PARENT_HANDLE", -1);
                    startLoginInProcess();
                    return v;
                }
                else if (action.equals(ACTION_REFRESH_STAGING)){
                    twoFA = true;
                    parentHandle = intentReceived.getLongExtra("PARENT_HANDLE", -1);
                    startFastLogin();
                    return v;
                } else if (action.equals(ACTION_REFRESH_AFTER_BLOCKED)) {
                    startFastLogin();
                    return v;
                } else {
                    if(action.equals(ACTION_OPEN_MEGA_FOLDER_LINK)){
                        url = intentReceived.getDataString();
                    }
                    else if(action.equals(ACTION_IMPORT_LINK_FETCH_NODES)){
                        url = intentReceived.getDataString();
                    }
                    else if(action.equals(ACTION_CHANGE_MAIL)){
                        logDebug("intent received ACTION_CHANGE_MAIL");
                        url = intentReceived.getDataString();
                    }
                    else if(action.equals(ACTION_CANCEL_ACCOUNT)){
                        logDebug("intent received ACTION_CANCEL_ACCOUNT");
                        url = intentReceived.getDataString();
                    }
                    else if (action.equals(ACTION_FILE_PROVIDER)){
                        uriData = intentReceived.getData();
                        extras = intentReceived.getExtras();
                        url = null;
                    }
                    else if(action.equals(ACTION_OPEN_HANDLE_NODE)){
                        url = intentReceived.getDataString();
                    }
                    else if(action.equals(ACTION_OPEN_FILE_LINK_ROOTNODES_NULL)){
                        uriData = intentReceived.getData();
                    }
                    else if(action.equals(ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL)){
                        uriData = intentReceived.getData();
                    }
                    else if(action.equals(ACTION_OPEN_CHAT_LINK)) {
                        url = intentReceived.getDataString();
                    }
                    else if (action.equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
                        url = intentReceived.getDataString();
                        idChatToJoin = intentReceived.getLongExtra("idChatToJoin", -1);
                    }

                    MegaNode rootNode = megaApi.getRootNode();
                    if (rootNode != null){
                        Intent intent = new Intent(context, ManagerActivityLollipop.class);
                        if (action != null){
//							if (action.equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//								intent = new Intent(this, FileExplorerActivityLollipop.class);
//								if(extras != null)
//								{
//									intent.putExtras(extras);
//								}
//								intent.setData(uriData);
//							}
                            if (action.equals(ACTION_FILE_PROVIDER)){
                                intent = new Intent(context, FileProviderActivity.class);
                                if(extras != null)
                                {
                                    intent.putExtras(extras);
                                }
                                intent.setData(uriData);
                            }
                            else if (action.equals(ACTION_OPEN_FILE_LINK_ROOTNODES_NULL)){
                                intent = new Intent(context, FileLinkActivityLollipop.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                action = ACTION_OPEN_MEGA_LINK;
                                intent.setData(uriData);
                            }
                            else if (action.equals(ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL)){
                                intent = new Intent(context, FolderLinkActivityLollipop.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                action = ACTION_OPEN_MEGA_FOLDER_LINK;
                                intent.setData(uriData);
                            }
                            else  if (action.equals(ACTION_OPEN_CONTACTS_SECTION)){
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                action = ACTION_OPEN_CONTACTS_SECTION;
                                if(intentReceived.getLongExtra(CONTACT_HANDLE, -1) != -1){
                                    intent.putExtra(CONTACT_HANDLE, intentReceived.getLongExtra(CONTACT_HANDLE, -1));
                                }
                            }

                            intent.setAction(action);
                            if (url != null){
                                intent.setData(Uri.parse(url));
                            }
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            ((LoginActivityLollipop) context).startCameraUploadService(false, 5 * 60 * 1000);
                        }

                        logDebug("Empty completed transfers data");
                        dbH.emptyCompletedTransfers();

                        this.startActivity(intent);
                        ((LoginActivityLollipop) context).finish();
                    }
                    else{
                        startFastLogin();
                        return v;
                    }
                }
            }
            else{
                MegaNode rootNode = megaApi.getRootNode();
                if (rootNode != null && !((LoginActivityLollipop)context).isFetchingNodes){

                    logDebug("rootNode != null");
                    Intent intent = new Intent(context, ManagerActivityLollipop.class);
                    if (action != null){

                        if (action.equals(ACTION_FILE_PROVIDER)){
                            intent = new Intent(context, FileProviderActivity.class);
                            if(extras != null)
                            {
                                intent.putExtras(extras);
                            }
                            intent.setData(uriData);
                        }
                        else if (action.equals(ACTION_OPEN_FILE_LINK_ROOTNODES_NULL)){
                            intent = new Intent(context, FileLinkActivityLollipop.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            action = ACTION_OPEN_MEGA_LINK;
                            intent.setData(uriData);
                        }
                        else if (action.equals(ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL)){
                            intent = new Intent(context, FolderLinkActivityLollipop.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            action = ACTION_OPEN_MEGA_FOLDER_LINK;
                            intent.setData(uriData);
                        }
                        else if (action.equals(ACTION_OPEN_CONTACTS_SECTION)){
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            if(intentReceived.getLongExtra(CONTACT_HANDLE, -1) != -1){
                                intent.putExtra(CONTACT_HANDLE, intentReceived.getLongExtra(CONTACT_HANDLE, -1));
                            }
                        }
                        intent.setAction(action);
                        if (url != null){
                            intent.setData(Uri.parse(url));
                        }
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    MegaPreferences prefs = dbH.getPreferences();
                    if(prefs!=null)
                    {
                        if (prefs.getCamSyncEnabled() != null){
                            if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                    ((LoginActivityLollipop) context).startCameraUploadService(false, 30 * 1000);
                                }
                            }
                        }
                    }

                    logDebug("Empty completed transfers data");
                    dbH.emptyCompletedTransfers();
                    this.startActivity(intent);
                    ((LoginActivityLollipop) context).finish();
                }
                else{
                    logWarning("rootNode == null");
                    startFastLogin();
                    return v;
                }
            }
        }
        else {
            logDebug("Credentials IS NULL");
            if ((intentReceived != null)) {
                logDebug("INTENT NOT NULL");
                if (action != null) {
                    logDebug("ACTION NOT NULL");
                    Intent intent;
                    if (action.equals(ACTION_FILE_PROVIDER)) {
                        intent = new Intent(context, FileProviderActivity.class);
                        if (extras != null) {
                            intent.putExtras(extras);
                        }
                        intent.setData(uriData);
                        intent.setAction(action);
                    }
                    else if (action.equals(ACTION_FILE_EXPLORER_UPLOAD)) {
                        ((LoginActivityLollipop)context).showSnackbar(getString(R.string.login_before_share));
                    }
                    else if (action.equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
                        url = intentReceived.getDataString();
                        idChatToJoin = intentReceived.getLongExtra("idChatToJoin", -1);
                    }
                }
            }
        }

        if ((passwdTemp != null) && (emailTemp != null)){
            submitForm(true);
        }

        logDebug("END onCreateView");
        return v;
    }

    void returnToLogin() {
        ((LoginActivityLollipop) context).hideAB();

        loginVerificationLayout.setVisibility(View.GONE);

        loginLoggingIn.setVisibility(View.GONE);
        loginLogin.setVisibility(View.VISIBLE);
        closeCancelDialog();
        scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
        loginCreateAccount.setVisibility(View.VISIBLE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        generatingKeysText.setVisibility(View.GONE);
        loggingInText.setVisibility(View.GONE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.pin_first_login:{
                if (hasFocus) {
                    firstPin.setText("");
                }
                break;
            }
            case R.id.pin_second_login:{
                if (hasFocus) {
                    secondPin.setText("");
                }
                break;
            }
            case R.id.pin_third_login:{
                if (hasFocus) {
                    thirdPin.setText("");
                }
                break;
            }
            case R.id.pin_fouth_login:{
                if (hasFocus) {
                    fourthPin.setText("");
                }
                break;
            }
            case R.id.pin_fifth_login:{
                if (hasFocus) {
                    fifthPin.setText("");
                }
                break;
            }
            case R.id.pin_sixth_login:{
                if (hasFocus) {
                    sixthPin.setText("");
                }
                break;
            }
        }
    }

    void verifyQuitError(){
        isErrorShown = false;
        pinError.setVisibility(View.GONE);
        firstPin.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
        secondPin.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
        thirdPin.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
        fourthPin.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
        fifthPin.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
        sixthPin.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
    }

    void verifyShowError(){
        isFirstTime = false;
        isErrorShown = true;
        pinError.setVisibility(View.VISIBLE);
        closeCancelDialog();
        firstPin.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
        secondPin.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
        thirdPin.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
        fourthPin.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
        fifthPin.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
        sixthPin.setTextColor(ContextCompat.getColor(context, R.color.login_warning));
    }

    void permitVerify(){
        logDebug("permitVerify");
        if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
            hideKeyboard((LoginActivityLollipop)context, 0);
            if (sb.length()>0) {
                sb.delete(0, sb.length());
            }
            sb.append(firstPin.getText());
            sb.append(secondPin.getText());
            sb.append(thirdPin.getText());
            sb.append(fourthPin.getText());
            sb.append(fifthPin.getText());
            sb.append(sixthPin.getText());
            pin = sb.toString();

            if (!isErrorShown && pin != null){
                logDebug("Login with factor login");
                verify2faProgressBar.setVisibility(View.VISIBLE);
                MegaApplication.setLoggingIn(true);
                megaApi.multiFactorAuthLogin(lastEmail, lastPassword, pin, this);
            }
        }
    }

    public void startLoginInProcess(){
        logDebug("startLoginInProcess");

        UserCredentials credentials = dbH.getCredentials();
        lastEmail = credentials.getEmail();
		gSession = credentials.getSession();

        loginLogin.setVisibility(View.GONE);
        loginCreateAccount.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
//					generatingKeysText.setVisibility(View.VISIBLE);
//					megaApi.fastLogin(gSession, this);

        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        loggingInText.setVisibility(View.VISIBLE);
        fetchingNodesText.setVisibility(View.VISIBLE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
        megaApi.fetchNodes(this);
    }

    public void startFastLogin(){
        logDebug("startFastLogin");
        UserCredentials credentials = dbH.getCredentials();
        lastEmail = credentials.getEmail();
        gSession = credentials.getSession();

        loginLogin.setVisibility(View.GONE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        loginCreateAccount.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
//						generatingKeysText.setVisibility(View.VISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        loggingInText.setVisibility(View.VISIBLE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
        resumeSesion = true;

        if (!MegaApplication.isLoggingIn()){

            MegaApplication.setLoggingIn(true);

            if (megaChatApi == null) {
                megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
            }

            int ret = megaChatApi.getInitState();
            if (ret == MegaChatApi.INIT_NOT_DONE || ret == MegaChatApi.INIT_ERROR) {
                logDebug("INIT STATE: " + ret);

                ret = megaChatApi.init(gSession);

                logDebug("result of init ---> " + ret);
                chatSettings = dbH.getChatSettings();
                if (ret == MegaChatApi.INIT_NO_CACHE) {
                    logDebug("condition ret == MegaChatApi.INIT_NO_CACHE");
                } else if (ret == MegaChatApi.INIT_ERROR) {
                    // chat cannot initialize, disable chat completely
                    logDebug("condition ret == MegaChatApi.INIT_ERROR");
                    megaChatApi.logout(new ChatLogoutListener(getContext()));
                } else {
                    logDebug("condition ret == OK -- chat correctly initialized");
                }
                logDebug("After init: " + ret);
            } else {
                logDebug("Do not init, chat already initialized: " + ret);
            }

            disableLoginButton();
            megaApi.fastLogin(gSession, this);
            if (intentReceived != null && intentReceived.getAction() != null && intentReceived.getAction().equals(ACTION_REFRESH_STAGING))  {
                logDebug("megaChatApi.refreshUrl()");
                megaChatApi.refreshUrl();
            }
        }
        else{
            logWarning("Another login is proccessing");
        }
    }

    private void submitForm(boolean fromConfirmAccount) {
        logDebug("fromConfirmAccount - " + fromConfirmAccount + " email: " + this.emailTemp + "__" + this.passwdTemp);

        lastEmail = this.emailTemp;
        lastPassword = this.passwdTemp;

        imm.hideSoftInputFromWindow(et_user.getWindowToken(), 0);

        if(!isOnline(context))
        {
            loginLoggingIn.setVisibility(View.GONE);
            loginLogin.setVisibility(View.VISIBLE);
            closeCancelDialog();
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
            loginCreateAccount.setVisibility(View.VISIBLE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.GONE);
            generatingKeysText.setVisibility(View.GONE);
            loggingInText.setVisibility(View.GONE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        loginLogin.setVisibility(View.GONE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        loginCreateAccount.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        generatingKeysText.setVisibility(View.VISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);

        logDebug("Generating keys");

        onKeysGenerated(lastEmail, lastPassword);
    }

    private void submitForm() {
        if (!validateForm()) {
            return;
        }

        performLogin();
    }

    private void performLogin() {

        imm.hideSoftInputFromWindow(et_user.getWindowToken(), 0);

        if(!isOnline(context))
        {
            loginLoggingIn.setVisibility(View.GONE);
            loginLogin.setVisibility(View.VISIBLE);
            closeCancelDialog();
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
            loginCreateAccount.setVisibility(View.VISIBLE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.GONE);
            generatingKeysText.setVisibility(View.GONE);
            loggingInText.setVisibility(View.GONE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }


        loginLogin.setVisibility(View.GONE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
        loginCreateAccount.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        generatingKeysText.setVisibility(View.VISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);

        lastEmail = et_user.getText().toString().toLowerCase(Locale.ENGLISH).trim();
        lastPassword = et_password.getText().toString();

        logDebug("Generating keys");

        onKeysGenerated(lastEmail, lastPassword);
    }

    private void onKeysGenerated(String email, String password) {
        logDebug("onKeysGenerated");

        this.lastEmail = email;
        this.lastPassword = password;

        if (confirmLink == null) {
            onKeysGeneratedLogin(email, password);
        }
        else{
            if(!isOnline(context)){
                ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
                return;
            }

            loginLogin.setVisibility(View.GONE);
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            loginCreateAccount.setVisibility(View.GONE);
            loginLoggingIn.setVisibility(View.VISIBLE);
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            generatingKeysText.setVisibility(View.VISIBLE);
            loginProgressBar.setVisibility(View.VISIBLE);
            loginFetchNodesProgressBar.setVisibility(View.GONE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.VISIBLE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            logDebug("fastConfirm");
            megaApi.confirmAccount(confirmLink, lastPassword, this);
        }
    }

    public void backToLoginForm() {
        //return to login form page
        loginLogin.setVisibility(View.VISIBLE);
        closeCancelDialog();
        loginCreateAccount.setVisibility(View.VISIBLE);
        loginLoggingIn.setVisibility(View.GONE);
        generatingKeysText.setVisibility(View.GONE);
        loginProgressBar.setVisibility(View.GONE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);

        queryingSignupLinkText.setVisibility(View.VISIBLE);
        confirmingAccountText.setVisibility(View.GONE);
        loggingInText.setVisibility(View.VISIBLE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
        resumeSesion = false;

        //reset 2fa page
        loginVerificationLayout.setVisibility(View.GONE);
        verify2faProgressBar.setVisibility(View.GONE);
        firstPin.setText("");
        secondPin.setText("");
        thirdPin.setText("");
        fourthPin.setText("");
        fifthPin.setText("");
        sixthPin.setText("");

        et_user.requestFocus();
    }

    private void onKeysGeneratedLogin(final String email, final String password) {
        logDebug("onKeysGeneratedLogin");

        if(!isOnline(context)){
            loginLoggingIn.setVisibility(View.GONE);
            loginLogin.setVisibility(View.VISIBLE);
            closeCancelDialog();
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
            loginCreateAccount.setVisibility(View.VISIBLE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.GONE);
            generatingKeysText.setVisibility(View.GONE);
            loggingInText.setVisibility(View.GONE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        if (!MegaApplication.isLoggingIn()) {
            MegaApplication.setLoggingIn(true);

            loggingInText.setVisibility(View.VISIBLE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            logDebug("fastLogin with publicKey & privateKey");
            resumeSesion = false;

            if (megaChatApi == null) {
                megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
            }
            int ret = megaChatApi.init(null);
            logDebug("result of init ---> " + ret);
            if (ret == MegaChatApi.INIT_WAITING_NEW_SESSION) {
                logDebug("condition ret == MegaChatApi.INIT_WAITING_NEW_SESSION");
                disableLoginButton();
                megaApi.login(lastEmail, lastPassword, this);
            } else {
                logWarning("ERROR INIT CHAT: " + ret);
                megaChatApi.logout(new ChatLogoutListener(getContext()));

                disableLoginButton();
                megaApi.login(lastEmail, lastPassword, this);
            }
        }
    }

    /*
     * Validate email and password
     */
    private boolean validateForm() {
        String emailError = getEmailError();
        String passwordError = getPasswordError();

        setError(et_user, emailError);
        setError(et_password, passwordError);

        if (emailError != null) {
            et_user.requestFocus();
            return false;
        } else if (passwordError != null) {
            et_password.requestFocus();
            return false;
        } else if (existOngoingTransfers(megaApi)) {
            showCancelTransfersDialog();
            return false;
        }
        return true;
    }

    private void disableLoginButton() {
        logDebug("Disable login button");
        //disbale login button
        bLogin.setBackground(context.getDrawable(R.drawable.background_button_disable));
        bLogin.setEnabled(false);
        //display login info
        loginInProgressPb.setVisibility(View.VISIBLE);
        loginInProgressInfo.setVisibility(View.VISIBLE);
        loginInProgressInfo.setText(R.string.login_in_progress);
    }

    private void enableLoginButton() {
        logDebug("Enable login button");
        bLogin.setEnabled(true);
        bLogin.setBackground(context.getDrawable(R.drawable.background_accent_button));
        loginInProgressPb.setVisibility(View.GONE);
        loginInProgressInfo.setVisibility(View.GONE);
    }

    public void onLoginClick(View v){
        submitForm();
    }

    public void onRegisterClick(View v){
        //Change fragmentVisible in the activity
        ((LoginActivityLollipop)context).showFragment(CREATE_ACCOUNT_FRAGMENT);
    }

    /*
     * Validate email
     */
    private String getEmailError() {
        String value = et_user.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_email);
        }
        if (!EMAIL_ADDRESS.matcher(value).matches()) {
            return getString(R.string.error_invalid_email);
        }
        return null;
    }

    /*
     * Validate password
     */
    private String getPasswordError() {
        String value = et_password.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_password);
        }
        return null;
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.button_login_login: {
                logDebug("Click on button_login_login");
                loginClicked = true;
                backWhileLogin = false;
                onLoginClick(v);
                break;
            }
            case R.id.button_create_account_login:{
                logDebug("Click on button_create_account_login");
                onRegisterClick(v);
                break;
            }
            case R.id.park_account_button:{
                logDebug("Click to park account");
                showDialogInsertMail(false);
                break;
            }
            case R.id.button_forgot_pass:{
                logDebug("Click on button_forgot_pass");
                try {
                    String url = "https://mega.nz/recovery";
                    Intent openTermsIntent = new Intent(context, WebViewActivityLollipop.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(url));
                    startActivity(openTermsIntent);
                }
                catch (Exception e){
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse("https://mega.nz/recovery"));
                    startActivity(viewIntent);
                }
                break;
            }
            case R.id.yes_MK_button:{
                logDebug("Click on yes_MK_button");
                showDialogInsertMail(true);
                break;
            }
            case R.id.no_MK_button:{
                logDebug("Click on no_MK_button");
                showParkAccountLayout();
                break;
            }

            case R.id.login_text_view:
                numberOfClicksKarere++;
                if (numberOfClicksKarere == 5){
                    MegaAttributes attrs = dbH.getAttributes();
                    if (attrs != null && attrs.getFileLoggerKarere() != null) {
                        if (Boolean.parseBoolean(attrs.getFileLoggerKarere())) {
                            numberOfClicksKarere = 0;
                            setStatusLoggerKarere(context, false);
                            break;
                        }
                    } else {
                        logWarning("Karere file logger attribute is NULL");
                    }
                    ((LoginActivityLollipop) context).showConfirmationEnableLogsKarere();
                }
                break;

            case R.id.text_newToMega:
                numberOfClicksSDK++;
                if (numberOfClicksSDK == 5) {
                    MegaAttributes attrs = dbH.getAttributes();
                    if (attrs != null && attrs.getFileLoggerSDK() != null) {
                        if (Boolean.parseBoolean(attrs.getFileLoggerSDK())) {
                            numberOfClicksSDK = 0;
                            setStatusLoggerSDK(context, false);
                            break;
                        }
                    } else {
                        logWarning("SDK file logger attribute is NULL");
                    }
                    ((LoginActivityLollipop) context).showConfirmationEnableLogsSDK();
                }
                break;

            case R.id.lost_authentication_device: {
                try {
                    String url = "https://mega.nz/recovery";
                    Intent openTermsIntent = new Intent(context, WebViewActivityLollipop.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(Uri.parse(url));
                    startActivity(openTermsIntent);
                }
                catch (Exception e){
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setData(Uri.parse("https://mega.nz/recovery"));
                    startActivity(viewIntent);
                }
                break;
            }
        }
    }


    public void showForgotPassLayout(){
        logDebug("showForgotPassLayout");
        loginLoggingIn.setVisibility(View.GONE);
        loginLogin.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
    }

    public void hideForgotPassLayout(){
        logDebug("hideForgotPassLayout");
        loginLoggingIn.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.GONE);
        loginLogin.setVisibility(View.VISIBLE);
        closeCancelDialog();
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
    }

    public void showParkAccountLayout(){
        logDebug("showParkAccountLayout");
        loginLoggingIn.setVisibility(View.GONE);
        loginLogin.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
    }

    public void hideParkAccountLayout(){
        logDebug("hideParkAccountLayout");
        loginLoggingIn.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.GONE);
        loginLogin.setVisibility(View.VISIBLE);
        closeCancelDialog();
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
    }

    /*
     * Get email address from confirmation code and set to emailView
     */
    private void updateConfirmEmail(String link) {
        if(!isOnline(context)){
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        loginLogin.setVisibility(View.GONE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        loginCreateAccount.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        generatingKeysText.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.VISIBLE);
        confirmingAccountText.setVisibility(View.GONE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
        loginProgressBar.setVisibility(View.VISIBLE);
        logDebug("querySignupLink");
        megaApi.querySignupLink(link, this);
    }

    /*
 * Handle intent from confirmation email
 */
    public void handleConfirmationIntent(Intent intent) {
        confirmLink = intent.getStringExtra(EXTRA_CONFIRMATION);
        loginTitle.setText(R.string.login_confirm_account);
        bLogin.setText(getString(R.string.login_confirm_account).toUpperCase(Locale.getDefault()));
        updateConfirmEmail(confirmLink);
    }

    @Override
    public void onAttach(Context context) {
        logDebug("onAttach");
        super.onAttach(context);
        this.context = context;

        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        logDebug("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null) {
            megaApi = ((MegaApplication) context.getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) context.getApplication()).getMegaChatApi();
        }
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

        try{
            if(timer!=null){
                timer.cancel();
                serversBusyText.setVisibility(View.GONE);
            }
        }
        catch(Exception e){
            logError("TIMER EXCEPTION", e);
        }
//		log("onRequestUpdate: " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
            if (firstRequestUpdate){
                loginProgressBar.setVisibility(View.GONE);
                firstRequestUpdate = false;
            }
            loginFetchNodesProgressBar.setVisibility(View.VISIBLE);
            loginFetchNodesProgressBar.getLayoutParams().width = px2dp((250*scaleW), outMetrics);
            if (request.getTotalBytes() > 0){
                double progressValue = 100.0 * request.getTransferredBytes() / request.getTotalBytes();
                if ((progressValue > 99) || (progressValue < 0)){
                    progressValue = 100;
                    prepareNodesText.setVisibility(View.VISIBLE);
                    loginProgressBar.setVisibility(View.VISIBLE);
                }
//				log("progressValue = " + (int)progressValue);
                loginFetchNodesProgressBar.setProgress((int)progressValue);
            }
        }
    }

    public void readyToManager(){
        closeCancelDialog();

        LoginActivityLollipop loginActivityLollipop = ((LoginActivityLollipop) context);

        if(confirmLink==null && !accountConfirmed){
            logDebug("confirmLink==null");

            logDebug("OK fetch nodes");
            logDebug("value of resumeSession: " + resumeSesion);

            if((action!=null)&&(url!=null)) {
                logDebug("Empty completed transfers data");
                dbH.emptyCompletedTransfers();

                if (action.equals(ACTION_CHANGE_MAIL)) {
                    logDebug("Action change mail after fetch nodes");
                    Intent changeMailIntent = new Intent(context, ManagerActivityLollipop.class);
                    changeMailIntent.setAction(ACTION_CHANGE_MAIL);
                    changeMailIntent.setData(Uri.parse(url));
                    loginActivityLollipop.startActivity(changeMailIntent);
                    loginActivityLollipop.finish();
                }
                else if(action.equals(ACTION_RESET_PASS)) {
                    logDebug("Action reset pass after fetch nodes");
                    Intent resetPassIntent = new Intent(context, ManagerActivityLollipop.class);
                    resetPassIntent.setAction(ACTION_RESET_PASS);
                    resetPassIntent.setData(Uri.parse(url));
                    loginActivityLollipop.startActivity(resetPassIntent);
                    loginActivityLollipop.finish();
                }
                else if(action.equals(ACTION_CANCEL_ACCOUNT)) {
                    logDebug("Action cancel Account after fetch nodes");
                    Intent cancelAccountIntent = new Intent(context, ManagerActivityLollipop.class);
                    cancelAccountIntent.setAction(ACTION_CANCEL_ACCOUNT);
                    cancelAccountIntent.setData(Uri.parse(url));
                    loginActivityLollipop.startActivity(cancelAccountIntent);
                    loginActivityLollipop.finish();
                }
            }

            if (!backWhileLogin){
                logDebug("NOT backWhileLogin");
                if (parentHandle != -1){
                    Intent intent = new Intent();
                    intent.putExtra("PARENT_HANDLE", parentHandle);
                    loginActivityLollipop.setResult(RESULT_OK, intent);
                    loginActivityLollipop.finish();
                }
                else{
                    Intent intent = null;
                    if (firstTime){
                        logDebug("First time");
                        intent = new Intent(context,ManagerActivityLollipop.class);
                        intent.putExtra("firstLogin", true);
                        if (action != null){
                            logDebug("Action not NULL");
                            if (action.equals(ACTION_EXPORT_MASTER_KEY)){
                                logDebug("ACTION_EXPORT_MK");
                                intent.setAction(action);
                            }
                            else if (action.equals(ACTION_JOIN_OPEN_CHAT_LINK) && url != null) {
                                intent.setAction(action);
                                intent.setData(Uri.parse(url));
                                if (idChatToJoin != -1) {
                                    intent.putExtra("idChatToJoin", idChatToJoin);
                                }
                            }
                        }
                    }
                    else{
                        boolean initialCam = false;
                        MegaPreferences prefs = dbH.getPreferences();
                        if (prefs != null){
                            if (prefs.getCamSyncEnabled() != null){
                                if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                        loginActivityLollipop.startCameraUploadService(false, 30 * 1000);
                                    }
                                }
                            }
                            else{
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                    loginActivityLollipop.startCameraUploadService(true, 30 * 1000);
                                }
                                initialCam = true;
                            }
                        }
                        else{
                            intent = new Intent(context,ManagerActivityLollipop.class);
                            intent.putExtra("firstLogin", true);
                            initialCam = true;
                        }

                        if (!initialCam){
                            logDebug("NOT initialCam");
                            intent = new Intent(context,ManagerActivityLollipop.class);
                            if (action != null){
                                logDebug("The action is: " + action);
//										if (action.equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//											intent = new Intent(this, FileExplorerActivityLollipop.class);
//											if(extras != null)
//											{
//												intent.putExtras(extras);
//											}
//											intent.setData(uriData);
//										}
                                if (action.equals(ACTION_FILE_PROVIDER)){
                                    intent = new Intent(context, FileProviderActivity.class);
                                    if(extras != null){
                                        intent.putExtras(extras);
                                    }
                                    if(uriData != null){
                                        intent.setData(uriData);
                                    }
                                }
                                else if (action.equals(ACTION_OPEN_FILE_LINK_ROOTNODES_NULL)){
                                    intent = new Intent(context, FileLinkActivityLollipop.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.setData(uriData);
                                }
                                else if (action.equals(ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL)){
                                    intent = new Intent(context, FolderLinkActivityLollipop.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    action = ACTION_OPEN_MEGA_FOLDER_LINK;
                                    intent.setData(uriData);
                                }
                                else if (action.equals(ACTION_OPEN_CONTACTS_SECTION)){
                                    intent.putExtra(CONTACT_HANDLE, intentReceived.getLongExtra(CONTACT_HANDLE, -1));
                                }
                                else if (action.equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
                                    if (idChatToJoin != -1) {
                                        intent.putExtra("idChatToJoin", idChatToJoin);
                                    }
                                }
                                intent.setAction(action);
                                if (url != null){
                                    intent.setData(Uri.parse(url));
                                }
                            }
                            else{
                                logWarning("The intent action is NULL");
                            }
                        }
                        else{
                            logDebug("initialCam YESSSS");
                            intent = new Intent(context,ManagerActivityLollipop.class);
                            if (action != null){
                                logDebug("The action is: " + action);
                                intent.setAction(action);
                            }
                            if (url != null){
                                intent.setData(Uri.parse(url));
                            }
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    }

                    logDebug("Empty completed transfers data");
                    dbH.emptyCompletedTransfers();

                    if (twoFA){
                        intent.setAction(ACTION_REFRESH_STAGING);
                        twoFA = false;
                    }
                    
                    if (action != null && action.equals(ACTION_REFRESH_AFTER_BLOCKED)) {
                        intent.setAction(ACTION_REFRESH_AFTER_BLOCKED);
                    }

                    loginActivityLollipop.startActivity(intent);
                    loginActivityLollipop.finish();
                }
            }
        }
        else{
            logDebug("Go to ChooseAccountFragment");
            accountConfirmed = false;
            loginActivityLollipop.showFragment(CHOOSE_ACCOUNT_FRAGMENT);
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request)
    {
        logDebug("onRequestStart: " + request.getRequestString());
        if(request.getType() == MegaRequest.TYPE_LOGIN) {
            disableLoginButton();
        }
        if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
//			loginProgressBar.setVisibility(View.GONE);
            loginFetchNodesProgressBar.setVisibility(View.VISIBLE);
            loginFetchNodesProgressBar.getLayoutParams().width = px2dp((250*scaleW), outMetrics);
            loginFetchNodesProgressBar.setProgress(0);
            LoginActivityLollipop.isFetchingNodes = true;
            disableLoginButton();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError error) {
        enableLoginButton();
        try{
            if(timer!=null){
                timer.cancel();
                serversBusyText.setVisibility(View.GONE);
            }
        }
        catch(Exception e){
            logError("TIMER EXCEPTION", e);
        }

        logDebug("onRequestFinish: " + request.getRequestString() + ",error code: " + error.getErrorCode());
        if (request.getType() == MegaRequest.TYPE_LOGIN){
            //cancel login process by press back.
            if(!MegaApplication.isLoggingIn()) {
                logWarning("Terminate login process when login");
                return;
            }
            if (error.getErrorCode() != MegaError.API_OK) {
                MegaApplication.setLoggingIn(false);
                if(confirmLogoutDialog != null) {
                    confirmLogoutDialog.dismiss();
                }
                enableLoginButton();
                String errorMessage = "";

                if (error.getErrorCode() == MegaError.API_ESID){
                    logWarning("MegaError.API_ESID " + getString(R.string.error_server_expired_session));
                    ((LoginActivityLollipop)context).showAlertLoggedOut();
                }
                else if (error.getErrorCode() == MegaError.API_EMFAREQUIRED){
                    logDebug("require 2fa");
                    is2FAEnabled = true;
                    ((LoginActivityLollipop) context).showAB(tB);
                    loginLogin.setVisibility(View.GONE);
                    scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                    loginCreateAccount.setVisibility(View.GONE);
                    loginLoggingIn.setVisibility(View.GONE);
                    generatingKeysText.setVisibility(View.GONE);
                    loginProgressBar.setVisibility(View.GONE);
                    loginFetchNodesProgressBar.setVisibility(View.GONE);
                    queryingSignupLinkText.setVisibility(View.GONE);
                    confirmingAccountText.setVisibility(View.GONE);
                    fetchingNodesText.setVisibility(View.GONE);
                    prepareNodesText.setVisibility(View.GONE);
                    serversBusyText.setVisibility(View.GONE);
                    loginVerificationLayout.setVisibility(View.VISIBLE);
                    closeCancelDialog();
                    firstPin.requestFocus();
                    firstPin.setCursorVisible(true);
                    return;
                }
                else if (error.getErrorCode() == MegaError.API_EFAILED || error.getErrorCode() == MegaError.API_EEXPIRED) {
                    if (verify2faProgressBar != null) {
                        verify2faProgressBar.setVisibility(View.GONE);
                    }
                    verifyShowError();
                    return;
                }
                else{
                    if (error.getErrorCode() == MegaError.API_ENOENT) {
                        errorMessage = getString(R.string.error_incorrect_email_or_password);
                    }
                    else if (error.getErrorCode() == MegaError.API_ETOOMANY){
                        errorMessage = getString(R.string.too_many_attempts_login);
                    }
                    else if (error.getErrorCode() == MegaError.API_EINCOMPLETE){
                        errorMessage = getString(R.string.account_not_validated_login);
                    }
                    else if(error.getErrorCode() == MegaError.API_EACCESS) {
                        errorMessage = error.getErrorString();
                    } else if (error.getErrorCode() == MegaError.API_EBLOCKED) {
                        //It will processed at the `onEvent` when receive an EVENT_ACCOUNT_BLOCKED
                        logWarning("Suspended account - Reason: " + request.getNumber());
                        return;
                    } else {
                        errorMessage = error.getErrorString();
                    }
                    logError("LOGIN_ERROR: "+error.getErrorCode()+ " "+error.getErrorString());

                    if (megaChatApi != null) {
                        megaChatApi.logout(new ChatLogoutListener(getContext()));
                    }

                    if(!errorMessage.isEmpty()){
                        if(!backWhileLogin) {
                            ((LoginActivityLollipop)context).showSnackbar(errorMessage);
                        }
                    }

                    if(chatSettings==null) {
                        logDebug("Reset chat setting enable");
                        chatSettings = new ChatSettings();
                        dbH.setChatSettings(chatSettings);
                    }
                }

                returnToLogin();
            } else {
                if (is2FAEnabled){
                    loginVerificationLayout.setVisibility(View.GONE);
                    ((LoginActivityLollipop) context).hideAB();
                }

                scrollView.setBackgroundColor(getResources().getColor(R.color.white));
                loginLogin.setVisibility(View.GONE);
                loginLoggingIn.setVisibility(View.VISIBLE);
                loginProgressBar.setVisibility(View.VISIBLE);
                loginFetchNodesProgressBar.setVisibility(View.GONE);
                loggingInText.setVisibility(View.VISIBLE);
                fetchingNodesText.setVisibility(View.VISIBLE);
                prepareNodesText.setVisibility(View.GONE);
                serversBusyText.setVisibility(View.GONE);
                saveCredentials();

                logDebug("Logged in with session");

                DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
                dbH.clearEphemeral();

                megaApi.fetchNodes(this);
            }
        } else if(request.getType() == MegaRequest.TYPE_LOGOUT) {
            logDebug("TYPE_LOGOUT");
            if (error.getErrorCode() == MegaError.API_OK){
                AccountController.localLogoutApp(context.getApplicationContext());
            }
        }
        else if(request.getType() == MegaRequest.TYPE_GET_RECOVERY_LINK){
            logDebug("TYPE_GET_RECOVERY_LINK");
            if (error.getErrorCode() == MegaError.API_OK){
                logDebug("The recovery link has been sent");
                showAlert(context, getString(R.string.email_verification_text), getString(R.string.email_verification_title));
            }
            else if (error.getErrorCode() == MegaError.API_ENOENT){
                logError("No account with this mail: "+error.getErrorString()+" "+error.getErrorCode());
                showAlert(context, getString(R.string.invalid_email_text), getString(R.string.invalid_email_title));
            }
            else{
                logError("Error when asking for recovery pass link");
                logError(error.getErrorString() + "___" + error.getErrorCode());
                showAlert(context,getString(R.string.general_text_error), getString(R.string.general_error_word));
            }
        }
        else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
            //cancel login process by press back.
            if(!MegaApplication.isLoggingIn()) {
                logDebug("Terminate login process when fetch nodes");
                return;
            }
            LoginActivityLollipop.isFetchingNodes = false;
            MegaApplication.setLoggingIn(false);

            if (error.getErrorCode() == MegaError.API_OK){
                receivedIntent = ((LoginActivityLollipop) context).getIntentReceived();
                if (receivedIntent != null) {
                    shareInfos = (ArrayList<ShareInfo>) receivedIntent.getSerializableExtra(FileExplorerActivityLollipop.EXTRA_SHARE_INFOS);
                    if (shareInfos != null && shareInfos.size() > 0) {
                        boolean canRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                        if (canRead) {
                            toSharePage();
                        } else {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_MEDIA_PERMISSION);
                        }
                        return;
                    }
                }

                readyToManager();
            } else {
                if(confirmLogoutDialog != null) {
                    confirmLogoutDialog.dismiss();
                }
                enableLoginButton();
                logError("Error fetch nodes: " + error.getErrorCode());
                String errorMessage;
                if (error.getErrorCode() == MegaError.API_ESID){
                    errorMessage = getString(R.string.error_server_expired_session);
                }
                else if (error.getErrorCode() == MegaError.API_ETOOMANY){
                    errorMessage = getString(R.string.too_many_attempts_login);
                }
                else if (error.getErrorCode() == MegaError.API_EINCOMPLETE){
                    errorMessage = getString(R.string.account_not_validated_login);
                }
                else{
                    errorMessage = error.getErrorString();
                }
                loginLoggingIn.setVisibility(View.GONE);
                loginLogin.setVisibility(View.VISIBLE);
                closeCancelDialog();
                scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
                loginCreateAccount.setVisibility(View.VISIBLE);
                generatingKeysText.setVisibility(View.GONE);
                loggingInText.setVisibility(View.GONE);
                fetchingNodesText.setVisibility(View.GONE);
                prepareNodesText.setVisibility(View.GONE);
                serversBusyText.setVisibility(View.GONE);
                queryingSignupLinkText.setVisibility(View.GONE);
                confirmingAccountText.setVisibility(View.GONE);

                if (error.getErrorCode() == MegaError.API_EACCESS){
                    logError("Error API_EACCESS");
                    if(((LoginActivityLollipop)context).accountBlocked!=null){
                        logError("Account blocked");
                    }
                    else{
                        errorMessage = error.getErrorString();
                        if(!backWhileLogin) {
                            ((LoginActivityLollipop)context).showSnackbar(errorMessage);
                        }
                    }
                }
                else{
                    if(error.getErrorCode() != MegaError.API_EBLOCKED) {
                        ((LoginActivityLollipop)context).showSnackbar(errorMessage);
                    }
                }

                if(chatSettings==null) {
                    logDebug("Reset chat setting enable");
                    chatSettings = new ChatSettings();
                    dbH.setChatSettings(chatSettings);
                }
            }
        }
        else if (request.getType() == MegaRequest.TYPE_QUERY_SIGNUP_LINK){
            logDebug("MegaRequest.TYPE_QUERY_SIGNUP_LINK");
            String s = "";
            loginLogin.setVisibility(View.VISIBLE);
            closeCancelDialog();
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
            bForgotPass.setVisibility(View.INVISIBLE);
            loginCreateAccount.setVisibility(View.VISIBLE);
            loginLoggingIn.setVisibility(View.GONE);
            generatingKeysText.setVisibility(View.GONE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.GONE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            if(error.getErrorCode() == MegaError.API_OK){
                logDebug("MegaRequest.TYPE_QUERY_SIGNUP_LINK MegaError API_OK");
                if (request.getFlag()) {
                    bForgotPass.setVisibility(View.VISIBLE);
                    loginProgressBar.setVisibility(View.GONE);

                    loginTitle.setText(R.string.login_to_mega);
                    bLogin.setText(getString(R.string.login_text).toUpperCase(Locale.getDefault()));
                    confirmLink = null;
                    ((LoginActivityLollipop)context).showSnackbar(getString(R.string.account_confirmed));
                    accountConfirmed = true;
                }
                else {
                    accountConfirmed = false;
                    ((LoginActivityLollipop)context).showSnackbar(getString(R.string.confirm_account));
                }
                s = request.getEmail();
                et_user.setText(s);
                et_password.requestFocus();
            }
            else{
                logWarning("MegaRequest.TYPE_QUERY_SIGNUP_LINK MegaError not API_OK " + error.getErrorCode());
                LoginActivityLollipop loginActivityLollipop = (LoginActivityLollipop) context;
                if (error.getErrorCode() == MegaError.API_ENOENT) {
                    loginActivityLollipop.showSnackbar(getString(R.string.reg_link_expired));
                } else {
                    loginActivityLollipop.showSnackbar(error.getErrorString());
                }
                confirmLink = null;
            }
        }
        else if (request.getType() == MegaRequest.TYPE_CONFIRM_ACCOUNT){
            if (error.getErrorCode() == MegaError.API_OK){
                logDebug("fastConfirm finished - OK");
                onKeysGeneratedLogin(lastEmail, lastPassword);
            }
            else{
                loginLogin.setVisibility(View.VISIBLE);
                closeCancelDialog();
                scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_create_account));
                loginCreateAccount.setVisibility(View.VISIBLE);
                loginLoggingIn.setVisibility(View.GONE);
                generatingKeysText.setVisibility(View.GONE);
                queryingSignupLinkText.setVisibility(View.GONE);
                confirmingAccountText.setVisibility(View.GONE);
                fetchingNodesText.setVisibility(View.GONE);
                prepareNodesText.setVisibility(View.GONE);
                serversBusyText.setVisibility(View.GONE);

                if (error.getErrorCode() == MegaError.API_ENOENT || error.getErrorCode() == MegaError.API_EKEY){
                    ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_incorrect_email_or_password));
                }
                else{
                    ((LoginActivityLollipop)context).showSnackbar(error.getErrorString());
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == READ_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toSharePage();
            } else {
                readyToManager();
            }
        }
    }

    private void toSharePage() {
        Intent shareIntent = new Intent(context, FileExplorerActivityLollipop.class);
        shareIntent.putExtra(FileExplorerActivityLollipop.EXTRA_SHARE_INFOS,shareInfos);
        shareIntent.setAction(receivedIntent.getStringExtra(FileExplorerActivityLollipop.EXTRA_SHARE_ACTION));
        shareIntent.setType(receivedIntent.getStringExtra(FileExplorerActivityLollipop.EXTRA_SHARE_TYPE));
        startActivity(shareIntent);
        ((LoginActivityLollipop) context).finish();
    }

    private void closeCancelDialog() {
        if (confirmLogoutDialog != null) {
            confirmLogoutDialog.dismiss();
        }
    }

    private void saveCredentials() {
        gSession = megaApi.dumpSession();
        MegaUser myUser = megaApi.getMyUser();
        String myUserHandle = "";
        if (myUser != null) {
            lastEmail = megaApi.getMyUser().getEmail();
            myUserHandle = megaApi.getMyUser().getHandle() + "";
        }

        UserCredentials credentials = new UserCredentials(lastEmail, gSession, "", "", myUserHandle);
        dbH.saveCredentials(credentials);
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
    {
        logWarning("onRequestTemporaryError: " + request.getRequestString() + e.getErrorCode());

//		if (request.getType() == MegaRequest.TYPE_LOGIN){
//
//		}
//		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
//
//		}
        final MegaError error = e;
        try{
            timer = new CountDownTimer(10000, 2000) {

                public void onTick(long millisUntilFinished) {
                    logDebug("TemporaryError one more");
                }

                public void onFinish() {
                    logDebug("The timer finished, message shown");
                    try {
                        serversBusyText.setVisibility(View.VISIBLE);
                        if(error.getErrorCode()==MegaError.API_EAGAIN){
                            logWarning("onRequestTemporaryError:onFinish:API_EAGAIN: :value: " + error.getValue());
                            if(error.getValue() == MegaApiJava.RETRY_CONNECTIVITY){
                                serversBusyText.setText(getString(R.string.login_connectivity_issues));
                                loginInProgressInfo.setText(getString(R.string.login_connectivity_issues));
                            }
                            else if(error.getValue() == MegaApiJava.RETRY_SERVERS_BUSY){
                                serversBusyText.setText(getString(R.string.login_servers_busy));
                                loginInProgressInfo.setText(getString(R.string.login_servers_busy));
                            }
                            else if(error.getValue() == MegaApiJava.RETRY_API_LOCK){
                                serversBusyText.setText(getString(R.string.login_API_lock));
                                loginInProgressInfo.setText(getString(R.string.login_API_lock));
                            }
                            else if(error.getValue() == MegaApiJava.RETRY_RATE_LIMIT){
                                serversBusyText.setText(getString(R.string.login_API_rate));
                                loginInProgressInfo.setText(getString(R.string.login_API_rate));
                            }
                            else{
                                serversBusyText.setText(getString(R.string.servers_busy));
                                loginInProgressInfo.setText(getString(R.string.servers_busy));
                            }
                        }
                        else{
                            serversBusyText.setText(getString(R.string.servers_busy));
                            loginInProgressInfo.setText(getString(R.string.servers_busy));
                        }
                    }
                    catch (Exception e){}

                }
            }.start();
        }catch (Exception exception){
            logError("EXCEPTION when starting count", exception);
        }
    }

    public void showDialogInsertMail(final boolean reset){
        logDebug("reset: " + reset);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

        final EditText input = new EditText(context);
        layout.addView(input, params);

        final RelativeLayout error_layout = new RelativeLayout(context);
        layout.addView(error_layout, params1);

        final ImageView error_icon = new ImageView(context);
        error_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_input_warning));
        error_layout.addView(error_icon);
        RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();


        params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        error_icon.setLayoutParams(params_icon);

        error_icon.setColorFilter(ContextCompat.getColor(context, R.color.login_warning));

        final TextView textError = new TextView(context);
        error_layout.addView(textError);
        RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
        params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
        params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0,0,0);
        textError.setLayoutParams(params_text_error);

        textError.setTextColor(ContextCompat.getColor(context, R.color.login_warning));

        error_layout.setVisibility(View.GONE);

//		input.setId(EDIT_TEXT_ID);
        input.getBackground().mutate().clearColorFilter();
        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(context, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(error_layout.getVisibility() == View.VISIBLE){
                    error_layout.setVisibility(View.GONE);
                    input.getBackground().mutate().clearColorFilter();
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(context, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });
        input.setSingleLine();
        input.setHint(getString(R.string.edit_text_insert_mail));
        input.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
//		input.setSelectAllOnFocus(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
                logDebug("OK RESET PASSWORD");
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String value = input.getText().toString().trim();
                    String emailError = Util.getEmailError(value, context);
                    if (emailError != null) {
                        logWarning("Mail incorrect");
//                        input.setError(emailError);
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(emailError);
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();
                    } else {
                        if(reset){
                            logDebug("Ask for link to reset pass");
                            megaApi.resetPassword(value, true, loginFragment);
                        }
                        else{
                            logDebug("Ask for link to park account");
                            megaApi.resetPassword(value, false, loginFragment);
                        }
                        insertMailDialog.dismiss();
                    }
                }
                else{
                    logDebug("Other IME" + actionId);
                }
                return false;
            }
        });
        input.setImeActionLabel(getString(R.string.general_add),EditorInfo.IME_ACTION_DONE);
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboardDelayed(v);
                }
                else{
                    hideKeyboardDelayed(v);
                }
            }
        });
        String title;
        String text;
        String buttonText;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        if(reset){
            title= getString(R.string.title_alert_reset_with_MK);
            text = getString(R.string.text_alert_reset_with_MK);
            buttonText=getString(R.string.context_send);
        }
        else{
            title= getString(R.string.park_account_dialog_title);
            text = getString(R.string.dialog_park_account);
            buttonText=getString(R.string.park_account_button);
        }
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setPositiveButton(buttonText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                hideKeyboard((LoginActivityLollipop)context, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                input.getBackground().mutate().clearColorFilter();
            }
        });
        builder.setView(layout);
        insertMailDialog = builder.create();
        insertMailDialog.show();
        insertMailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logDebug("OK BTTN PASSWORD");
                String value = input.getText().toString().trim();
                String emailError = Util.getEmailError(value, context);
                if (emailError != null) {
                    logWarning("Mail incorrect");
//                    input.setError(emailError);
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(context, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                    textError.setText(emailError);
                    error_layout.setVisibility(View.VISIBLE);
                    input.requestFocus();
                } else {
                    if(reset){
                        logDebug("Ask for link to reset pass");
                        megaApi.resetPassword(value, true, loginFragment);
                    }
                    else{
                        logDebug("Ask for link to park account");
                        megaApi.resetPassword(value, false, loginFragment);
                    }

                    insertMailDialog.dismiss();
                }
            }
        });
    }

    /*
     * Display keyboard
     */
    private void showKeyboardDelayed(final View view) {
        logDebug("showKeyboardDelayed");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    private void hideKeyboardDelayed(final View view) {
        logDebug("showKeyboardDelayed");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (imm.isActive()) {
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        }, 50);
    }

    public void showConfirmationParkAccount(String link){
        logDebug("link: " + link);

        final String linkUrl = link;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        logDebug("Call to Change Password Activity: " + linkUrl);
                        Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
                        intent.setAction(ACTION_RESET_PASS_FROM_PARK_ACCOUNT);
                        intent.setData(Uri.parse(linkUrl));
                        startActivity(intent);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getResources().getString(R.string.park_account_dialog_title));
        String message= getResources().getString(R.string.park_account_text_last_step);
        builder.setMessage(message).setPositiveButton(R.string.park_account_button, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showDialogInsertMKToChangePass(String link){
        logDebug("link: " + link);

        final String linkUrl = link;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

        final EditText input = new EditText(context);
        layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
        input.setSingleLine();
        input.setHint(getString(R.string.edit_text_insert_mk));
        input.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
//		input.setSelectAllOnFocus(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    logDebug("IME OK BTTN PASSWORD");
                    String value = input.getText().toString().trim();
                    if(value.equals("")||value.isEmpty()){
                        logWarning("Input is empty");
                        input.setError(getString(R.string.invalid_string));
                        input.requestFocus();
                    }
                    else {

                        logDebug("Positive button pressed - reset pass");
                        Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
                        intent.setAction(ACTION_RESET_PASS_FROM_LINK);
                        intent.setData(Uri.parse(linkUrl));
                        intent.putExtra("MK", value);
                        startActivity(intent);
                        insertMKDialog.dismiss();
                    }
                }
                else{
                    logDebug("Other IME" + actionId);
                }
                return false;
            }
        });
        input.setImeActionLabel(getString(R.string.general_add),EditorInfo.IME_ACTION_DONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getString(R.string.title_dialog_insert_MK));
        builder.setMessage(getString(R.string.text_dialog_insert_MK));
        builder.setPositiveButton(getString(R.string.general_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                hideKeyboard((LoginActivityLollipop)context, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setView(layout);
        insertMKDialog = builder.create();
        insertMKDialog.show();
        insertMKDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                logDebug("OK BTTN PASSWORD");
                String value = input.getText().toString().trim();
                if(value.equals("")||value.isEmpty()){
                    logWarning("Input is empty");
                    input.setError(getString(R.string.invalid_string));
                    input.requestFocus();
                }
                else {
                    logDebug("Positive button pressed - reset pass");
                    Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
                    intent.setAction(ACTION_RESET_PASS_FROM_LINK);
                    intent.setData(Uri.parse(linkUrl));
                    intent.putExtra("MK", value);
                    startActivity(intent);
                    insertMKDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if (megaApi != null) {
            megaApi.removeRequestListener(this);
        }

        if (megaChatApi != null) {
            megaChatApi.removeChatListener(this);
        }

        closeCancelDialog();
        super.onDestroy();
    }

    private AlertDialog confirmLogoutDialog;
    private void showConfirmLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog,int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        backToLoginForm();
                        backWhileLogin = true;
                        MegaApplication.setLoggingIn(false);
                        LoginActivityLollipop.isFetchingNodes = false;
                        loginClicked = false;
                        firstTime = true;
                        if (megaChatApi == null){
                            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
                        }
                        megaChatApi.logout(new ChatLogoutListener(getContext()));
                        megaApi.localLogout(LoginFragmentLollipop.this);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        String message= getString(R.string.confirm_cancel_login);
        confirmLogoutDialog =  builder.setCancelable(true)
                .setMessage(message)
                .setPositiveButton(getString(R.string.general_positive_button), dialogClickListener)
                .setNegativeButton(getString(R.string.general_negative_button), dialogClickListener)
                .show();
    }

    public int onBackPressed() {
        logDebug("onBackPressed");
        //refresh, point to staging server, enable chat. block the back button
        if (ACTION_REFRESH.equals(action) || ACTION_REFRESH_STAGING.equals(action)){
            return -1;
        }
        //login is in process
        boolean onLoginPage = loginLogin.getVisibility() == View.VISIBLE;
        boolean on2faPage = loginVerificationLayout.getVisibility() == View.VISIBLE;
        if ((MegaApplication.isLoggingIn() || LoginActivityLollipop.isFetchingNodes) && !onLoginPage && !on2faPage) {
            showConfirmLogoutDialog();
            return 2;
        }
        else{

            if(forgotPassLayout.getVisibility()==View.VISIBLE){
                logDebug("Forgot Pass layout is VISIBLE");
                hideForgotPassLayout();
                return 1;
            }
            if(on2faPage) {
                logDebug("Back from 2fa page");
                showConfirmLogoutDialog();
                return 2;
            }

            if(parkAccountLayout.getVisibility()==View.VISIBLE){
                logDebug("Park account layout is VISIBLE");
                hideParkAccountLayout();
                return 1;
            }

            ((LoginActivityLollipop) context).isBackFromLoginPage = true;
            ((LoginActivityLollipop) context).showFragment(TOUR_FRAGMENT);
            return 1;
        }
    }

    public void setPasswdTemp(String passwdTemp){
        this.passwdTemp = passwdTemp;
    }

    public String getPasswdTemp(){
        return this.passwdTemp;
    }

    public void setEmailTemp(String emailTemp){
        this.emailTemp = emailTemp;
    }

    public String getEmailTemp(){
        return this.emailTemp;
    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {
        logDebug("newState: " + newState);

        if(isAdded()){
            if (newState == MegaChatApi.INIT_ERROR) {
                // chat cannot initialize, disable chat completely
                if(megaChatApi!=null){
                    megaChatApi.logout(null);
                }
            }
        }
    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {

    }

    private void setError(final EditText editText, String error){
        if(error == null || error.equals("")){
            return;
        }
        switch (editText.getId()){
            case R.id.login_email_text:{
                et_userLayout.setError(error);
                et_userLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                et_userError.setVisibility(View.VISIBLE);
                break;
            }
            case R.id.login_password_text:{
                et_passwordLayout.setError(error);
                et_passwordLayout.setHintTextAppearance(R.style.InputTextAppearanceError);
                et_passwordError.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    private void quitError(EditText editText){
        switch (editText.getId()){
            case R.id.login_email_text:{
                et_userLayout.setError(null);
                et_userLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                et_userError.setVisibility(View.GONE);
                break;
            }
            case R.id.login_password_text:{
                et_passwordLayout.setError(null);
                et_passwordLayout.setHintTextAppearance(R.style.TextAppearance_Design_Hint);
                et_passwordError.setVisibility(View.GONE);
                break;
            }
        }
    }

    void pasteClipboard() {
        logDebug("pasteClipboard");
        pinLongClick = false;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null) {
            String code = clipData.getItemAt(0).getText().toString();
            logDebug("code: " + code);
            if (code != null && code.length() == 6) {
                boolean areDigits = true;
                for (int i=0; i<6; i++) {
                    if (!Character.isDigit(code.charAt(i))) {
                        areDigits = false;
                        break;
                    }
                }
                if (areDigits) {
                    firstPin.setText(""+code.charAt(0));
                    secondPin.setText(""+code.charAt(1));
                    thirdPin.setText(""+code.charAt(2));
                    fourthPin.setText(""+code.charAt(3));
                    fifthPin.setText(""+code.charAt(4));
                    sixthPin.setText(""+code.charAt(5));
                }
                else {
                    firstPin.setText("");
                    secondPin.setText("");
                    thirdPin.setText("");
                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()){
            case R.id.pin_first_login:
            case R.id.pin_second_login:
            case R.id.pin_third_login:
            case R.id.pin_fouth_login:
            case R.id.pin_fifth_login:
            case R.id.pin_sixth_login: {
                pinLongClick = true;
                v.requestFocus();
            }
        }
        return false;
    }

    private void showCancelTransfersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.login_warning_abort_transfers);
        builder.setPositiveButton(R.string.login_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD);
                megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD);
                performLogin();
            }
        });
        builder.setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                Hide dialog
            }
        });
        builder.setCancelable(false);
        builder.show();
    }
}
