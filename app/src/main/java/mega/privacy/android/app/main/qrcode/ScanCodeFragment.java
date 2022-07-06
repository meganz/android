package mega.privacy.android.app.main.qrcode;

import static android.graphics.Color.WHITE;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

public class ScanCodeFragment extends Fragment implements View.OnClickListener {

    public static int DEFAULT_AVATAR_WIDTH_HEIGHT = 150;
    public static int WIDTH = 500;
    public static String EXTRA_CONTACTS = "extra_contacts";
    public static String EXTRA_NODE_HANDLE = "node_handle";
    public static String EXTRA_MEGA_CONTACTS = "mega_contacts";
    public static String PRINT_EMAIL = "PRINT_EMAIL";

    // Bug #14988: disableLocalCamera() may hasn't completely released the camera resource as
    // the megaChatApi.disableVideo() is async call. A simply way to solve the issue is
    // setErrorCallback for CodeScanner. If error occurs, retry in 300ms. Retry 5 times max.
    private static final int START_PREVIEW_RETRY = 5;
    private static final int START_PREVIEW_DELAY = 300;
    private int mStartPreviewRetried = 0;

    private ActionBar aB;

    private Context context;

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
    private boolean printEmail;

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
        Timber.d("newInstance");
        ScanCodeFragment fragment = new ScanCodeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");

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
            printEmail = savedInstanceState.getBoolean(PRINT_EMAIL, false);
            handleContactLink = savedInstanceState.getLong("handleContactLink", 0);

