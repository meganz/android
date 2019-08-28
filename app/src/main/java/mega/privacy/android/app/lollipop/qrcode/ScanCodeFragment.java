package mega.privacy.android.app.lollipop.qrcode;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
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
import android.widget.TextView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaUser;

import static android.graphics.Color.WHITE;
import static mega.privacy.android.app.utils.CacheFolderManager.*;

//import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by mega on 22/01/18.
 */

public class ScanCodeFragment extends Fragment implements /*ZXingScannerView.ResultHandler, */View.OnClickListener{

    public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150;
    public static int WIDTH = 500;
    public static String EXTRA_CONTACTS = "extra_contacts";
    public static String EXTRA_NODE_HANDLE = "node_handle";
    public static String EXTRA_MEGA_CONTACTS = "mega_contacts";

    private ActionBar aB;

    private Context context;

//    public static ZXingScannerView scannerView;
    public CodeScanner codeScanner;
    CodeScannerView codeScannerView;

    private AlertDialog inviteAlertDialog;
    private AlertDialog requestedAlertDialog;
    private MegaRequest request;

    private Button invite;
    private Button view;
    private RoundedImageView avatarImage;
    private TextView initialLetter;
    private TextView contactName;
    private TextView contactMail;
    private TextView invalidCode;

    private TextView dialogTitle;
    private TextView dialogText;
    private Button dialogButton;

    public String myEmail;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH = null;
    Handler handler;
    long handle = -1;
    long handleContactLink = -1;
    private boolean success = true;

    private boolean inviteShown = false;
    private boolean dialogshown = false;
    public int dialogTitleContent = -1;
    public int dialogTextContent = -1;
    private String contactNameContent;

    private boolean isContact = false;

    private Bitmap avatarSave;
    private String initialLetterSave;
    private boolean contentAvatar = false;

    private MegaUser userQuery;

    public static ScanCodeFragment newInstance() {
        log("newInstance");
        ScanCodeFragment fragment = new ScanCodeFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isContact = savedInstanceState.getBoolean("isContact", false);
            inviteShown = savedInstanceState.getBoolean("inviteShown", false);
            dialogshown = savedInstanceState.getBoolean("dialogshown", false);
            dialogTitleContent = savedInstanceState.getInt("dialogTitleContent", -1);
            dialogTextContent = savedInstanceState.getInt("dialogTextContent", -1);
            contactNameContent = savedInstanceState.getString("contactNameContent");
            myEmail = savedInstanceState.getString("myEmail");
            success = savedInstanceState.getBoolean("success", true);
            handleContactLink = savedInstanceState.getLong("handleContactLink", 0);

            byte[] avatarByteArray = savedInstanceState.getByteArray("avatar");
            if (avatarByteArray != null){
                avatarSave = BitmapFactory.decodeByteArray(avatarByteArray, 0, avatarByteArray.length);
                contentAvatar = savedInstanceState.getBoolean("contentAvatar", false);
                if (!contentAvatar){
                    initialLetterSave = savedInstanceState.getString("initialLetter");
                }
            }
        }

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(context);
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

//        scannerView = new ZXingScannerView(getActivity());

        View view = inflater.inflate(R.layout.fragment_scan_code, container, false);

        invalidCode = (TextView) view.findViewById(R.id.invalid_code_text);
        invalidCode.setVisibility(View.GONE);

        codeScannerView = (CodeScannerView) view.findViewById(R.id.scanner_view);
        codeScanner = new CodeScanner(context, codeScannerView);
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                ((QRCodeActivity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invite(result);
                    }
                });
            }
        });
        codeScannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                codeScanner.startPreview();
                if (invalidCode.getVisibility() == View.VISIBLE) {
                    invalidCode.setVisibility(View.GONE);
                }
            }
        });

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if(aB!=null){
            aB.setTitle(getString(R.string.section_qr_code));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }

        if (inviteShown){
            showInviteDialog();
        }
        else if (dialogshown){
            showAlertDialog(dialogTitleContent, dialogTextContent, success);
        }

//        return scannerView;
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (inviteShown){
            outState.putBoolean("inviteShown",inviteShown);
            outState.putString("contactNameContent", contactNameContent);
            outState.putBoolean("isContact", isContact);
        }
        if (dialogshown){
            outState.putBoolean("dialogshown", dialogshown);
            outState.putInt("dialogTitleContent", dialogTitleContent);
            outState.putInt("dialogTextContent", dialogTextContent);
        }
        if (dialogshown || inviteShown){
            outState.putString("myEmail", myEmail);
            outState.putBoolean("success", success);
            outState.putLong("handleContactLink", handleContactLink);
            if (avatarImage != null){
                avatarImage.buildDrawingCache(true);
                Bitmap avatarBitmap = avatarImage.getDrawingCache(true);

                ByteArrayOutputStream avatarOutputStream = new ByteArrayOutputStream();
                avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, avatarOutputStream);
                byte[] avatarByteArray = avatarOutputStream.toByteArray();
                outState.putByteArray("avatar", avatarByteArray);
                outState.putBoolean("contentAvatar", contentAvatar);
            }
            if (!contentAvatar){
                outState.putString("initialLetter", initialLetter.getText().toString());
            }
        }
    }

    @Override
    public void onStart(){
        log("onStart");
        super.onStart();
//        scannerView.setAutoFocus(true);
//        scannerView.startCamera();
    }

    @Override
    public void onResume() {
        log("onResume");
        super.onResume();
//        scannerView.setAutoFocus(true);
//        scannerView.startCamera();
//        scannerView.setResultHandler(this);
        codeScanner.startPreview();
    }

    @Override
    public void onPause() {
        log("onPause");
        super.onPause();
//        scannerView.stopCamera();
        codeScanner.releaseResources();
    }

