package mega.privacy.android.app.lollipop.qrcode;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.graphics.Color.WHITE;

/**
 * Created by mega on 22/01/18.
 */

public class MyCodeFragment extends Fragment implements View.OnClickListener, MegaRequestListenerInterface {

    public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150;
    public static int WIDTH = 500;

    MegaUser myUser;
    String myEmail;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH = null;
    Handler handler;

    long handle;
    String contactLink = null;

    private ActionBar aB;

    private RelativeLayout relativeQRCode;
    private RoundedImageView avatarImage;
    private TextView initialLetter;
    private ImageView qrcode;
    private TextView qrcode_link;
    private Button qrcode_copy_link;
    private View v;

    private Context context;

    public static MyCodeFragment newInstance() {
        log("newInstance");
        MyCodeFragment fragment = new MyCodeFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null){
            handle = savedInstanceState.getLong("handle");
        }

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        myEmail = megaApi.getMyUser().getEmail();
        myUser = megaApi.getMyUser();
        dbH = DatabaseHandler.getDbHandler(context);
        handler = new Handler();

        megaApi.contactLinkCreate(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong("handle", handle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        v = inflater.inflate(R.layout.fragment_mycode, container, false);

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if(aB!=null){
            aB.setTitle(getString(R.string.section_qr_code));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }

        relativeQRCode = (RelativeLayout) v.findViewById(R.id.qr_code_relative_layout_avatar);
        initialLetter = (TextView) v.findViewById(R.id.qr_code_initial_letter);
        avatarImage = (RoundedImageView) v.findViewById(R.id.qr_code_avatar);
        qrcode = (ImageView) v.findViewById(R.id.qr_code_image);
        qrcode_link = (TextView) v.findViewById(R.id.qr_code_link);
        qrcode_copy_link = (Button) v.findViewById(R.id.qr_code_button_copy_link);
        qrcode_copy_link.setOnClickListener(this);

        updateAvatar(false);

        return v;
    }

    public Bitmap queryQR () {
        Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = null;

        DisplayMetrics displaymetrics = new DisplayMetrics();
        try {
            bitMatrix = new MultiFormatWriter().encode(contactLink, BarcodeFormat.QR_CODE, WIDTH, WIDTH, hints);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
        int w = bitMatrix.getWidth();
        int h = bitMatrix.getHeight();
        int[] pixels = new int[w * h];

        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? getResources().getColor(R.color.lollipop_primary_color) : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);

        return bitmap;
    }

    public void updateAvatar(boolean retry){
        log("updateAvatar");
        File avatar = null;
        String contactEmail = myUser.getEmail();
        if(context!=null){
            log("context is not null");

            if (context.getExternalCacheDir() != null){
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contactEmail + ".jpg");
            }
            else{
                avatar = new File(context.getCacheDir().getAbsolutePath(), contactEmail + ".jpg");
            }
        }
        else{
            log("context is null!!!");
            if(getActivity()!=null){
                log("getActivity is not null");
                if (getActivity().getExternalCacheDir() != null){
                    avatar = new File(getActivity().getExternalCacheDir().getAbsolutePath(), contactEmail + ".jpg");
                }
                else{
                    avatar = new File(getActivity().getCacheDir().getAbsolutePath(), contactEmail + ".jpg");
                }
            }
            else{
                log("getActivity is ALSOOO null");
                return;
            }
        }

        if(avatar!=null){
            setProfileAvatar(avatar, retry);
        }
        else{
            setDefaultAvatar();
        }
    }

    public void setProfileAvatar(File avatar, boolean retry){
        log("setProfileAvatar");

        Bitmap imBitmap = null;
        if (avatar.exists()){
            log("avatar path: "+avatar.getAbsolutePath());
            if (avatar.length() > 0){
                log("my avatar exists!");
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (imBitmap == null) {
                    avatar.delete();
                    log("Call to getUserAvatar");
                    if(retry){
                        log("Retry!");
                        if (context.getExternalCacheDir() != null){
                            megaApi.getUserAvatar(myUser, context.getExternalCacheDir().getAbsolutePath() + "/" + myEmail);
                        }
                        else{
                            megaApi.getUserAvatar(myUser, context.getCacheDir().getAbsolutePath() + "/" + myEmail);
                        }
                    }
                    else{
                        log("DO NOT Retry!");
                        setDefaultAvatar();
                    }
                }
                else{
                    log("Show my avatar");
                    avatarImage.setImageBitmap(imBitmap);
                    initialLetter.setVisibility(View.GONE);
                }
            }
        }else{
            log("my avatar NOT exists!");
            log("Call to getUserAvatar");
            if(retry){
                log("Retry!");
                if (context.getExternalCacheDir() != null){
                    megaApi.getUserAvatar(myUser, context.getExternalCacheDir().getAbsolutePath() + "/" + myEmail);
                }
                else{
                    megaApi.getUserAvatar(myUser, context.getCacheDir().getAbsolutePath() + "/" + myEmail);
                }
            }
            else{
                log("DO NOT Retry!");
                setDefaultAvatar();
            }
        }
    }

