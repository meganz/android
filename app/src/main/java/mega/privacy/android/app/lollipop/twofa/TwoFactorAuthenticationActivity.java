package mega.privacy.android.app.lollipop.twofa;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
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
import mega.privacy.android.app.lollipop.PinActivityLollipop;
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

    private ScrollView scrollContainer2FA;
    private RelativeLayout qrSeedContainer;
    private RelativeLayout confirmContainer;
    private Button next2FAButton;
    private Button setup2FAButton;
    private Button verify2FAButton;
    private ImageView qrImage;
    private TextView seedText;

    private String seed = null;

    InputMethodManager imm;
    private EditTextPIN firstPin;
    private EditTextPIN secondPin;
    private EditTextPIN thirdPin;
    private EditTextPIN fourthPin;
    private EditTextPIN fifthPin;
    private EditTextPIN sixthPin;
    private StringBuilder sb = new StringBuilder();

    private boolean scanOrCopyIsShown = false;
    private boolean confirm2FAIsShown = false;

    MegaApiAndroid megaApi;

    Bitmap qr = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        setContentView(R.layout.activity_two_factor_authentication);

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
            seed = savedInstanceState.getString("seed");
            byte[] qrByteArray = savedInstanceState.getByteArray("qr");
            if (qrByteArray != null){
                qr = BitmapFactory.decodeByteArray(qrByteArray, 0, qrByteArray.length);
            }
        }
        else {
            confirm2FAIsShown = false;
            scanOrCopyIsShown = false;
        }

        scrollContainer2FA = (ScrollView) findViewById(R.id.scroll_container_2fa);
        setup2FAButton = (Button) findViewById(R.id.button_enable_2fa);
        setup2FAButton.setOnClickListener(this);
        next2FAButton = (Button) findViewById(R.id.button_next_2fa);
        next2FAButton.setOnClickListener(this);
        verify2FAButton = (Button) findViewById(R.id.button_verify_2fa);
        verify2FAButton.setOnClickListener(this);
        qrSeedContainer = (RelativeLayout) findViewById(R.id.container_qr_2fa);
        confirmContainer = (RelativeLayout) findViewById(R.id.container_confirm_2fa);
        qrImage = (ImageView) findViewById(R.id.qr_2fa);
        seedText = (TextView) findViewById(R.id.seed_2fa);
        seedText.setOnLongClickListener(this);

        if (confirm2FAIsShown){
            scrollContainer2FA.setVisibility(View.GONE);
            qrSeedContainer.setVisibility(View.GONE);
            confirmContainer.setVisibility(View.VISIBLE);
        }
        else if (scanOrCopyIsShown){
            scrollContainer2FA.setVisibility(View.GONE);
            qrSeedContainer.setVisibility(View.VISIBLE);
            confirmContainer.setVisibility(View.GONE);

            if (seed != null){
                log("seed no null");
                seedText.setText(seed);
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

                    secondPin.setText("");
                    thirdPin.setText("");
                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
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

                    thirdPin.setText("");
                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
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

                    fourthPin.setText("");
                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
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

                    fifthPin.setText("");
                    sixthPin.setText("");
                }
                else {
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

                    sixthPin.setText("");
                }
                else {
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
                    sixthPin.setCursorVisible(false);
                    verify2FAButton.setVisibility(View.VISIBLE);
                    hideKeyboard();

                    if (firstPin.length() == 1 && secondPin.length() == 1 && thirdPin.length() == 1 && fourthPin.length() == 1 && fifthPin.length() == 1 && sixthPin.length() == 1){
                        sb.append(firstPin.getText());
                        sb.append(secondPin.getText());
                        sb.append(thirdPin.getText());
                        sb.append(fourthPin.getText());
                        sb.append(fifthPin.getText());
                        sb.append(sixthPin.getText());
                    }
                }
                else {
                    verify2FAButton.setVisibility(View.GONE);
                }
            }
        });

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
            String seed2FA = "";
            int i = 0;
            int k = 0;
            for (int j=0; j<seed.length(); j++){
                i++;
                seed2FA += seed.charAt(j);
                if (i == 4 && j != 32){
                    k++;
                    i = 0;
                    if (k == 4){
                        k = 0;
                        seed2FA += "\n";
                    }
                    else {
                        seed2FA += "     ";
                    }
                }
            }
            seedText.setText(seed2FA.toUpperCase());
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

            qrImage.setImageBitmap(qr);
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

        if (scanOrCopyIsShown){
            log("scanOrCopyIsShown");
            if (qr != null) {
                log("QR no null");
                ByteArrayOutputStream qrOutputStream = new ByteArrayOutputStream();
                qr.compress(Bitmap.CompressFormat.JPEG, 100, qrOutputStream);
                byte[] qrByteArray = qrOutputStream.toByteArray();
                outState.putByteArray("qr", qrByteArray);
            }

            if (seedText != null){
                outState.putString("seed", seedText.getText().toString());
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
                confirm2FAIsShown = false;
                scanOrCopyIsShown = true;
                scrollContainer2FA.setVisibility(View.GONE);
                qrSeedContainer.setVisibility(View.VISIBLE);
                confirmContainer.setVisibility(View.GONE);
                megaApi.multiFactorAuthGetCode(this);
                break;
            }
            case R.id.button_next_2fa:{
                confirm2FAIsShown = true;
                scanOrCopyIsShown = false;
                scrollContainer2FA.setVisibility(View.GONE);
                qrSeedContainer.setVisibility(View.GONE);
                confirmContainer.setVisibility(View.VISIBLE);
                break;
            }
            case R.id.button_verify_2fa: {

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
                if (seedText != null){
                    generate2FAQR();
                }
            }
            else {
                log("e.getErrorCode(): " + e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public boolean onLongClick(View v) {

        switch (v.getId()){
            case R.id.seed_2fa: {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                String stringClip = seedText.getText().toString();
                if (stringClip != null) {
                    ClipData clip = ClipData.newPlainText("seed", stringClip);
                    if (clip != null){
                        clipboard.setPrimaryClip(clip);
                    }
                }
                return true;
            }
        }
        return false;
    }
}
