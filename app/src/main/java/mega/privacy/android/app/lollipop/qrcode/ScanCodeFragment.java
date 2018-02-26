package mega.privacy.android.app.lollipop.qrcode;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.app.Activity.RESULT_OK;
import static android.graphics.Color.WHITE;

/**
 * Created by mega on 22/01/18.
 */

public class ScanCodeFragment extends Fragment implements ZXingScannerView.ResultHandler, View.OnClickListener, MegaRequestListenerInterface{

    public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150;
    public static int WIDTH = 500;
    public static String EXTRA_CONTACTS = "extra_contacts";
    public static String EXTRA_NODE_HANDLE = "node_handle";
    public static String EXTRA_MEGA_CONTACTS = "mega_contacts";

    private ActionBar aB;

    private Context context;

    public static ZXingScannerView scannerView;

    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1010;

    private AlertDialog inviteAlertDialog;
    private AlertDialog requestedAlertDialog;
    private MegaRequest request;

    private Button invite;
    private RoundedImageView avatarImage;
    private TextView initialLetter;
    private TextView contactName;
    private TextView contactMail;

    private TextView dialogTitle;
    private TextView dialogText;
    private Button dialogButton;

    MegaUser myUser;
    String myEmail;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH = null;
    Handler handler;
    long handle = -1;
    long handleContactLink = -1;
    private boolean success = true;

    public static ScanCodeFragment newInstance() {
        log("newInstance");
        ScanCodeFragment fragment = new ScanCodeFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");

        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(context);
        handler = new Handler();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        scannerView = new ZXingScannerView(getActivity());

//        TextView textView = new TextView(getActivity());
//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.setMargins(70, 100, 70, 0);
//        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
//        textView.setLayoutParams(layoutParams);
//        textView.setText("Line up the QR code to scan it with your device's camera");
//        textView.setTextColor(getResources().getColor(R.color.white));
//        scannerView.addView(textView);

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if(aB!=null){
            aB.setTitle(getString(R.string.section_qr_code));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }

        return scannerView;
    }

    @Override
    public void onStart(){
        log("onStart");
        super.onStart();
        scannerView.startCamera();
    }

    @Override
    public void onResume() {
        log("onResume");
        super.onResume();
        scannerView.startCamera();
        scannerView.setResultHandler(this);
    }

    @Override
    public void onPause() {
        log("onPause");
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        log("handleResult");

        invite(rawResult);
    }

    public void showAlertDialog (int title, int text, final boolean success) {
        scannerView.stopCamera();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_invite, null);
        builder.setView(v);

        dialogTitle = (TextView) v.findViewById(R.id.dialog_invite_title);
        dialogText = (TextView) v.findViewById(R.id.dialog_invite_text);
        dialogButton = (Button) v.findViewById(R.id.dialog_invite_button);
        dialogButton.setOnClickListener(this);
        this.success = success;
//            dialogTitle.setText(getResources().getString(R.string.invite_accepted));
//            dialogText.setText(getResources().getString(R.string.invite_accepted_text, myEmail));

        if (success){
            dialogTitle.setText(getResources().getString(title));
            dialogText.setText(getResources().getString(text, myEmail));
        }
        else {
            dialogTitle.setText(getResources().getString(title));
            dialogText.setText(getResources().getString(text));
        }

        requestedAlertDialog = builder.create();
        requestedAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (success){
                    scannerView.stopCamera();
                    getActivity().finish();
                }
            }
        });
        requestedAlertDialog.show();
    }

    public void invite (Result rawResult){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        String contactLink = rawResult.getText();
        String[] s = contactLink.split("C!");

        handle = MegaApiAndroid.base64ToHandle(s[1].trim());
        log("Contact link: "+contactLink+ " s[1]: "+s[1]+" handle: "+handle);
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        megaApi.contactLinkQuery(handle, this);

        View v = inflater.inflate(R.layout.dialog_accept_contact, null);
        builder.setView(v);

        invite = (Button) v.findViewById(R.id.accept_contact_invite);
        invite.setOnClickListener(this);

        avatarImage = (RoundedImageView) v.findViewById(R.id.accept_contact_avatar);
        initialLetter = (TextView) v.findViewById(R.id.accept_contact_initial_letter);
        contactName = (TextView) v.findViewById(R.id.accept_contact_name);
        contactMail = (TextView) v.findViewById(R.id.accept_contact_mail);

        inviteAlertDialog = builder.create();
        inviteAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                log("onDismiss");
                scannerView.startCamera();
                onResume();
            }
        });

        scannerView.stopCamera();
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
        Util.log("ScanCodeFragment", log);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.accept_contact_invite: {
                sendInvitation();
                if (inviteAlertDialog != null){
                    inviteAlertDialog.dismiss();
                }
                break;
            }
            case R.id.dialog_invite_button: {
                scannerView.stopCamera();
                if (requestedAlertDialog != null){
                    requestedAlertDialog.dismiss();
                }
                if (success){
                    getActivity().finish();
                }
                else {
                    scannerView.startCamera();
                    onResume();
                }
                break;
            }
        }
    }

    public void sendInvitation () {
        log("sendInvitation");
        megaApi.inviteContact(myEmail, null, MegaContactRequest.INVITE_ACTION_ADD, handleContactLink, (QRCodeActivity) context);
    }

    public void setAvatar(){
        log("updateAvatar");
        File avatar = null;
        if(context!=null){
            log("context is not null");

            if (context.getExternalCacheDir() != null){
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
            }
            else{
                avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
            }
        }
        else{
            log("context is null!!!");
            if(getActivity()!=null){
                log("getActivity is not null");
                if (getActivity().getExternalCacheDir() != null){
                    avatar = new File(getActivity().getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
                }
                else{
                    avatar = new File(getActivity().getCacheDir().getAbsolutePath(), myEmail + ".jpg");
                }
            }
            else{
                log("getActivity is ALSOOO null");
                return;
            }
        }

        if(avatar!=null){
            setProfileAvatar(avatar);
        }
        else{
            setDefaultAvatar();
        }
    }

    public void setProfileAvatar(File avatar){
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
                    setDefaultAvatar();
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
            log("DO NOT Retry!");
            setDefaultAvatar();
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
        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(myUser+""));
        String fullName = "";
        if(contactDB!=null){
            ContactController cC = new ContactController(context);
            fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(),myEmail);
        }
        else{
            //No name, ask for it and later refresh!!
            fullName = myEmail;
        }
        String firstLetter = fullName.charAt(0) + "";
        firstLetter = firstLetter.toUpperCase(Locale.getDefault());

        initialLetter.setText(firstLetter);
        initialLetter.setTextSize(30);
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
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {


//        megaApi.contactLinkQuery(request.getNodeHandle(), this);
        if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_QUERY){
            if (e.getErrorCode() == MegaError.API_OK){
                log("Contact link query " + request.getNodeHandle() + "_" + MegaApiAndroid.handleToBase64(request.getNodeHandle()) + "_" + request.getEmail() + "_" + request.getName() + "_" + request.getText());

                this.request = request;
                handleContactLink = request.getNodeHandle();
                myEmail = request.getEmail();
                myUser = megaApi.getContact(myEmail);
                contactName.setText(request.getName() + " " + request.getText());
                contactMail.setText(request.getEmail());
                setAvatar();

                inviteAlertDialog.show();
            }
            else if (e.getErrorCode() == MegaError.API_EEXIST){
                showAlertDialog(R.string.invite_not_sent, R.string.invite_not_sent_text_already_contact, true);
            }
            else {
                showAlertDialog( R.string.invite_not_sent, R.string.invite_not_sent_text, false);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
