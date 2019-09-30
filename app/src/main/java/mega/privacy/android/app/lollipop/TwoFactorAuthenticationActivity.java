package mega.privacy.android.app.lollipop;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextPIN;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class TwoFactorAuthenticationActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface, View.OnLongClickListener, View.OnFocusChangeListener{

    final int LENGTH_SEED = 13;
    final int WIDTH = 520;
    final int FACTOR = 65;
    final float RESIZE = 8f;

    private Toolbar tB;
    private ActionBar aB;

    private RelativeLayout container2FA;
    private ScrollView scrollContainer2FA;
    private ScrollView scrollContainerVerify;
    private ScrollView scrollContainer2FAEnabled;
    private RelativeLayout qrSeedContainer;
    private RelativeLayout confirmContainer;
    private TextView explainSeed;
    private Button setup2FAButton;
    private Button openWithButton;
    private Button next2FAButton;
    private Button exportRKButton;
    private Button dismissRKButton;
    private ImageView qrImage;
    private TableLayout tabSeed;
    private TextView seedText1;
    private TextView seedText2;
    private TextView seedText3;
    private TextView seedText4;
    private TextView seedText5;
    private TextView seedText6;
    private TextView seedText7;
    private TextView seedText8;
    private TextView seedText9;
    private TextView seedText10;
    private TextView seedText11;
    private TextView seedText12;
    private TextView seedText13;
    private ProgressBar qrProgressBar;
    private TextView pinError;
    private TextView suggestionRK;
    private LinearLayout saveRKButton;
    private TextView fileNameRK;

    private String seed = null;
    private ArrayList<String> arraySeed;

    InputMethodManager imm;
    private EditTextPIN firstPin;
    private EditTextPIN secondPin;
    private EditTextPIN thirdPin;
    private EditTextPIN fourthPin;
    private EditTextPIN fifthPin;
    private EditTextPIN sixthPin;
    private StringBuilder sb = new StringBuilder();
    private String pin = null;

    private boolean scanOrCopyIsShown = false;
    private boolean confirm2FAIsShown = false;
    private boolean isEnabled2FA = false;
    private boolean isErrorShown = false;
    private boolean firstTime = true;
    private boolean newAccount = false;
    private boolean pinLongClick = false;
    private boolean rkSaved = false;

    MegaApiAndroid megaApi;

    Bitmap qr = null;

    DisplayMetrics outMetrics;
    String url;

    AlertDialog noAppsDialog;
    boolean isNoAppsDialogShown = false;
    AlertDialog helpDialog;
    boolean isHelpDialogShown = false;

    TwoFactorAuthenticationActivity twoFactorAuthenticationActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");

        setContentView(R.layout.activity_two_factor_authentication);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color_secondary));
        }

        twoFactorAuthenticationActivity = this;

        tB = (Toolbar) findViewById(R.id.toolbar);
        if(tB==null){
            logError("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        tB.setTitle(getString(R.string.settings_2fa));
        setTitle(getString(R.string.settings_2fa));

        if (savedInstanceState != null){
            logDebug("savedInstanceState No null");
            confirm2FAIsShown = savedInstanceState.getBoolean("confirm2FAIsShown", false);
            scanOrCopyIsShown = savedInstanceState.getBoolean("scanOrCopyIsShown", false);
            isEnabled2FA = savedInstanceState.getBoolean("isEnabled2FA", false);
            isErrorShown = savedInstanceState.getBoolean("isErrorShown", false);
            firstTime = savedInstanceState.getBoolean("firstTimeAfterInstallation", true);
            rkSaved = savedInstanceState.getBoolean("rkSaved", false);
            seed = savedInstanceState.getString("seed");
            arraySeed = savedInstanceState.getStringArrayList("arraySeed");
            byte[] qrByteArray = savedInstanceState.getByteArray("qr");
            if (qrByteArray != null){
                qr = BitmapFactory.decodeByteArray(qrByteArray, 0, qrByteArray.length);
            }
            isNoAppsDialogShown = savedInstanceState.getBoolean("isNoAppsDialogShown", false);
            isHelpDialogShown = savedInstanceState.getBoolean("isHelpDialogShown", false);
        }
        else {
            rkSaved = false;
            confirm2FAIsShown = false;
            scanOrCopyIsShown = false;
            isEnabled2FA = false;
            if (getIntent() != null) {
                newAccount = getIntent().getBooleanExtra("newAccount", false);
            }
            isNoAppsDialogShown = false;
            isHelpDialogShown = false;
        }

        container2FA = (RelativeLayout) findViewById(R.id.container_2fa);
        scrollContainer2FA = (ScrollView) findViewById(R.id.scroll_container_2fa);
        scrollContainerVerify = (ScrollView) findViewById(R.id.scroll_container_verify);
        scrollContainer2FAEnabled = (ScrollView) findViewById(R.id.container_2fa_enabled);

        explainSeed = (TextView) findViewById(R.id.explain_qr_seed_2fa_2);
        SpannableString text =  new SpannableString(getString(R.string.explain_qr_seed_2fa_2) + "  QM");
        Drawable questionMarck = ContextCompat.getDrawable(this, R.drawable.ic_b_question_mark);
        questionMarck.setBounds(0, 0, questionMarck.getIntrinsicWidth(), questionMarck.getIntrinsicHeight());
        ImageSpan imageSpan =  new ImageSpan(questionMarck, ImageSpan.ALIGN_BOTTOM);
        text.setSpan(imageSpan, text.length()-2,text.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        explainSeed.setText(text);
        explainSeed.setOnClickListener(this);

        setup2FAButton = (Button) findViewById(R.id.button_enable_2fa);
        setup2FAButton.setOnClickListener(this);
        openWithButton = (Button) findViewById(R.id.button_open_with_2fa);
        openWithButton.setOnClickListener(this);
        next2FAButton = (Button) findViewById(R.id.button_next_2fa);
        next2FAButton.setOnClickListener(this);
        exportRKButton = (Button) findViewById(R.id.button_export_rk);
        exportRKButton.setOnClickListener(this);
        dismissRKButton  = (Button) findViewById(R.id.button_dismiss_rk);
        dismissRKButton.setOnClickListener(this);
        qrSeedContainer = (RelativeLayout) findViewById(R.id.container_qr_seed_2fa);
        confirmContainer = (RelativeLayout) findViewById(R.id.container_confirm_2fa);
        qrImage = (ImageView) findViewById(R.id.qr_2fa);
        qrProgressBar = (ProgressBar) findViewById(R.id.qr_progress_bar);
        tabSeed = (TableLayout) findViewById(R.id.seed_2fa);
        tabSeed.setOnLongClickListener(this);
        seedText1 = (TextView) findViewById(R.id.seed_2fa_1);
        seedText2 = (TextView) findViewById(R.id.seed_2fa_2);
        seedText3 = (TextView) findViewById(R.id.seed_2fa_3);
        seedText4 = (TextView) findViewById(R.id.seed_2fa_4);
        seedText5 = (TextView) findViewById(R.id.seed_2fa_5);
        seedText6 = (TextView) findViewById(R.id.seed_2fa_6);
        seedText7 = (TextView) findViewById(R.id.seed_2fa_7);
        seedText8 = (TextView) findViewById(R.id.seed_2fa_8);
        seedText9 = (TextView) findViewById(R.id.seed_2fa_9);
        seedText10 = (TextView) findViewById(R.id.seed_2fa_10);
        seedText11 = (TextView) findViewById(R.id.seed_2fa_11);
        seedText12 = (TextView) findViewById(R.id.seed_2fa_12);
        seedText13 = (TextView) findViewById(R.id.seed_2fa_13);
        pinError = (TextView) findViewById(R.id.pin_2fa_error);
        pinError.setVisibility(View.GONE);

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        firstPin = (EditTextPIN) findViewById(R.id.pass_first);
        firstPin.setOnLongClickListener(this);
        imm.showSoftInput(firstPin, InputMethodManager.SHOW_FORCED);
        firstPin.setOnFocusChangeListener(this);
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

                    if (firstTime && !pinLongClick){
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
                        quitError();
                    }
                }
            }
        });

        secondPin = (EditTextPIN) findViewById(R.id.pass_second);
        secondPin.setOnLongClickListener(this);
        imm.showSoftInput(secondPin, InputMethodManager.SHOW_FORCED);
        secondPin.setOnFocusChangeListener(this);
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

                    if (firstTime && !pinLongClick) {
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
                        quitError();
                    }
                }
            }
        });

        thirdPin = (EditTextPIN) findViewById(R.id.pass_third);
        thirdPin.setOnLongClickListener(this);
        imm.showSoftInput(thirdPin, InputMethodManager.SHOW_FORCED);
        thirdPin.setOnFocusChangeListener(this);
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

                    if (firstTime && !pinLongClick) {
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
                        quitError();
                    }
                }
            }
        });

        fourthPin = (EditTextPIN) findViewById(R.id.pass_fourth);
        fourthPin.setOnLongClickListener(this);
        imm.showSoftInput(fourthPin, InputMethodManager.SHOW_FORCED);
        fourthPin.setOnFocusChangeListener(this);
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

                    if (firstTime && !pinLongClick) {
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
                        quitError();
                    }
                }
            }
        });

        fifthPin = (EditTextPIN) findViewById(R.id.pass_fifth);
        fifthPin.setOnLongClickListener(this);
        imm.showSoftInput(fifthPin, InputMethodManager.SHOW_FORCED);
        fifthPin.setOnFocusChangeListener(this);
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

                    if (firstTime && !pinLongClick) {
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
                        quitError();
                    }
                }
            }
        });

        sixthPin = (EditTextPIN) findViewById(R.id.pass_sixth);
        sixthPin.setOnLongClickListener(this);
        imm.showSoftInput(sixthPin, InputMethodManager.SHOW_FORCED);
        sixthPin.setOnFocusChangeListener(this);
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
                    hideKeyboard(twoFactorAuthenticationActivity, 0);

                    if (pinLongClick) {
                        pasteClipboard();
                    }
                    else {
                        permitVerify();
                    }
                }
                else {
                    if (isErrorShown){
                        quitError();
                    }
                }
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

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

        suggestionRK = (TextView) findViewById(R.id.recommendation_2fa_enabled);
        saveRKButton = (LinearLayout) findViewById(R.id.container_rk_2fa);
        saveRKButton.setOnClickListener(this);

        fileNameRK = findViewById(R.id.fileNameRK);
        fileNameRK.setText(getRecoveryKeyFileName());

        if (scanOrCopyIsShown || newAccount){
            showScanOrCopyLayout();
        }
        else if (confirm2FAIsShown){
            qrSeedContainer.setVisibility(View.GONE);
            confirmContainer.setVisibility(View.VISIBLE);
            scrollContainer2FA.setVisibility(View.GONE);
            scrollContainerVerify.setVisibility(View.VISIBLE);
            scrollContainer2FAEnabled.setVisibility(View.GONE);

            if (isErrorShown){
                showError();
            }
        }
        else if (isEnabled2FA){
            scrollContainer2FA.setVisibility(View.GONE);
            scrollContainerVerify.setVisibility(View.GONE);
            scrollContainer2FAEnabled.setVisibility(View.VISIBLE);
            if (rkSaved) {
                dismissRKButton.setVisibility(View.VISIBLE);
            }
            else {
                dismissRKButton.setVisibility(View.GONE);
            }
        }
        else {
            megaApi.multiFactorAuthGetCode(this);
            scrollContainer2FA.setVisibility(View.VISIBLE);
            scrollContainerVerify.setVisibility(View.GONE);
            scrollContainer2FAEnabled.setVisibility(View.GONE);
        }