    public void setDefaultAvatar(){
        log("setDefaultAvatar");
        Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        String color = megaApi.getUserAvatarColor(myUser);
        if(color!=null){
            log("The color to set the avatar is "+color);
            p.setColor(Color.parseColor(color));
        }
        else{
            log("Default color to the avatar");
            p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        avatarImage.setImageBitmap(defaultAvatar);

        float density = ((Activity) context).getResources().getDisplayMetrics().density;
        int avatarTextSize = getAvatarTextSize(density);
        log("DENSITY: " + density + ":::: " + avatarTextSize);

        if (dbH == null) {
            dbH = DatabaseHandler.getDbHandler(context);
        }
//        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(myUser.getHandle()+""));
        UserCredentials credentials = dbH.getCredentials();
        String fullName = "";
        if(credentials!=null){
//            ContactController cC = new ContactController(context);
//            fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), myEmail);
            fullName = credentials.getFirstName();
            if (fullName == null) {
                fullName = credentials.getLastName();
                if (fullName == null) {
                    fullName = myEmail;
                }
            }
        }
        else{
            //No name, ask for it and later refresh!!
            fullName = myEmail;
        }
        String firstLetter = fullName.charAt(0) + "";
        firstLetter = firstLetter.toUpperCase(Locale.getDefault());

        initialLetter.setText(firstLetter);
        initialLetter.setTextSize(80);
        initialLetter.setTextColor(WHITE);
        initialLetter.setVisibility(View.VISIBLE);
    }

    private int getAvatarTextSize (float density){
        float textSize = 0.0f;

        if (density > 3.0){
            textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
        }
        else if (density > 2.0){
            textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
        }
        else if (density > 1.5){
            textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
        }
        else if (density > 1.0){
            textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
        }
        else if (density > 0.75){
            textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
        }
        else{
            textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f);
        }

        return (int)textSize;
    }

    @Override
    public void onAttach(Activity activity) {
        log("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    @Override
    public void onAttach(Context context) {
        log("onAttach context");
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
    }

    private static void log(String log) {
        Util.log("MyCodeFragment", log);
    }

    @Override
    public void onClick(View v) {
        log("onClick");
        switch (v.getId()) {
            case R.id.qr_code_button_copy_link: {
                copyLink();
                break;
            }
        }
    }

    public void copyLink () {
        log("copyLink");

        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", contactLink);
        clipboardManager.setPrimaryClip(clip);
        showSnackbar(getString(R.string.qrcode_link_copied));
    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(v, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE) {
            if (e.getErrorCode() == MegaError.API_OK) {
                log("Contact link create LONG: " + request.getNodeHandle());
                log("Contact link create BASE64: " + "https://mega.nz/C!" + MegaApiAndroid.handleToBase64(request.getNodeHandle()));

                handle = request.getNodeHandle();
                contactLink = "https://mega.nz/C!" + MegaApiAndroid.handleToBase64(request.getNodeHandle());
                qrcode_link.setText(contactLink);
                qrcode.setImageBitmap(queryQR());
            }
        }

//        megaApi.contactLinkQuery(request.getNodeHandle(), this);
        if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_QUERY){
            if (e.getErrorCode() == MegaError.API_OK){
                log("Contact link query " + request.getNodeHandle() + "_" + MegaApiAndroid.handleToBase64(request.getNodeHandle()) + "_" + request.getEmail() + "_" + request.getName() + "_" + request.getText());
            }
        }

//        megaApi.contactLinkDelete(request.getNodeHandle(), this);
        if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_DELETE){
            if (e.getErrorCode() == MegaError.API_OK){
                log("Contact link delete:" + e.getErrorCode() + "_" + request.getNodeHandle() + "_"  + MegaApiAndroid.handleToBase64(request.getNodeHandle()));
                ((QRCodeActivity) context).resetSuccessfully(true);
            }
            else {
                ((QRCodeActivity) context).resetSuccessfully(false);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    public void resetQRCode () {
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        megaApi.contactLinkDelete(handle, this);
        megaApi.contactLinkCreate(this);
    }
}
