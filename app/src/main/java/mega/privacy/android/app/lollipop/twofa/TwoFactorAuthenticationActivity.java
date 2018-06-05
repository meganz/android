package mega.privacy.android.app.lollipop.twofa;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
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

public class TwoFactorAuthenticationActivity extends PinActivityLollipop implements View.OnClickListener, MegaRequestListenerInterface, View.OnLongClickListener{

    private Toolbar tB;
    private ActionBar aB;

    private RelativeLayout container2FA;
    private ScrollView scrollContainer2FA;
    private ScrollView scrollContainerVerify;
    private ScrollView scrollContainer2FAEnabled;
    private RelativeLayout qrSeedContainer;
    private RelativeLayout confirmContainer;
//    private Button next2FAButton;
    private Button setup2FAButton;
    private Button verify2FAButton;
    private Button exportRKButton;
    private Button dismissRKButton;
    private ImageView qrImage;
    private TextView seedText;
    private ProgressBar qrProgressBar;
    private TextView pinError;

    private String seed = null;

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
//        next2FAButton = (Button) findViewById(R.id.button_next_2fa);
//        next2FAButton.setOnClickListener(this);
        verify2FAButton = (Button) findViewById(R.id.button_verify_2fa);
        verify2FAButton.setOnClickListener(this);
        exportRKButton = (Button) findViewById(R.id.button_export_rk);
        exportRKButton.setOnClickListener(this);
        dismissRKButton  =(Button) findViewById(R.id.button_dismiss_rk);
        dismissRKButton.setOnClickListener(this);
        qrSeedContainer = (RelativeLayout) findViewById(R.id.container_qr_2fa);
        confirmContainer = (RelativeLayout) findViewById(R.id.container_confirm_2fa);
        qrImage = (ImageView) findViewById(R.id.qr_2fa);
        qrProgressBar = (ProgressBar) findViewById(R.id.qr_progress_bar);
        seedText = (TextView) findViewById(R.id.seed_2fa);
        seedText.setOnLongClickListener(this);
        pinError = (TextView) findViewById(R.id.pin_2fa_error);
        pinError.setVisibility(View.GONE);

        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        firstPin = (EditTextPIN) findViewById(R.id.pass_first);
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
                    verify2FAButton.setVisibility(View.GONE);
                }
            }
        });

        secondPin = (EditTextPIN) findViewById(R.id.pass_second);
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
                    verify2FAButton.setVisibility(View.GONE);
                }
            }
        });

        thirdPin = (EditTextPIN) findViewById(R.id.pass_third);
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
                    verify2FAButton.setVisibility(View.GONE);
                }
            }
        });

        fourthPin = (EditTextPIN) findViewById(R.id.pass_fourth);
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
                    verify2FAButton.setVisibility(View.GONE);
                }
            }
        });

        fifthPin = (EditTextPIN) findViewById(R.id.pass_fifth);
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
                    verify2FAButton.setVisibility(View.GONE);
                }
            }
        });

        sixthPin = (EditTextPIN) findViewById(R.id.pass_sixth);
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
                    hideKeyboard();

                    permitVerify();
                }
                else {
                    if (isErrorShown){
                        quitError();
                    }
                    verify2FAButton.setVisibility(View.GONE);
                }
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        if (confirm2FAIsShown){
            scrollContainer2FA.setVisibility(View.GONE);
            scrollContainerVerify.setVisibility(View.VISIBLE);
            scrollContainer2FAEnabled.setVisibility(View.GONE);

            if (seed != null){
                log("seed no null");
                seedText.setText(seed.toUpperCase());
                if (qr != null){
                    log("qr no null");
                    qrImage.setImageBitmap(qr);
                    qrProgressBar.setVisibility(View.GONE);
                }
                else {
                    qrProgressBar.setVisibility(View.VISIBLE);
                    generate2FAQR();
                }
            }
            else {
                megaApi.multiFactorAuthGetCode(this);
            }

            if (isErrorShown){
                showError();
            }
//            qrSeedContainer.setVisibility(View.GONE);
//            confirmContainer.setVisibility(View.VISIBLE);
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
//        else if (scanOrCopyIsShown){
//            scrollContainer2FA.setVisibility(View.GONE);
//            qrSeedContainer.setVisibility(View.VISIBLE);
//            confirmContainer.setVisibility(View.GONE);
//
//            if (seed != null){
//                log("seed no null");
//                seedText.setText(seed);
//                if (qr != null){
//                    log("qr no null");
//                    qrImage.setImageBitmap(qr);
//                }
//                else {
//                    generate2FAQR();
//                }
//            }
//            else {
//                megaApi.multiFactorAuthGetCode(this);
//            }
//        }

        megaApi.multiFactorAuthDisable("", this);
        megaApi.multiFactorAuthCheck(megaApi.getMyEmail(), this);
    }

    void permitVerify(){
        if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
            if (!isErrorShown) {
                verify2FAButton.setVisibility(View.VISIBLE);
            }
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
        }
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
//            String seed2FA = "";
//            int i = 0;
//            int k = 0;
//            for (int j=0; j<seed.length(); j++){
//                i++;
//                seed2FA += seed.charAt(j);
//                if (i == 4 && j != 32){
//                    k++;
//                    i = 0;
//                    if (k == 4){
//                        k = 0;
//                        seed2FA += "\n";
//                    }
//                    else {
//                        seed2FA += "     ";
//                    }
//                }
//            }
//            seedText.setText(seed2FA.toUpperCase());
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

            if (seed != null){
                outState.putString("seed", seed);
            }
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
//                confirm2FAIsShown = false;
//                scanOrCopyIsShown = true;
//                scrollContainer2FA.setVisibility(View.GONE);
//                qrSeedContainer.setVisibility(View.VISIBLE);
//                confirmContainer.setVisibility(View.GONE);
                confirm2FAIsShown = true;
                isEnabled2FA = false;
                scrollContainer2FA.setVisibility(View.GONE);
                scrollContainerVerify.setVisibility(View.VISIBLE);
                scrollContainer2FAEnabled.setVisibility(View.GONE);
                break;
            }
//            case R.id.button_next_2fa:{
//                confirm2FAIsShown = true;
//                scanOrCopyIsShown = false;
//                scrollContainer2FA.setVisibility(View.GONE);
//                qrSeedContainer.setVisibility(View.GONE);
//                confirmContainer.setVisibility(View.VISIBLE);
//                break;
//            }
            case R.id.button_verify_2fa: {
                hideKeyboard();
                if (pin != null){
                    megaApi.multiFactorAuthEnable(pin, this);
                }
                break;
            }
            case R.id.button_export_rk:{
                AccountController aC = new AccountController(this);
                aC.saveRkToFileSystem(false);
                break;
            }
            case R.id.button_dismiss_rk:{
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
                    seedText.setText(seed.toUpperCase());
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
        verify2FAButton.setVisibility(View.GONE);
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
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (seed != null) {
                    ClipData clip = ClipData.newPlainText("seed", seed);
                    if (clip != null){
                        clipboard.setPrimaryClip(clip);
                        showSnackbar(getResources().getString(R.string.messages_copied_clipboard));
                    }
                }
                return true;
            }
        }
        return false;
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
}