//        megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
    }

    void showScanOrCopyLayout () {
        scanOrCopyIsShown = true;
        qrSeedContainer.setVisibility(View.VISIBLE);
        confirmContainer.setVisibility(View.GONE);
        scrollContainer2FA.setVisibility(View.GONE);
        scrollContainerVerify.setVisibility(View.VISIBLE);
        scrollContainer2FAEnabled.setVisibility(View.GONE);
        if (seed != null){
            logDebug("Seed not null");
            setSeed();
            if (qr != null){
                logDebug("QR not null");
                qrImage.setImageBitmap(qr);
            }
            else {
                generate2FAQR();
            }
        }
        else {
            megaApi.multiFactorAuthGetCode(this);
        }

        if (isNoAppsDialogShown) {
            showAlertNotAppAvailable();
        }

        if (isHelpDialogShown) {
            showAlertHelp();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (megaApi != null) {
            megaApi.removeRequestListener(this);
        }
    }

    @Override
    public void onBackPressed() {
        retryConnectionsAndSignalPresence();

        if (confirm2FAIsShown) {
            confirm2FAIsShown = false;
            showScanOrCopyLayout();
        }
        else {
            if (isEnabled2FA) {
                if (rkSaved) {
                    super.onBackPressed();
                }
                else {
                    showSnackbar(getString(R.string.backup_rk_2fa_end));
                }
                update2FASetting();
            }
            else {
                super.onBackPressed();
            }
        }
    }

    void update2FASetting () {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_2FA_SETTINGS);
        intent.putExtra("enabled", true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void pasteClipboard() {
        logDebug("pasteClipboard");
        pinLongClick = false;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null) {
            String code = clipData.getItemAt(0).getText().toString();
            logDebug("Code: "+code);
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

    void permitVerify(){
        logDebug("permitVerify");
        if (confirm2FAIsShown && firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1
                && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
            hideKeyboard(this, 0);
            if (sb.length()>0) {
                sb.delete(0, sb.length());
            }
            sb.append(firstPin.getText());
            sb.append(secondPin.getText());
            sb.append(thirdPin.getText());
            sb.append(fourthPin.getText());
            sb.append(fifthPin.getText());
            sb.append(sixthPin.getText());
            pin = sb.toString().trim();

            if (pin != null){
                megaApi.multiFactorAuthEnable(pin, this);
            }
        }
    }

    void setSeed () {
        arraySeed = new ArrayList<>();
        int index = 0;
        for (int i=0; i<LENGTH_SEED; i++) {
            arraySeed.add(seed.substring(index, index+4));
            index += 4;
        }
        seedText1.setText(arraySeed.get(0));
        seedText2.setText(arraySeed.get(1));
        seedText3.setText(arraySeed.get(2));
        seedText4.setText(arraySeed.get(3));
        seedText5.setText(arraySeed.get(4));
        seedText6.setText(arraySeed.get(5));
        seedText7.setText(arraySeed.get(6));
        seedText8.setText(arraySeed.get(7));
        seedText9.setText(arraySeed.get(8));
        seedText10.setText(arraySeed.get(9));
        seedText11.setText(arraySeed.get(10));
        seedText12.setText(arraySeed.get(11));
        seedText13.setText(arraySeed.get(12));
    }

    void generate2FAQR (){
        logDebug("generate2FAQR");

        url = null;
        String myEmail = megaApi.getMyEmail();
//        String myEmail = "aw+@mega.nz";
//        String myEmail = "";
//        String myEmail = "abcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyz@yopmail.com";
//        String myEmail = "abcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyz@gmail.com";
//        String myEmail = "abcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyzabcdabcdefghijklmnopqrstuvwxyz@gmail.com";

        if (myEmail != null & seed != null){
            url = getString(R.string.url_qr_2fa, myEmail, seed);
            setSeed();
        }
        if (url != null) {
            Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            BitMatrix bitMatrix = null;
            try {
                bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 40, 40, null);
            } catch (WriterException e) {
                e.printStackTrace();
                return;
            }
            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            int[] pixels = new int[w * h];
            int width = (w * WIDTH) / FACTOR;

            qr = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(qr);
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(WHITE);
            c.drawRect(0, 0, width, width, paint);
            paint.setColor(BLACK);

            float size = w - 12;

            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? BLACK : WHITE;
                    if (pixels[offset + x] == BLACK){
                        c.drawCircle(x*RESIZE, y*RESIZE, 3.5f, paint);
                    }
                }
            }

//            8.5 width
            paint.setColor(WHITE);
            c.drawRect(3*RESIZE, 3*RESIZE, 11.5f*RESIZE, 11.5f*RESIZE, paint);
            c.drawRect(size*RESIZE, 3*RESIZE, (size+8.5f)*RESIZE, 11.5f*RESIZE, paint);
            c.drawRect(3*RESIZE, size*RESIZE, 11.5f*RESIZE, (size+8.5f)*RESIZE, paint);

            paint.setColor(BLACK);

            if (Build.VERSION.SDK_INT >= 21) {
                c.drawRoundRect(3.75f * RESIZE, 3.75f * RESIZE, 10.75f * RESIZE, 10.75f * RESIZE, 15, 15, paint);
//                7 width, 0.75 more than last
                c.drawRoundRect((size+0.75f) * RESIZE, 3.75f * RESIZE, (size+0.75f+7f) * RESIZE, 10.75f * RESIZE, 15, 15, paint);
                c.drawRoundRect(3.75f * RESIZE, (size+0.75f) * RESIZE, 10.75f * RESIZE, (size+0.75f+7f) * RESIZE, 15, 15, paint);

                paint.setColor(WHITE);
                c.drawRoundRect(4.75f * RESIZE, 4.75f * RESIZE, 9.75f * RESIZE, 9.75f * RESIZE, 12.5f, 12.5f, paint);
//                5 width, 1.75 more than first
                c.drawRoundRect((size+1.75f) * RESIZE, 4.75f * RESIZE, (size+1.75f+5f) * RESIZE, 9.75f * RESIZE, 12.5f, 12.5f, paint);
                c.drawRoundRect(4.75f * RESIZE, (size+1.75f) * RESIZE, 9.75f * RESIZE, (size+1.75f+5f) * RESIZE, 12.5f, 12.5f, paint);
            }
            else {
                c.drawRoundRect(new RectF(3.75f * RESIZE, 3.75f * RESIZE, 10.75f * RESIZE, 10.75f * RESIZE), 15, 15, paint);
                c.drawRoundRect(new RectF((size+0.75f) * RESIZE, 3.75f * RESIZE, (size+0.75f+7f) * RESIZE, 10.75f * RESIZE), 15, 15, paint);
                c.drawRoundRect(new RectF(3.75f * RESIZE, (size+0.75f) * RESIZE, 10.75f * RESIZE, (size+0.75f+7f) * RESIZE), 15, 15, paint);

                paint.setColor(WHITE);
                c.drawRoundRect(new RectF(4.75f * RESIZE, 4.75f * RESIZE, 9.75f * RESIZE, 9.75f * RESIZE), 12.5f, 12.5f, paint);
                c.drawRoundRect(new RectF((size+1.75f) * RESIZE, 4.75f * RESIZE, (size+1.75f+5f) * RESIZE, 9.75f * RESIZE), 12.5f, 12.5f, paint);
                c.drawRoundRect(new RectF(4.75f * RESIZE, (size+1.75f) * RESIZE, 9.75f * RESIZE, (size+1.75f+5f) * RESIZE), 12.5f, 12.5f, paint);
            }

            paint.setColor(BLACK);
            c.drawCircle(7.25f*RESIZE, 7.25f*RESIZE, 12f, paint);
//            4.25 more than first
            c.drawCircle((size+4.25f)*RESIZE, 7.25f*RESIZE, 12f, paint);
            c.drawCircle(7.25f*RESIZE, (size+4.25f)*RESIZE, 12f, paint);

            if (qr != null){
                qrImage.setImageBitmap(qr);
                qrProgressBar.setVisibility(View.GONE);
            }
            else {
                showSnackbar(getResources().getString(R.string.qr_seed_text_error));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        logDebug("onSaveInstanceState");

        outState.putBoolean("confirm2FAIsShown", confirm2FAIsShown);
        outState.putBoolean("scanOrCopyIsShown", scanOrCopyIsShown);
        outState.putBoolean("isEnabled2FA", isEnabled2FA);
        outState.putBoolean("isErrorShown", isErrorShown);
        outState.putBoolean("firstTimeAfterInstallation", firstTime);
        outState.putBoolean("rkSaved", rkSaved);
        outState.putBoolean("isNoAppsDialogShown", isNoAppsDialogShown);
        outState.putBoolean("isHelpDialogShown", isHelpDialogShown);

        if (scanOrCopyIsShown){
            logDebug("scanOrCopyIsShown");
            if (qr != null) {
                logDebug("QR not null");
                ByteArrayOutputStream qrOutputStream = new ByteArrayOutputStream();
                qr.compress(Bitmap.CompressFormat.JPEG, 100, qrOutputStream);
                byte[] qrByteArray = qrOutputStream.toByteArray();
                outState.putByteArray("qr", qrByteArray);
            }
            outState.putString("seed", seed);
            outState.putStringArrayList("arraySeed", arraySeed);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.button_enable_2fa: {
                scanOrCopyIsShown = true;
                confirm2FAIsShown = false;
                isEnabled2FA = false;
                qrSeedContainer.setVisibility(View.VISIBLE);
                confirmContainer.setVisibility(View.GONE);
                scrollContainer2FA.setVisibility(View.GONE);
                scrollContainerVerify.setVisibility(View.VISIBLE);
                scrollContainer2FAEnabled.setVisibility(View.GONE);
                break;
            }
            case R.id.button_next_2fa:{
                scanOrCopyIsShown = false;
                newAccount = false;
                confirm2FAIsShown = true;
                isEnabled2FA = false;
                qrSeedContainer.setVisibility(View.GONE);
                confirmContainer.setVisibility(View.VISIBLE);
                scrollContainer2FA.setVisibility(View.GONE);
                scrollContainerVerify.setVisibility(View.VISIBLE);
                scrollContainer2FAEnabled.setVisibility(View.GONE);
                firstPin.requestFocus();
                imm.showSoftInput(fifthPin, InputMethodManager.SHOW_FORCED);
                break;
            }
            case R.id.button_open_with_2fa: {
                if (url == null) {
                    String myEmail = megaApi.getMyEmail();
                    if (myEmail != null & seed != null){
                        url = getString(R.string.url_qr_2fa, myEmail, seed);
                    }
                }
                if (url != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    logDebug("URL: " + url + ", Seed: " + seed);
                    if (isIntentAvailable(this, intent)) {
                        startActivity(intent);
                    }
                    else {
                        showAlertNotAppAvailable();
                    }
                }
                break;
            }
            case R.id.container_rk_2fa:
//                {
//                update2FASetting();
//                RecoveryKeyBottomSheetDialogFragment recoveryKeyBottomSheetDialogFragment = new RecoveryKeyBottomSheetDialogFragment();
//                recoveryKeyBottomSheetDialogFragment.show(getSupportFragmentManager(), recoveryKeyBottomSheetDialogFragment.getTag());
//                break;
//            }
            case R.id.button_export_rk:{
                update2FASetting();
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_RECOVERY_KEY_EXPORTED);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.button_dismiss_rk:{
                update2FASetting();
                this.finish();
                break;
            }
            case R.id.cancel_button_no_app: {
                try {
                    noAppsDialog.dismiss();
                }catch (Exception e){}
                isNoAppsDialogShown = false;
                break;
            }
            case R.id.open_button_no_app: {
                try {
                    noAppsDialog.dismiss();
                }catch (Exception e){}
                isNoAppsDialogShown = false;
                openPlayStore();
                break;
            }
            case R.id.explain_qr_seed_2fa_2: {
                showAlertHelp();
                break;
            }
            case R.id.cancel_button_help: {
                try {
                    helpDialog.dismiss();
                }catch (Exception e){}
                isHelpDialogShown = false;
                break;
            }
            case R.id.play_store_button_help: {
                try {
                    helpDialog.dismiss();
                }catch (Exception e){}
                isHelpDialogShown = false;
                openPlayStore();
                break;
            }
        }
    }

    void openPlayStore() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=authenticator&c=apps"));
        startActivity(intent);
    }

    void showAlertHelp () {
        logDebug("showAlertHelp");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_2fa_help, null);
        builder.setView(v);

        Button cancelButton = (Button) v.findViewById(R.id.cancel_button_help);
        cancelButton.setOnClickListener(this);
        Button playStoreButton = (Button) v.findViewById(R.id.play_store_button_help);
        playStoreButton.setOnClickListener(this);

        helpDialog = builder.create();
        helpDialog.setCanceledOnTouchOutside(false);
        helpDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isHelpDialogShown = false;
            }
        });
        try {
            helpDialog.show();
        }catch (Exception e){}

        isHelpDialogShown = true;
    }

    void showAlertNotAppAvailable () {
        logDebug("showAlertNotAppAvailable");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_no_authentication_apps, null);
        builder.setView(v);

        Button cancelButton = (Button) v.findViewById(R.id.cancel_button_no_app);
        cancelButton.setOnClickListener(this);
        Button openButton = (Button) v.findViewById(R.id.open_button_no_app);
        openButton.setOnClickListener(this);

        noAppsDialog = builder.create();
        noAppsDialog.setCanceledOnTouchOutside(false);
        noAppsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isNoAppsDialogShown = false;
            }
        });
        try {
            noAppsDialog.show();
        }catch (Exception e){}

        isNoAppsDialogShown = true;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish");
        if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_GET){
            logDebug("MegaRequest.TYPE_MULTI_FACTOR_AUTH_GET");
            if (e.getErrorCode() == MegaError.API_OK){
                logDebug("MegaError.API_OK");
                seed = request.getText();
                if (seed != null){
//                    seedText.setText(seed.toUpperCase());
                    qrProgressBar.setVisibility(View.VISIBLE);
                    generate2FAQR();
                }
                else {
                    showSnackbar(getResources().getString(R.string.qr_seed_text_error));
                }
            }
            else {
                logError("e.getErrorCode(): " + e.getErrorCode());
                showSnackbar(getResources().getString(R.string.qr_seed_text_error));
            }
        }
        else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_SET){
            logDebug("TYPE_MULTI_FACTOR_AUTH_SET: " + e.getErrorCode());
            if (request.getFlag() && e.getErrorCode() == MegaError.API_OK) {
                logDebug("Pin correct: Two-Factor Authentication enabled");
                confirm2FAIsShown = false;
                isEnabled2FA = true;
                megaApi.isMasterKeyExported(this);
            }
            else if (e.getErrorCode() == MegaError.API_EFAILED){
                logWarning("Pin not correct: " + request.getPassword());
                if (request.getFlag()){
                    showError();
                }
            }
            else {
                logError("An error ocurred trying to enable Two-Factor Authentication");
                showSnackbar(getString(R.string.error_enable_2fa));
            }

//            megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
        }
        else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK){
            if (e.getErrorCode() == MegaError.API_OK){
                logDebug("TYPE_MULTI_FACTOR_AUTH_CHECK: " + request.getFlag());
            }
        }
        else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER && request.getParamType() == MegaApiJava.USER_ATTR_PWD_REMINDER) {
            logDebug("TYPE_GET_ATTR_USER");
            if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
                logDebug("TYPE_GET_ATTR_USER API_OK");

                scrollContainer2FA.setVisibility(View.GONE);
                scrollContainerVerify.setVisibility(View.GONE);
                scrollContainer2FAEnabled.setVisibility(View.VISIBLE);

                if (e.getErrorCode() == MegaError.API_OK && request.getAccess() == 1) {
                    rkSaved = true;
                    dismissRKButton.setVisibility(View.VISIBLE);
                }
                else {
                    rkSaved = false;
                    dismissRKButton.setVisibility(View.GONE);
                }
            }
            else {
                logError("TYPE_GET_ATTR_USER error: " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    void quitError(){
        isErrorShown = false;
        pinError.setVisibility(View.GONE);
        firstPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
        secondPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
        thirdPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
        fourthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
        fifthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
        sixthPin.setTextColor(ContextCompat.getColor(this, R.color.name_my_account));
    }

    void showError(){
        firstTime = false;
        isErrorShown = true;
        pinError.setVisibility(View.VISIBLE);
        firstPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
        secondPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
        thirdPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
        fourthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
        fifthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
        sixthPin.setTextColor(ContextCompat.getColor(this, R.color.login_warning));
    }

    @Override
    public boolean onLongClick(View v) {

        switch (v.getId()){
            case R.id.seed_2fa: {
                copySeed();
                return true;
            }
            case R.id.pass_first:
            case R.id.pass_second:
            case R.id.pass_third:
            case R.id.pass_fourth:
            case R.id.pass_fifth:
            case R.id.pass_sixth: {
                pinLongClick = true;
                v.requestFocus();
            }

        }
        return false;
    }

    void copySeed () {
        logDebug("Copy seed");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (seed != null) {
            ClipData clip = ClipData.newPlainText("seed", seed);
            if (clip != null){
                clipboard.setPrimaryClip(clip);
                showSnackbar(getResources().getString(R.string.messages_copied_clipboard));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK){
            logDebug("REQUEST_DOWNLOAD_FOLDER");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (parentPath != null){
                logDebug("parentPath no NULL");

                parentPath = parentPath + File.separator + getRecoveryKeyFileName();

                Intent newIntent = new Intent(this, ManagerActivityLollipop.class);
                newIntent.putExtra("parentPath", parentPath);
                newIntent.setAction(ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT);
                startActivity(newIntent);
            }
        }
    }

    public void showSnackbar(String s){
        showSnackbar(container2FA, s);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch (v.getId()) {
            case R.id.pass_first:{
                if (hasFocus) {
                    firstPin.setText("");
                }
                break;
            }
            case R.id.pass_second:{
                if (hasFocus) {
                    secondPin.setText("");
                }
                break;
            }
            case R.id.pass_third:{
                if (hasFocus) {
                    thirdPin.setText("");
                }
                break;
            }
            case R.id.pass_fourth:{
                if (hasFocus) {
                    fourthPin.setText("");
                }
                break;
            }
            case R.id.pass_fifth:{
                if (hasFocus) {
                    fifthPin.setText("");
                }
                break;
            }
            case R.id.pass_sixth:{
                if (hasFocus) {
                    sixthPin.setText("");
                }
                break;
            }
        }
    }
}