            byte[] avatarByteArray = savedInstanceState.getByteArray("avatar");
            if (avatarByteArray != null) {
                avatarSave = BitmapFactory.decodeByteArray(avatarByteArray, 0, avatarByteArray.length);
                contentAvatar = savedInstanceState.getBoolean("contentAvatar", false);
                if (!contentAvatar) {
                    initialLetterSave = savedInstanceState.getString("initialLetter");
                }
            }
        }

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(context);
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        View view = inflater.inflate(R.layout.fragment_scan_code, container, false);

        invalidCode = (TextView) view.findViewById(R.id.invalid_code_text);
        invalidCode.setVisibility(View.GONE);

        codeScannerView = (CodeScannerView) view.findViewById(R.id.scanner_view);
        codeScanner = new CodeScanner(context, codeScannerView);
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                ((QRCodeActivity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        invite(result);
                    }
                });
            }
        });
        codeScanner.setErrorCallback(error -> {
            Timber.w("Start preview error:%s, retry:%d", error.getMessage(), mStartPreviewRetried + 1);
            if (mStartPreviewRetried++ < START_PREVIEW_RETRY) {
                handler.postDelayed(() -> {
                    codeScanner.startPreview();
                }, START_PREVIEW_DELAY);
            } else {
                Timber.e("Start preview failed");
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

        if (aB == null) {
            aB = ((AppCompatActivity) context).getSupportActionBar();
        }

        if (aB != null) {
            aB.setTitle(StringResourcesUtils.getString(R.string.section_qr_code));

            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }

        if (inviteShown) {
            showInviteDialog();
        } else if (dialogshown) {
            showAlertDialog(dialogTitleContent, dialogTextContent, success, printEmail);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (inviteShown) {
            outState.putBoolean("inviteShown", inviteShown);
            outState.putString("contactNameContent", contactNameContent);
            outState.putBoolean("isContact", isContact);
        }
        if (dialogshown) {
            outState.putBoolean("dialogshown", dialogshown);
            outState.putInt("dialogTitleContent", dialogTitleContent);
            outState.putInt("dialogTextContent", dialogTextContent);
        }
        if (dialogshown || inviteShown) {
            outState.putString("myEmail", myEmail);
            outState.putBoolean("success", success);
            outState.putBoolean(PRINT_EMAIL, printEmail);
            outState.putLong("handleContactLink", handleContactLink);
            if (avatarImage != null) {
                avatarImage.buildDrawingCache(true);
                Bitmap avatarBitmap = avatarImage.getDrawingCache(true);

                ByteArrayOutputStream avatarOutputStream = new ByteArrayOutputStream();
                avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, avatarOutputStream);
                byte[] avatarByteArray = avatarOutputStream.toByteArray();
                outState.putByteArray("avatar", avatarByteArray);
                outState.putBoolean("contentAvatar", contentAvatar);
            }
            if (!contentAvatar) {
                outState.putString("initialLetter", initialLetter.getText().toString());
            }
        }
    }

    @Override
    public void onStart() {
        Timber.d("onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Timber.d("onResume");
        super.onResume();
        codeScanner.startPreview();
    }

    @Override
    public void onPause() {
        Timber.d("onPause");
        super.onPause();
        codeScanner.releaseResources();
    }

    /**
     * Method to display an alert dialog just after scan QR code and send the contact invitation to
     * communicate the operation result to the user.
     *
     * @param title      String resource ID of the dialog title.
     * @param text       String resource ID of the dialog message.
     * @param success    Flag to indicate if the operation finished with success or not.
     * @param printEmail Flag to indicate if the dialog message includes contact email or not.
     */
    public void showAlertDialog(int title, int text, final boolean success, final boolean printEmail) {
        if (requestedAlertDialog == null) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View v = inflater.inflate(R.layout.dialog_invite, null);
            builder.setView(v);

            requestedAlertDialog = builder.create();
            requestedAlertDialog.setOnDismissListener(dialog -> {
                if (success) {
                    dialogshown = false;
                    codeScanner.releaseResources();
                    getActivity().finish();
                } else {
                    codeScanner.startPreview();
                }
            });

            dialogTitle = v.findViewById(R.id.dialog_invite_title);
            dialogText = v.findViewById(R.id.dialog_invite_text);
            dialogButton = v.findViewById(R.id.dialog_invite_button);
            dialogButton.setOnClickListener(this);
        }
        this.success = success;
        this.printEmail = printEmail;

        if (dialogTitleContent == -1) {
            dialogTitleContent = title;
        }
        if (dialogTextContent == -1) {
            dialogTextContent = text;
        }
        dialogTitle.setText(StringResourcesUtils.getString(dialogTitleContent));
        if (printEmail) {
            dialogText.setText(StringResourcesUtils.getString(dialogTextContent, myEmail));
        } else {
            dialogText.setText(StringResourcesUtils.getString(dialogTextContent));
        }
        dialogshown = true;
        requestedAlertDialog.show();
    }

    public void invite(Result rawResult) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        String contactLink = rawResult.getText();
        String[] s = contactLink.split("C!");

        codeScanner.startPreview();
        if (s.length <= 1) {
            invalidCode.setVisibility(View.VISIBLE);
        } else if (!s[0].equals("https://mega.nz/")) {
            invalidCode.setVisibility(View.VISIBLE);
        } else {
            invalidCode.setVisibility(View.GONE);
            handle = MegaApiAndroid.base64ToHandle(s[1].trim());
            Timber.d("Contact link: %s s[1]: %s handle: %d", contactLink, s[1], handle);
            if (megaApi == null) {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
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
                    Timber.d("onDismiss");
                    inviteShown = false;
                    codeScanner.startPreview();
                }
            });
        }
    }

    @Override
    public void onAttach(Activity activity) {
        Timber.d("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity) activity).getSupportActionBar();
    }

    @Override
    public void onAttach(Context context) {
        Timber.d("onAttach context");
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.accept_contact_invite: {
                inviteShown = false;
                sendInvitation();
                if (inviteAlertDialog != null) {
                    inviteAlertDialog.dismiss();
                }
                break;
            }
            case R.id.dialog_invite_button: {
                dialogshown = false;
                codeScanner.releaseResources();
                if (requestedAlertDialog != null) {
                    requestedAlertDialog.dismiss();
                }
                if (success) {
                    getActivity().finish();
                } else {
                    codeScanner.startPreview();
                }
                break;
            }
            case R.id.view_contact: {
                inviteShown = false;
                codeScanner.releaseResources();
                if (inviteAlertDialog != null) {
                    inviteAlertDialog.dismiss();
                }
                ContactUtil.openContactInfoActivity(context, myEmail);
                ((QRCodeActivity) context).finish();
                break;
            }
        }
    }

    public void sendInvitation() {
        Timber.d("sendInvitation");
        megaApi.inviteContact(myEmail, null, MegaContactRequest.INVITE_ACTION_ADD, handleContactLink, (QRCodeActivity) context);
    }

    public void setAvatar() {
        Timber.d("updateAvatar");

        if (!isContact) {
            Timber.d("Is not Contact");
            setDefaultAvatar();
        } else {

            Timber.d("Is Contact");
            File avatar = null;
            if (context != null) {
                Timber.d("Context is not null");
                avatar = buildAvatarFile(context, myEmail + ".jpg");
            } else {
                Timber.w("Context is null!!!");
                if (getActivity() != null) {
                    Timber.d("getActivity is not null");
                    avatar = buildAvatarFile(getActivity(), myEmail + ".jpg");
                } else {
                    Timber.w("getActivity is ALSO null");
                    return;
                }
            }

            if (avatar != null) {
                setProfileAvatar(avatar);
            } else {
                setDefaultAvatar();
            }
        }
    }

    public void setProfileAvatar(File avatar) {
        Timber.d("setProfileAvatar");

        Bitmap imBitmap = null;
        if (avatar.exists()) {
            Timber.d("Avatar path: %s", avatar.getAbsolutePath());
            if (avatar.length() > 0) {
                Timber.d("My avatar exists!");
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                imBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (imBitmap == null) {
                    avatar.delete();
                    Timber.d("Call to getUserAvatar");
                    setDefaultAvatar();
                } else {
                    Timber.d("Show my avatar");
                    avatarImage.setImageBitmap(imBitmap);
                    initialLetter.setVisibility(View.GONE);
                    contentAvatar = true;
                }
            }
        } else {
            Timber.d("My avatar NOT exists!");
            Timber.d("Call to getUserAvatar");
            Timber.d("DO NOT Retry!");
            megaApi.getUserAvatar(myEmail, avatar.getPath(), (QRCodeActivity) context);
        }
    }

    public void setDefaultAvatar() {
        Timber.d("setDefaultAvatar");
        Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT, DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        if (isContact && userQuery != null) {
            String color = megaApi.getUserAvatarColor(userQuery);
            if (color != null) {
                Timber.d("The color to set the avatar is %s", color);
                p.setColor(Color.parseColor(color));
            } else {
                Timber.d("Default color to the avatar");
                p.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));
            }
        } else {
            p.setColor(ContextCompat.getColor(context, R.color.red_600_red_300));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth() / 2;
        else
            radius = defaultAvatar.getHeight() / 2;

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);
        avatarImage.setImageBitmap(defaultAvatar);

        float density = ((Activity) context).getResources().getDisplayMetrics().density;
        int avatarTextSize = getAvatarTextSize(density);
        Timber.d("DENSITY: %s:::: %d", density, avatarTextSize);

        String fullName = "";
        if (contactNameContent != null) {
            fullName = contactNameContent;
        } else {
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

    private int getAvatarTextSize(float density) {
        float textSize = 0.0f;

        if (density > 3.0) {
            textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
        } else if (density > 2.0) {
            textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
        } else if (density > 1.5) {
            textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
        } else if (density > 1.0) {
            textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
        } else if (density > 0.75) {
            textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
        } else {
            textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f);
        }

        return (int) textSize;
    }

    void showInviteDialog() {
        if (inviteAlertDialog != null) {
            contactName.setText(contactNameContent);
            if (isContact) {
                contactMail.setText(getResources().getString(R.string.context_contact_already_exists, myEmail));
                invite.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
            } else {
                contactMail.setText(myEmail);
                invite.setVisibility(View.VISIBLE);
                view.setVisibility(View.GONE);
            }
            setAvatar();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
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

            if (avatarSave != null) {
                avatarImage.setImageBitmap(avatarSave);
                if (contentAvatar) {
                    initialLetter.setVisibility(View.GONE);
                } else {
                    if (initialLetterSave != null) {
                        initialLetter.setText(initialLetterSave);
                        initialLetter.setTextSize(30);
                        initialLetter.setTextColor(WHITE);
                        initialLetter.setVisibility(View.VISIBLE);
                    } else {
                        setAvatar();
                    }
                }
            } else {
                setAvatar();
            }

            if (isContact) {
                contactMail.setText(getResources().getString(R.string.context_contact_already_exists, myEmail));
                invite.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
            } else {
                contactMail.setText(myEmail);
                invite.setVisibility(View.VISIBLE);
                view.setVisibility(View.GONE);
            }
            inviteAlertDialog = builder.create();
            inviteAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Timber.d("onDismiss");
                    inviteShown = false;
                    codeScanner.startPreview();
                }
            });
            contactName.setText(contactNameContent);
        }
        inviteAlertDialog.show();
        inviteShown = true;
    }

    MegaUser queryIfIsContact() {

        ArrayList<MegaUser> contacts = megaApi.getContacts();

        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                Timber.d("Contact mail[i]=%d:%s contact mail request: %s", i, contacts.get(i).getEmail(), myEmail);
                if (contacts.get(i).getEmail().equals(myEmail)) {
                    isContact = true;
                    return contacts.get(i);
                }
            }
        }
        isContact = false;
        return null;
    }

    public void initDialogInvite(MegaRequest request, MegaError e) {
        if (e.getErrorCode() == MegaError.API_OK) {
            Timber.d("Contact link query %d_%s_%s_%s_%s", request.getNodeHandle(), MegaApiAndroid.handleToBase64(request.getNodeHandle()), request.getEmail(), request.getName(), request.getText());
            handleContactLink = request.getNodeHandle();
            contactNameContent = request.getName() + " " + request.getText();
            myEmail = request.getEmail();
            userQuery = queryIfIsContact();
            showInviteDialog();
        } else if (e.getErrorCode() == MegaError.API_EEXIST) {
            dialogTitleContent = R.string.invite_not_sent;
            dialogTextContent = R.string.invite_not_sent_text_already_contact;
            showAlertDialog(dialogTitleContent, dialogTextContent, true, true);
        } else {
            dialogTitleContent = R.string.invite_not_sent;
            dialogTextContent = R.string.invite_not_sent_text;
            showAlertDialog(dialogTitleContent, dialogTextContent, false, false);
        }
    }
}