//    @Override
//    public void handleResult(Result rawResult) {
//        log("handleResult");
//
//        invite(rawResult);
//    }

    public void showAlertDialog (int title, int text, final boolean success) {
//        scannerView.stopCamera();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_invite, null);
        builder.setView(v);

        dialogTitle = (TextView) v.findViewById(R.id.dialog_invite_title);
        dialogText = (TextView) v.findViewById(R.id.dialog_invite_text);
        dialogButton = (Button) v.findViewById(R.id.dialog_invite_button);
        dialogButton.setOnClickListener(this);
        this.success = success;

        if (dialogTitleContent == -1){
            dialogTitleContent = title;
        }
        if (dialogTextContent == -1) {
            dialogTextContent = text;
        }
        dialogTitle.setText(getResources().getString(dialogTitleContent));
        if (success){
            dialogText.setText(getResources().getString(dialogTextContent, myEmail));
        }
        else {
            dialogText.setText(getResources().getString(dialogTextContent));
        }

        requestedAlertDialog = builder.create();
        requestedAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (success){
//                    scannerView.stopCamera();
                    dialogshown = false;
                    codeScanner.releaseResources();
                    getActivity().finish();
                }
                else {
                    codeScanner.startPreview();
                }
            }
        });
        dialogshown = true;
        requestedAlertDialog.show();
    }

    public void invite (Result rawResult){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        String contactLink = rawResult.getText();
        String[] s = contactLink.split("C!");

        codeScanner.startPreview();
        if (s.length<=1){
            invalidCode.setVisibility(View.VISIBLE);
        }
        else if (!s[0].equals("https://mega.nz/")) {
            invalidCode.setVisibility(View.VISIBLE);
        }
        else{
            invalidCode.setVisibility(View.GONE);
            handle = MegaApiAndroid.base64ToHandle(s[1].trim());
            log("Contact link: "+contactLink+ " s[1]: "+s[1]+" handle: "+handle);
            if (megaApi == null){
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
            }
            megaApi.contactLinkQuery(handle, (QRCodeActivity) context);

            View v = inflater.inflate(R.layout.dialog_accept_contact, null);
            builder.setView(v);

            invite = (Button) v.findViewById(R.id.accept_contact_invite);
            invite.setOnClickListener(this);
            view = (Button) v.findViewById(R.id.view_contact);
            view.setOnClickListener(this);

            avatarImage = (RoundedImageView) v.findViewById(R.id.accept_contact_avatar);
            initialLetter = (TextView) v.findViewById(R.id.accept_contact_initial_letter);
            contactName = (TextView) v.findViewById(R.id.accept_contact_name);
            contactMail = (TextView) v.findViewById(R.id.accept_contact_mail);

            inviteAlertDialog = builder.create();
            inviteAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    log("onDismiss");
//                scannerView.setAutoFocus(true);
//                scannerView.startCamera();
//                onResume();
                    inviteShown = false;
                    codeScanner.startPreview();
                }
            });

