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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ExportRecoveryKeyFragment extends Fragment implements View.OnClickListener{

    private DatabaseHandler dbH;
    private MegaApiAndroid megaApi;
    private Context context;

    private Button printMK;
    private Button copyMK;
    private Button saveMK;

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
}
