package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;

public class PermissionsFragment extends Fragment implements View.OnClickListener {

    public static final int PERMISSIONS_FRAGMENT = 666;

    private static final int READ_WRITE = 0;
    private static final int CAMERA = 1;
    private static final int CALLS = 2;
    private static final int CONTACTS = 3;
    private static final int PERMISSION_FLOW_PAGE_SIZE = 4;

    private Context context;

    private LinearLayout setupLayout;
    private LinearLayout allowAccessLayout;
    private ImageView imgDisplay;
    private TextView titleDisplay;
    private TextView subtitleDisplay;
    // only for access contacts permission.
    private TextView explanationDisplay;

    private boolean isAllowingAccessShown;
    private int numItems = 0;
    private int[] items = new int[PERMISSION_FLOW_PAGE_SIZE];
    private int currentPermission = 0;
    private boolean writeGranted;
    private boolean readGranted;
    private boolean cameraGranted;
    private boolean microphoneGranted;
    private boolean contactsGranted;

    private int[] mImages;
    private String[] mTitles;
    private String[] mSubtitles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        View v = inflater.inflate(R.layout.fragment_permissions, container, false);
        ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO);

        setupLayout = v.findViewById(R.id.setup_fragment_container);
        Button notNowButton, setupButton, notNow2Button, enableButton;
        notNowButton = v.findViewById(R.id.not_now_button);
        notNowButton.setOnClickListener(this);
        setupButton = v.findViewById(R.id.setup_button);
        setupButton.setOnClickListener(this);
        allowAccessLayout = v.findViewById(R.id.allow_access_fragment_container);
        imgDisplay = v.findViewById(R.id.image_permissions);
        titleDisplay = v.findViewById(R.id.title_permissions);
        subtitleDisplay = v.findViewById(R.id.subtitle_permissions);
        explanationDisplay = v.findViewById(R.id.subtitle_explanation);

        mImages = new int[]{
                R.drawable.photos,
                R.drawable.enable_camera,
                R.drawable.calls,
                R.drawable.contacts
        };

        mTitles = new String[]{
                context.getString(R.string.allow_acces_media_title),
                context.getString(R.string.allow_acces_camera_title),
                context.getString(R.string.allow_acces_calls_title),
                context.getString(R.string.allow_acces_contact_title)
        };

        mSubtitles = new String[]{
                context.getString(R.string.allow_acces_media_subtitle),
                context.getString(R.string.allow_acces_camera_subtitle),
                context.getString(R.string.allow_acces_calls_subtitle_microphone),
                context.getString(R.string.allow_acces_contact_subtitle)
        };

        notNow2Button = v.findViewById(R.id.not_now_button_2);
        notNow2Button.setOnClickListener(this);
        enableButton = v.findViewById(R.id.enable_button);
        enableButton.setOnClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            numItems = 0;
            readGranted = hasPermissions(this.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
            writeGranted = hasPermissions(this.getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            cameraGranted = hasPermissions(this.getActivity(), Manifest.permission.CAMERA);
            microphoneGranted = hasPermissions(this.getActivity(), Manifest.permission.RECORD_AUDIO);
            contactsGranted = hasPermissions(this.getActivity(), Manifest.permission.READ_CONTACTS);

            if (!readGranted || !writeGranted) {
                items[numItems] = READ_WRITE;
                numItems++;
            }
            if (!cameraGranted) {
                items[numItems] = CAMERA;
                numItems++;
            }
            if (!microphoneGranted) {
                items[numItems] = CALLS;
                numItems++;
            }
            if (!contactsGranted) {
                items[numItems] = CONTACTS;
                numItems++;
            }

            currentPermission = items[0];
            setContent(currentPermission);
            showSetupLayout();
        } else {
            isAllowingAccessShown = savedInstanceState.getBoolean("isAllowingAccessShown", false);
            numItems = savedInstanceState.getInt("numItems", 0);
            currentPermission = savedInstanceState.getInt("currentPermission", 0);
            items = savedInstanceState.getIntArray("items");
            microphoneGranted = savedInstanceState.getBoolean("microphoneGranted", false);

            setContent(currentPermission);

            if (isAllowingAccessShown) {
                showAllowAccessLayout();
            } else {
                showSetupLayout();
            }
        }

        if (isAllowingAccessShown) {
            ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.not_now_button: {
                ((ManagerActivityLollipop) context).destroyPermissionsFragment();
                break;
            }
            case R.id.setup_button: {
                ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
                showAllowAccessLayout();
                break;
            }
            case R.id.not_now_button_2: {
                setNextPermission();
                break;
            }
            case R.id.enable_button: {
                askForPermission();
                break;
            }
        }
    }

    public void setNextPermission() {
        if (items != null && items.length > 0) {
            for (int i = 0; i < numItems; i++) {
                if (items[i] == currentPermission) {
                    if (i + 1 < numItems) {
                        currentPermission = items[i + 1];
                        setContent(currentPermission);
                        break;
                    } else {
                        ((ManagerActivityLollipop) context).destroyPermissionsFragment();
                    }
                }
            }
        }
    }

    void setContent(int permission) {
        imgDisplay.setImageDrawable(ContextCompat.getDrawable(context, mImages[permission]));
        titleDisplay.setText(mTitles[permission]);
        subtitleDisplay.setText(mSubtitles[permission]);
        // access contacts
        if(permission == 3) {
            explanationDisplay.setVisibility(View.VISIBLE);
        } else {
            explanationDisplay.setVisibility(View.GONE);
        }
    }

    void askForPermission() {

        switch (currentPermission) {
            case READ_WRITE: {
                askForMediaPermissions();
                break;
            }
            case CAMERA: {
                askForCameraPermission();
                break;
            }
            case CALLS: {
                askForCallsPermissions();
                break;
            }

            case CONTACTS: {
                askForContactsPermissions();
                break;
            }
        }
    }

    void askForMediaPermissions () {
        if (!readGranted && !writeGranted)  {
            logDebug("WRITE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_FRAGMENT);
        }
        else if (!writeGranted) {
            logDebug("WRITE_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_FRAGMENT);
        }
        else if (!readGranted) {
            logDebug("READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_FRAGMENT);
        }
    }

    void askForCameraPermission() {
        if (!cameraGranted) {
            logDebug("CAMERA");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_FRAGMENT);
        }
    }

    void askForCallsPermissions() {
        if (!microphoneGranted) {
            logDebug("RECORD_AUDIO");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_FRAGMENT);
        }
    }

    void askForContactsPermissions() {
        if (!contactsGranted) {
            logDebug("Ask for CONTACT permission");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_FRAGMENT);
        }
    }

    void showSetupLayout() {
        setupLayout.setVisibility(View.VISIBLE);
        allowAccessLayout.setVisibility(View.GONE);
    }

    void showAllowAccessLayout() {
        isAllowingAccessShown = true;
        setupLayout.setVisibility(View.GONE);
        allowAccessLayout.setVisibility(View.VISIBLE);
    }

    public boolean askingForMicrophoneAndWriteCallsLog() {
        if (!microphoneGranted) {
            return true;
        } else {
            return false;
        }
    }

    public int getCurrentPermission() {
        return currentPermission;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isAllowingAccessShown", isAllowingAccessShown);
        outState.putInt("numItems", numItems);
        outState.putInt("currentPermission", currentPermission);
        outState.putIntArray("items", items);
        outState.putBoolean("microphoneGranted", ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.RECORD_AUDIO));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }
}
