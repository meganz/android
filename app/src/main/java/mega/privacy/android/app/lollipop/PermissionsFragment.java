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

import mega.privacy.android.app.PermissionsImageAdapter;
import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class PermissionsFragment extends Fragment implements View.OnClickListener {

    public static final int PERMISSIONS_FRAGMENT = 666;

    final int READ_WRITE = 0;
    final int CAMERA = 1;
    final int CALLS = 2;

    Context context;

    LinearLayout setupLayout;
    Button notNowButton;
    Button setupButton;
    LinearLayout allowAccessLayout;
    PermissionsImageAdapter adapter;
    ImageView imgDisplay;
    TextView itemsText;
    TextView titleDisplay;
    TextView subtitleDisplay;
    LinearLayout itemsLayout;
    Button notNow2Button;
    Button enableButton;

    boolean isAllowingAccessShown = false;
    int permissionsPosition = 0;
    int numItems = 0;
    int[] items = new int[3];
    int currentPermission = 0;
    boolean writeGranted = false;
    boolean readGranted = false;
    boolean cameraGranted = false;
    boolean microphoneGranted = false;
//    boolean writeCallsGranted = false;

    int[] mImages;
    String[] mTitles;
    String[] mSubtitles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        View v = inflater.inflate(R.layout.fragment_permissions, container, false);

        ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO);

        setupLayout = (LinearLayout) v.findViewById(R.id.setup_fragment_container);
        notNowButton = (Button) v.findViewById(R.id.not_now_button);
        notNowButton.setOnClickListener(this);
        setupButton = (Button) v.findViewById(R.id.setup_button);
        setupButton.setOnClickListener(this);
        allowAccessLayout = (LinearLayout) v.findViewById(R.id.allow_access_fragment_container);
        itemsText = (TextView) v.findViewById(R.id.items_text);
        imgDisplay = (ImageView) v.findViewById(R.id.image_permissions);
        titleDisplay = (TextView) v.findViewById(R.id.title_permissions);
        subtitleDisplay = (TextView) v.findViewById(R.id.subtitle_permissions);

        itemsLayout = (LinearLayout) v.findViewById(R.id.items_layout);

        mImages = new int[] {
                R.drawable.photos,
                R.drawable.enable_camera,
                R.drawable.calls,
        };

        mTitles = new String[] {
                context.getString(R.string.allow_acces_media_title),
                context.getString(R.string.allow_acces_camera_title),
                context.getString(R.string.allow_acces_calls_title)
        };

        mSubtitles =  new String[] {
                context.getString(R.string.allow_acces_media_subtitle),
                context.getString(R.string.allow_acces_camera_subtitle),
                context.getString(R.string.allow_acces_calls_subtitle_microphone)
        };


        notNow2Button = (Button) v.findViewById(R.id.not_now_button_2);
        notNow2Button.setOnClickListener(this);
        enableButton = (Button) v.findViewById(R.id.enable_button);
        enableButton.setOnClickListener(this);



        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            numItems = 0;
            permissionsPosition = 0;
            readGranted = ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            writeGranted = ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            cameraGranted = ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.CAMERA);
            microphoneGranted = ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.RECORD_AUDIO);
