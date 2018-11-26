package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import mega.privacy.android.app.PermissionsImageAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

public class PermissionsFragment extends Fragment implements View.OnClickListener {

    Context context;

    LinearLayout setupLayout;
    Button notNowButton;
    Button setupButton;
    LinearLayout allowAccessLayout;
    PermissionsImageAdapter adapter;
    ViewPager viewPager;
    ImageView firstItem;
    ImageView secondItem;
    ImageView thirdItem;
    Button notNow2Button;
    Button enableButton;

    boolean isAllowingAccessShown = false;


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

        ((ManagerActivityLollipop) context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_SEARCH);

        setupLayout = (LinearLayout) v.findViewById(R.id.setup_fragment_container);
        notNowButton = (Button) v.findViewById(R.id.not_now_button);
        notNowButton.setOnClickListener(this);
        setupButton = (Button) v.findViewById(R.id.setup_button);
        setupButton.setOnClickListener(this);
        allowAccessLayout = (LinearLayout) v.findViewById(R.id.allow_access_fragment_container);
        viewPager = (ViewPager) v.findViewById(R.id.pager);
        adapter = new PermissionsImageAdapter((ManagerActivityLollipop) context);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        firstItem = (ImageView) v.findViewById(R.id.first_item);
        secondItem = (ImageView) v.findViewById(R.id.second_item);
        thirdItem = (ImageView) v.findViewById(R.id.third_item);
        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));

        viewPager.getLayoutParams().height = Util.px2dp(428, metrics);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){

            @Override
            public void onPageSelected (int position){
                switch(position){
                    case 0:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 1:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 2:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        break;
                    }
                }
            }
        });

        notNow2Button = (Button) v.findViewById(R.id.not_now_button_2);
        notNow2Button.setOnClickListener(this);
        enableButton = (Button) v.findViewById(R.id.enable_button);
        enableButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            isAllowingAccessShown = savedInstanceState.getBoolean("isAllowingAccessShown", false);
        }
        else {
            isAllowingAccessShown = false;
        }

        if (isAllowingAccessShown) {
            showAllowAccessLayout();
        }
        else {
            showSetupLayout();
        }

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.not_now_button_2 :
            case R.id.not_now_button: {
                ((ManagerActivityLollipop) context).destroyPermissionsFragment();
                break;
            }
            case R.id.setup_button: {
                showAllowAccessLayout();
                break;
            }
            case R.id.enable_button: {
                askForPermission(viewPager.getCurrentItem());
                break;
            }
        }
    }

    void askForPermission (int page) {
        switch (page) {
            case 0: {
                askForMediaPermissions();
                break;
            }
            case 1: {
                askForCameraAndMicrophonePermissions();
                break;
            }
            case 2: {
                askForNotificationsPermissions();
                break;
            }
        }
    }

    void askForMediaPermissions () {
        boolean askForWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        boolean askForRead  = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        if (askForWrite && askForRead)  {
            log("WRITE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.REQUEST_READ_WRITE_STORAGE);
        }
        else if (askForWrite) {
            log("WRITE_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.REQUEST_WRITE_STORAGE);
        }
        else if (askForRead) {
            log("READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constants.REQUEST_READ_STORAGE);
        }
    }

    void askForCameraAndMicrophonePermissions () {
        boolean askForCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        boolean askForMicrophone = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
        if (askForCamera && askForMicrophone) {
            log("CAMERA and RECORD_AUDIO");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, Constants.REQUEST_CAMERA);
        }
        else if (askForCamera) {
            log("CAMERA");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA);
        }
        else if (askForMicrophone) {
            log("RECORD_AUDIO");
            ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.RECORD_AUDIO}, Constants.RECORD_AUDIO);
        }
    }

    void askForNotificationsPermissions () {

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isAllowingAccessShown", isAllowingAccessShown);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private static void log(String log) {
        Util.log("PermissionsFragment", log);
    }
}
