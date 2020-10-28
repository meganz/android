package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.TourImageAdapter;
import mega.privacy.android.app.components.LoopViewPager;
import mega.privacy.android.app.utils.TextUtil;

import static mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS_FROM_LINK;
import static mega.privacy.android.app.utils.Constants.CREATE_ACCOUNT_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class TourFragmentLollipop extends Fragment implements View.OnClickListener{

    private static final String EXTRA_RECOVERY_KEY_URL = "EXTRA_RECOVERY_KEY_URL";

    Context context;
    private TourImageAdapter adapter;
    private LoopViewPager viewPager;
    private ImageView firstItem;
    private ImageView secondItem;
    private ImageView thirdItem;
    private ImageView fourthItem;
    private Button bRegister;
    private Button bLogin;
    private ScrollView baseContainer;

    public static TourFragmentLollipop newInstance(@Nullable String recoveryKeyUrl) {
        TourFragmentLollipop fragment = new TourFragmentLollipop();
        if (recoveryKeyUrl != null && !recoveryKeyUrl.isEmpty()) {
            Bundle arguments = new Bundle();
            arguments.putString(EXTRA_RECOVERY_KEY_URL, recoveryKeyUrl);
            fragment.setArguments(arguments);
        }
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            logError("Context is null");
            return;
        }
    }

    void setStatusBarColor (int position) {
        switch (position) {
            case 0: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_1));
                break;
            }
            case 1: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_2));
                break;
            }
            case 2: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_3));
                break;
            }
            case 3: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_4));
                break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setStatusBarColor(viewPager.getCurrentItem());

        // For small screen like nexus one or bigger screen, this is to force the scroll view to bottom to show buttons
        // Meanwhile, tour image glide could also be shown
        baseContainer.post(new Runnable() {
            @Override
            public void run() {
                if (baseContainer != null) {
                    baseContainer.fullScroll(View.FOCUS_DOWN);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        View v = inflater.inflate(R.layout.fragment_tour, container, false);
        viewPager = (LoopViewPager) v.findViewById(R.id.pager);
        firstItem = (ImageView) v.findViewById(R.id.first_item);
        secondItem = (ImageView) v.findViewById(R.id.second_item);
        thirdItem = (ImageView) v.findViewById(R.id.third_item);
        fourthItem = (ImageView) v.findViewById(R.id.fourth_item);

        baseContainer = (ScrollView) v.findViewById(R.id.tour_fragment_base_container);

        bLogin = (Button) v.findViewById(R.id.button_login_tour);
        bRegister = (Button) v.findViewById(R.id.button_register_tour);

        bLogin.setOnClickListener(this);
        bRegister.setOnClickListener(this);

        adapter = new TourImageAdapter((LoginActivityLollipop)context);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
        setStatusBarColor(0);

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){

            @Override
            public void onPageSelected (int position){

                setStatusBarColor(position);

                switch(position){
                    case 0:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 1:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 2:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 3:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        break;
                    }
                }
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            String recoveryKeyUrl = getArguments().getString(EXTRA_RECOVERY_KEY_URL, null);
            if (recoveryKeyUrl != null) {
                logDebug("Link to resetPass: " + recoveryKeyUrl);
                showRecoveryKeyDialog(recoveryKeyUrl);
            }
        }
    }

    public void showRecoveryKeyDialog(String recoveryKeyUrl) {
        logDebug("link: " + recoveryKeyUrl);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setView(R.layout.dialog_recovery_key);
        dialogBuilder.setTitle(R.string.title_dialog_insert_MK);
        dialogBuilder.setMessage(R.string.text_dialog_insert_MK);
        dialogBuilder.setPositiveButton(R.string.general_ok, null);
        dialogBuilder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        TextInputLayout editInputLayout = dialog.findViewById(R.id.input_recovery_key);
        EditText editText = dialog.findViewById(R.id.edit_recovery_key);
        editText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String key = editText.getText().toString();
                if (TextUtil.isTextEmpty(key)) {
                    editInputLayout.setError(getString(R.string.invalid_string));
                    view.requestFocus();
                } else {
                    startChangePasswordActivity(recoveryKeyUrl, key.trim());
                    dialog.dismiss();
                }
            } else {
                logDebug("Other IME" + actionId);
            }
            return false;
        });

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            String key = editText.getText().toString();
            if (TextUtil.isTextEmpty(key)) {
                editInputLayout.setError(getString(R.string.invalid_string));
                editText.requestFocus();
            } else {
                startChangePasswordActivity(recoveryKeyUrl, key.trim());
                dialog.dismiss();
            }
        });
    }

    private void startChangePasswordActivity(String recoveryKeyUrl, String key) {
        Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
        intent.setAction(ACTION_RESET_PASS_FROM_LINK);
        intent.setData(Uri.parse(recoveryKeyUrl));
        intent.putExtra("MK", key);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.button_register_tour:
                logDebug("onRegisterClick");
                ((LoginActivityLollipop)context).showFragment(CREATE_ACCOUNT_FRAGMENT);
                break;
            case R.id.button_login_tour:
                logDebug("onLoginClick");
                ((LoginActivityLollipop)context).showFragment(LOGIN_FRAGMENT);
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        logDebug("onAttach");
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity context) {
        logDebug("onAttach Activity");
        super.onAttach(context);
        this.context = context;
    }
}