//        scannerView.stopCamera();
        }
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
                inviteShown = false;
                sendInvitation();
                if (inviteAlertDialog != null){
                    inviteAlertDialog.dismiss();
                }
                break;
            }
            case R.id.dialog_invite_button: {
                dialogshown = false;
//                scannerView.stopCamera();
                codeScanner.releaseResources();
                if (requestedAlertDialog != null){
                    requestedAlertDialog.dismiss();
                }
                if (success){
                    getActivity().finish();
                }
                else {
//                    scannerView.setAutoFocus(true);
//                    scannerView.startCamera();
//                    onResume();
                    codeScanner.startPreview();
                }
                break;
            }
            case R.id.view_contact: {
                inviteShown = false;
                codeScanner.releaseResources();
                if (inviteAlertDialog != null){
                    inviteAlertDialog.dismiss();
                }
                Intent intent = new Intent(context, ContactInfoActivityLollipop.class);
                intent.putExtra("name", myEmail);
                startActivity(intent);
                ((QRCodeActivity) context).finish();
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

        if (!isContact){
            log("setAvatar is not Contact");
            setDefaultAvatar();
        }
        else {

            log("setAvatar is Contact");
            File avatar = null;
            if(context!=null){
                log("context is not null");
                avatar = buildAvatarFile(context,myEmail + ".jpg");
            }
            else{
                log("context is null!!!");
                if(getActivity()!=null){
                    log("getActivity is not null");
                    avatar = buildAvatarFile(getActivity(),myEmail + ".jpg");
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
                    contentAvatar = true;
                }
            }
        }else{
            log("my avatar NOT exists!");
            log("Call to getUserAvatar");
            log("DO NOT Retry!");
            megaApi.getUserAvatar(myEmail, avatar.getPath(), (QRCodeActivity) context);
//            setDefaultAvatar();
        }
    }

    public void setDefaultAvatar(){
        log("setDefaultAvatar");
        Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        if (isContact && userQuery != null){
            String color = megaApi.getUserAvatarColor(userQuery);
            if(color!=null){
                log("The color to set the avatar is "+color);
                p.setColor(Color.parseColor(color));
            }
            else{
                log("Default color to the avatar");
                p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
            }
        }
        else {
            p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
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

        String fullName = "";
        if(contactNameContent != null){
            fullName = contactNameContent;
        }
        else{
            //No name, ask for it and later refresh!!
            fullName = myEmail;
        }
        if (fullName != null && fullName.length() > 0) {
            String firstLetter = fullName.charAt(0) + "";
            firstLetter = firstLetter.toUpperCase(Locale.getDefault());

            initialLetter.setText(firstLetter);
            initialLetter.setTextSize(30);
            initialLetter.setTextColor(WHITE);
            initialLetter.setVisibility(View.VISIBLE);
            contentAvatar = false;
        }
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

    void showInviteDialog (){
        if (inviteAlertDialog != null){
            contactName.setText(contactNameContent);
            if (isContact){
                contactMail.setText(getResources().getString(R.string.context_contact_already_exists, myEmail));
                invite.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
            }
            else {
                contactMail.setText(myEmail);
                invite.setVisibility(View.VISIBLE);
                view.setVisibility(View.GONE);
            }
            setAvatar();
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View v = inflater.inflate(R.layout.dialog_accept_contact, null);
            builder.setView(v);
            invite = (Button) v.findViewById(R.id.accept_contact_invite);
            invite.setOnClickListener(this);
            view = (Button) v.findViewById(R.id.view_contact);
            view.setOnClickListener(this);

            avatarImage = (RoundedImageView) v.findViewById(R.id.accept_contact_avatar);
            initialLetter = (TextView) v.findViewById(R.id.accept_contact_initial_letter);
            contactName = (TextView) v.findViewById(R.id.accept_contact_name);
            contactMail = (TextView) v.findViewById(R.id.accept_contact_mail);

            if (avatarSave != null){
                avatarImage.setImageBitmap(avatarSave);
                if (contentAvatar){
                    initialLetter.setVisibility(View.GONE);
                }
                else {
                    if (initialLetterSave != null) {
                        initialLetter.setText(initialLetterSave);
                        initialLetter.setTextSize(30);
                        initialLetter.setTextColor(WHITE);
                        initialLetter.setVisibility(View.VISIBLE);
                    }
                    else {
                        setAvatar();
                    }
                }
            }
            else {
                setAvatar();
            }

            if (isContact){
                contactMail.setText(getResources().getString(R.string.context_contact_already_exists, myEmail));
                invite.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
            }
            else {
                contactMail.setText(myEmail);
                invite.setVisibility(View.VISIBLE);
                view.setVisibility(View.GONE);
            }
            inviteAlertDialog = builder.create();
            inviteAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    log("onDismiss");
                    inviteShown = false;
                    codeScanner.startPreview();
                }
            });
            contactName.setText(contactNameContent);
//            setAvatar();
        }
        inviteAlertDialog.show();
        inviteShown = true;
    }

    MegaUser queryIfIsContact() {

        ArrayList<MegaUser> contacts = megaApi.getContacts();

        for (int i=0; i<contacts.size(); i++){
            if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){
                log("Contact mail[i]="+i+":"+contacts.get(i).getEmail()+" contact mail request: "+myEmail);
                if (contacts.get(i).getEmail().equals(myEmail)){
                    isContact = true;
                    return contacts.get(i);
                }
            }
        }
        isContact = false;
        return null;
    }

    public void initDialogInvite(MegaRequest request, MegaError e){
        if (e.getErrorCode() == MegaError.API_OK) {
            log("Contact link query " + request.getNodeHandle() + "_" + MegaApiAndroid.handleToBase64(request.getNodeHandle()) + "_" + request.getEmail() + "_" + request.getName() + "_" + request.getText());
            handleContactLink = request.getNodeHandle();
            contactNameContent = request.getName() + " " + request.getText();
            myEmail = request.getEmail();
            userQuery = queryIfIsContact();
            showInviteDialog();
        } else if (e.getErrorCode() == MegaError.API_EEXIST) {
            dialogTitleContent = R.string.invite_not_sent;
            dialogTextContent = R.string.invite_not_sent_text_already_contact;
            showAlertDialog(dialogTitleContent, dialogTextContent, true);
        } else {
            dialogTitleContent = R.string.invite_not_sent;
            dialogTextContent = R.string.invite_not_sent_text;
            showAlertDialog(dialogTitleContent, dialogTextContent, false);
        }
    }
}