//            writeCallsGranted = ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.WRITE_CALL_LOG);

            if (!readGranted || !writeGranted) {
                numItems++;
                items[0] = currentPermission = READ_WRITE;
                if (!cameraGranted) {
                    numItems++;
                    items[1] = CAMERA;
                    if (!microphoneGranted/* || !writeCallsGranted*/){
                        numItems++;
                        items[2] = CALLS;
                    }
                }
                else if (!microphoneGranted/* || !writeCallsGranted*/) {
                    numItems++;
                    items[1] = CALLS;
                }

                setContent(READ_WRITE);
            }
            else if (!cameraGranted) {
                numItems++;
                items[0] = currentPermission = CAMERA;
                if (!microphoneGranted/* || !writeCallsGranted*/) {
                    numItems++;
                    items[1] = CALLS;
                }

                setContent(CAMERA);
            }
            else if (!microphoneGranted/* || !writeCallsGranted*/) {
                numItems++;
                items[0] = currentPermission = CALLS;
                setContent(CALLS);
            }

            showSetupLayout();
        }
        else {
            isAllowingAccessShown = savedInstanceState.getBoolean("isAllowingAccessShown", false);
            permissionsPosition = savedInstanceState.getInt("permissionsPosition", 0);
            numItems = savedInstanceState.getInt("numItems", 0);
            currentPermission = savedInstanceState.getInt("currentPermission", 0);
            items = savedInstanceState.getIntArray("items");
            microphoneGranted = savedInstanceState.getBoolean("microphoneGranted", false);
//            writeCallsGranted = savedInstanceState.getBoolean("writeCallsGranted", false);

            setContent(currentPermission);

            if (isAllowingAccessShown) {
                showAllowAccessLayout();
            }
            else {
                showSetupLayout();
            }
        }

        if (isAllowingAccessShown) {
            ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
        }

        if (numItems == 1){
            itemsLayout.setVisibility(View.GONE);
        }
        else {
            itemsText.setText(getString(R.string.wizard_steps_indicator, currentPermission + 1, numItems));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
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

    public void setNextPermission () {
        if (items != null && items.length > 0) {
            for (int i=0; i<numItems; i++) {
                if (items[i] == currentPermission) {
                    if (i+1 < numItems) {
                        permissionsPosition++;
                        currentPermission = items[i+1];
                        setContent(currentPermission);
                        itemsText.setText(getString(R.string.wizard_steps_indicator, currentPermission+1, numItems));
                        break;
                    }
                    else {
                        ((ManagerActivityLollipop) context).destroyPermissionsFragment();
                    }
                }
            }
        }
    }

    void setContent (int permission) {
        imgDisplay.setImageDrawable(ContextCompat.getDrawable(context, mImages[permission]));
        titleDisplay.setText(mTitles[permission]);
        subtitleDisplay.setText(mSubtitles[permission]);
    }

    void askForPermission () {

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
//        if (!microphoneGranted && !writeCallsGranted)  {
//            log("RECORD_AUDIO and WRITE_CALL_LOG");
//            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
//                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_CALL_LOG},
//                    PERMISSIONS_FRAGMENT);
//        }
//        else if (!microphoneGranted) {
        if (!microphoneGranted) {
            logDebug("RECORD_AUDIO");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_FRAGMENT);
        }
//        else if (!writeCallsGranted) {
//            log("WRITE_CALL_LOG");
//            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
//                    new String[]{Manifest.permission.WRITE_CALL_LOG},
//                    PERMISSIONS_FRAGMENT);
//        }
    }

    void showSetupLayout () {
        setupLayout.setVisibility(View.VISIBLE);
        allowAccessLayout.setVisibility(View.GONE);
    }

    void showAllowAccessLayout () {
        isAllowingAccessShown = true;
        setupLayout.setVisibility(View.GONE);
        allowAccessLayout.setVisibility(View.VISIBLE);
    }

    public boolean askingForMicrophoneAndWriteCallsLog () {
        if (!microphoneGranted/* && !writeCallsGranted*/) {
            return true;
        }
        else {
            return false;
        }
    }

    public int getCurrentPermission () {
        return currentPermission;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isAllowingAccessShown", isAllowingAccessShown);
        outState.putInt("permissionsPosition", permissionsPosition);
        outState.putInt("numItems", numItems);
        outState.putInt("currentPermission", currentPermission);
        outState.putIntArray("items", items);
        outState.putBoolean("microphoneGranted", ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.RECORD_AUDIO));
//        outState.putBoolean("writeCallsGranted", ((ManagerActivityLollipop) context).checkPermission(Manifest.permission.WRITE_CALL_LOG));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }
}
