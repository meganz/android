package mega.privacy.android.app.lollipop.twofa;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextPIN;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * Created by mega on 28/05/18.
 */

public class TwoFactorAuthenticationActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface, View.OnLongClickListener, View.OnFocusChangeListener{

    final int LENGTH_SEED = 13;

    private Toolbar tB;
    private ActionBar aB;

    private RelativeLayout container2FA;
    private ScrollView scrollContainer2FA;
    private ScrollView scrollContainerVerify;
    private ScrollView scrollContainer2FAEnabled;
    private RelativeLayout qrSeedContainer;
    private RelativeLayout confirmContainer;
    private Button setup2FAButton;
    private Button copySeedButton;
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

    MegaApiAndroid megaApi;

    Bitmap qr = null;

    DisplayMetrics outMetrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        setContentView(R.layout.activity_two_factor_authentication);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        tB = (Toolbar) findViewById(R.id.toolbar);
        if(tB==null){
            log("Tb is Null");
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
            log("savedInstanceState No null");
            confirm2FAIsShown = savedInstanceState.getBoolean("confirm2FAIsShown", false);
            scanOrCopyIsShown = savedInstanceState.getBoolean("scanOrCopyIsShown", false);
            isEnabled2FA = savedInstanceState.getBoolean("isEnabled2FA", false);
            isErrorShown = savedInstanceState.getBoolean("isErrorShown", false);
            firstTime = savedInstanceState.getBoolean("firstTime", true);
            seed = savedInstanceState.getString("seed");
            arraySeed = savedInstanceState.getStringArrayList("arraySeed");
            byte[] qrByteArray = savedInstanceState.getByteArray("qr");
            if (qrByteArray != null){
                qr = BitmapFactory.decodeByteArray(qrByteArray, 0, qrByteArray.length);
            }
        }
        else {
            confirm2FAIsShown = false;
            scanOrCopyIsShown = false;
            isEnabled2FA = false;
        }

        container2FA = (RelativeLayout) findViewById(R.id.container_2fa);
        scrollContainer2FA = (ScrollView) findViewById(R.id.scroll_container_2fa);
        scrollContainerVerify = (ScrollView) findViewById(R.id.scroll_container_verify);
        scrollContainer2FAEnabled = (ScrollView) findViewById(R.id.container_2fa_enabled);
        setup2FAButton = (Button) findViewById(R.id.button_enable_2fa);
        setup2FAButton.setOnClickListener(this);
        copySeedButton = (Button) findViewById(R.id.button_copy_2fa);
        copySeedButton.setOnClickListener(this);
        next2FAButton = (Button) findViewById(R.id.button_next_2fa);
        next2FAButton.setOnClickListener(this);
        exportRKButton = (Button) findViewById(R.id.button_export_rk);
        exportRKButton.setOnClickListener(this);
        dismissRKButton  =(Button) findViewById(R.id.button_dismiss_rk);
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

