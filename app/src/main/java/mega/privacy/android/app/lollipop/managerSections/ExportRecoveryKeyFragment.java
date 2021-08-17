package mega.privacy.android.app.lollipop.managerSections;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE;
import static mega.privacy.android.app.utils.LogUtil.*;

import com.google.android.material.button.MaterialButton;

public class ExportRecoveryKeyFragment extends Fragment implements View.OnClickListener{

    private DatabaseHandler dbH;
    private MegaApiAndroid megaApi;
    private Context context;

    private MaterialButton printMK;
    private MaterialButton copyMK;
    private MaterialButton saveMK;
    private LinearLayout MKLayout;

    DisplayMetrics outMetrics;

    public static ExportRecoveryKeyFragment newInstance() {
        logDebug("newInstance");
        return new ExportRecoveryKeyFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.export_mk_layout, container, false);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        printMK = v.findViewById(R.id.print_MK_button);
        printMK.setOnClickListener(this);

        copyMK = v.findViewById(R.id.copy_MK_button);
        copyMK.setOnClickListener(this);

        saveMK = v.findViewById(R.id.save_MK_button);
        saveMK.setOnClickListener(this);

        MKLayout = v.findViewById(R.id.MK_buttons_layout);
        MKLayout.post(() -> {
            if (isOverOneLine()) {
                verticalLayout();
            }
        });

        return v;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        logDebug("onAttach context");
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        AccountController aC = new AccountController(context);
        switch (v.getId()) {
            case R.id.print_MK_button:
                hideMKLayout();
                aC.printRK();
                break;
            case R.id.copy_MK_button:
                hideMKLayout();
                aC.copyMK(false);
                break;
            case R.id.save_MK_button:
                if (checkStoragePermission()) {
                    toFileSystem();
                }
                break;
        }
    }

    private void toFileSystem() {
        hideMKLayout();
        AccountController aC = new AccountController(context);
        aC.saveRkToFileSystem();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toFileSystem();
            } else {
                logWarning("Don't grant the write storage permission when export RK.");
            }
        }
    }

    private void hideMKLayout() {
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).hideMKLayout();
        }
    }

    /**
     * Determine if one of those buttons show the content in greater than one line
     *
     * @return if one of those buttons show the content in greater than one line return true, else false
     */
    private Boolean isOverOneLine() {
        return printMK.getLineCount() > 1 || copyMK.getLineCount() > 1 || saveMK.getLineCount() > 1;
    }

    /**
     * Change the layout to vertical
     */
    private void verticalLayout() {
        MKLayout.setOrientation(LinearLayout.VERTICAL);
        updateViewParam(copyMK);
        updateViewParam(saveMK);
        updateViewParam(printMK);
    }

    /**
     * Update the param for the button
     *
     * @param view the target view need to update
     */
    private void updateViewParam(MaterialButton view) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.setMarginStart(0);
        view.setLayoutParams(params);
        view.setStrokeWidth(0);
        view.setPadding(0, 0, 0, 0);
        view.setGravity(Gravity.START);
        view.setGravity(Gravity.CENTER_VERTICAL);
    }
}