                    if (firstTime){
                        secondPin.setText("");
                        thirdPin.setText("");
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
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

                    if (firstTime) {
                        thirdPin.setText("");
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
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

                    if (firstTime) {
                        fourthPin.setText("");
                        fifthPin.setText("");
                        sixthPin.setText("");
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

                    if (firstTime) {
                        fifthPin.setText("");
                        sixthPin.setText("");
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

                    if (firstTime) {
                        sixthPin.setText("");
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
                    hideKeyboard();

                    permitVerify();
                }
                else {
                    if (isErrorShown){
                        quitError();
                    }
                }
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (scanOrCopyIsShown){
            qrSeedContainer.setVisibility(View.VISIBLE);
            confirmContainer.setVisibility(View.GONE);
            scrollContainer2FA.setVisibility(View.GONE);
            scrollContainerVerify.setVisibility(View.VISIBLE);
            scrollContainer2FAEnabled.setVisibility(View.GONE);
            if (seed != null){
                log("seed no null");
                setSeed();
                if (qr != null){
                    log("qr no null");
                    qrImage.setImageBitmap(qr);
                }
                else {
                    generate2FAQR();
                }
            }
            else {
                megaApi.multiFactorAuthGetCode(this);
            }
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
        }
        else {
            megaApi.multiFactorAuthGetCode(this);
            scrollContainer2FA.setVisibility(View.VISIBLE);
            scrollContainerVerify.setVisibility(View.GONE);
            scrollContainer2FAEnabled.setVisibility(View.GONE);
        }

        megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isEnabled2FA) {
            update2FASetting();
        }
    }

    void update2FASetting () {
        Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_2FA_SETTINGS);
        intent.putExtra("enabled", true);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void permitVerify(){
        if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
            hideKeyboard();
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
            log("PIN: "+pin);
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
        seedText1.setText(arraySeed.get(0).toUpperCase());
        seedText2.setText(arraySeed.get(1).toUpperCase());
        seedText3.setText(arraySeed.get(2).toUpperCase());
        seedText4.setText(arraySeed.get(3).toUpperCase());
        seedText5.setText(arraySeed.get(4).toUpperCase());
        seedText6.setText(arraySeed.get(5).toUpperCase());
        seedText7.setText(arraySeed.get(6).toUpperCase());
        seedText8.setText(arraySeed.get(7).toUpperCase());
        seedText9.setText(arraySeed.get(8).toUpperCase());
        seedText10.setText(arraySeed.get(9).toUpperCase());
        seedText11.setText(arraySeed.get(10).toUpperCase());
        seedText12.setText(arraySeed.get(11).toUpperCase());
        seedText13.setText(arraySeed.get(12).toUpperCase());
    }

    void generate2FAQR (){
        log("generate2FAQR");

        Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        BitMatrix bitMatrix = null;
        String url = null;
        String myEmail = megaApi.getMyEmail();
        if (myEmail != null & seed != null){
            url = getString(R.string.url_qr_2fa, myEmail, seed);
            setSeed();
        }
        if (url != null){
            try {
                bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 500, 500, null);
            } catch (WriterException e) {
                e.printStackTrace();
            }
            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            int[] pixels = new int[w * h];

            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? BLACK : WHITE;
                }
            }

            qr = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            qr.setPixels(pixels, 0, w, 0, 0, w, h);

            if (qr != null){
                qrImage.setImageBitmap(qr);
                qrProgressBar.setVisibility(View.GONE);
            }
            else {
                showSnackbar(getResources().getString(R.string.qr_seed_text_error));
            }
        }
    }

    void hideKeyboard(){

        View v = this.getCurrentFocus();
        if (v != null){
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        log("onSaveInstanceState");

        outState.putBoolean("confirm2FAIsShown", confirm2FAIsShown);
        outState.putBoolean("scanOrCopyIsShown", scanOrCopyIsShown);
        outState.putBoolean("isEnabled2FA", isEnabled2FA);
        outState.putBoolean("isErrorShown", isErrorShown);
        outState.putBoolean("firstTime", firstTime);

        if (scanOrCopyIsShown){
            log("scanOrCopyIsShown");
            if (qr != null) {
                log("QR no null");
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
            case R.id.button_copy_2fa: {
                copySeed();
                break;
            }
            case R.id.button_export_rk:{
                update2FASetting();
                AccountController aC = new AccountController(this);
                aC.saveRkToFileSystem(false);
                break;
            }
            case R.id.button_dismiss_rk:{
                update2FASetting();
                this.finish();
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("TwoFactorAuthenticationActivity", message);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish");
        if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_GET){
            log("MegaRequest.TYPE_MULTI_FACTOR_AUTH_GET");
            if (e.getErrorCode() == MegaError.API_OK){
                log("MegaError.API_OK");
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
                log("e.getErrorCode(): " + e.getErrorCode());
                showSnackbar(getResources().getString(R.string.qr_seed_text_error));
            }
        }
        else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_SET){
            log("TYPE_MULTI_FACTOR_AUTH_SET: "+e.getErrorCode());
            if (request.getFlag() && e.getErrorCode() == MegaError.API_OK){
                log("Pin correct: Two-Factor Authentication enabled");
                confirm2FAIsShown = false;
                isEnabled2FA = true;
                scrollContainer2FA.setVisibility(View.GONE);
                scrollContainerVerify.setVisibility(View.GONE);
                scrollContainer2FAEnabled.setVisibility(View.VISIBLE);
            }
            else if (!request.getFlag() && e.getErrorCode() == MegaError.API_OK){
                log("Pin correct: Two-Factor Authentication disabled");
            }
            else if (e.getErrorCode() == MegaError.API_EFAILED){
                log("Pin not correct");
                if (request.getFlag()){
                    showError();
                }
            }
            else {
                log("An error ocurred trying to enable Two-Factor Authentication");
            }

            megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
        }
        else if (request.getType() == MegaRequest.TYPE_MULTI_FACTOR_AUTH_CHECK){
            if (e.getErrorCode() == MegaError.API_OK){
                log("TYPE_MULTI_FACTOR_AUTH_CHECK: "+request.getFlag());
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
        }
        return false;
    }

    void copySeed () {
        log("copy seed");
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

        if (requestCode == Constants.REQUEST_DOWNLOAD_FOLDER && resultCode == RESULT_OK){
            log("REQUEST_DOWNLOAD_FOLDER");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (parentPath != null){
                log("parentPath no NULL");
                String[] split = Util.rKFile.split("/");
                parentPath = parentPath+"/"+split[split.length-1];
                Intent newIntent = new Intent(this, ManagerActivityLollipop.class);
                newIntent.putExtra("parentPath", parentPath);
                newIntent.putExtra("fromOffline", true);
                newIntent.setAction(Constants.ACTION_REQUEST_DOWNLOAD_FOLDER_LOGOUT);
                startActivity(newIntent);
            }
        }
    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(container2FA, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
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
