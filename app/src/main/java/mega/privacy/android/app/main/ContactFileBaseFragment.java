package mega.privacy.android.app.main;

import static mega.privacy.android.app.utils.FileUtil.getDownloadLocation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Stack;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.adapters.RotatableAdapter;
import mega.privacy.android.app.main.managerSections.RotatableFragment;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.data.model.MegaPreferences;
import mega.privacy.android.domain.entity.SortOrder;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@AndroidEntryPoint
public class ContactFileBaseFragment extends RotatableFragment {

    public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;

    @Inject
    DatabaseHandler dbH;

    protected MegaApiAndroid megaApi;
    protected ActionBar aB;
    protected Context context;
    protected String userEmail;
    protected MegaUser contact;
    protected long parentHandle = -1;
    protected Stack<Integer> lastPositionStack;
    protected ArrayList<MegaNode> contactNodes;
    protected SortOrder orderGetChildren = SortOrder.ORDER_DEFAULT_ASC;

    protected MegaPreferences prefs = null;
    protected String downloadLocationDefaultPath;
    protected DisplayMetrics outMetrics;

    protected MegaNodeAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("ContactFileBaseFragment onCreate");
        super.onCreate(savedInstanceState);

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (aB == null) {
            aB = ((AppCompatActivity) context).getSupportActionBar();
        }

        prefs = dbH.getPreferences();

        downloadLocationDefaultPath = getDownloadLocation();

        lastPositionStack = new Stack<>();

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
        aB = ((AppCompatActivity) activity).getSupportActionBar();
        if (aB != null) {
            aB.show();
            ((AppCompatActivity) context).invalidateOptionsMenu();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity) context).getSupportActionBar();
        if (aB != null) {
            aB.show();
            ((AppCompatActivity) context).invalidateOptionsMenu();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserEmail() {
        return this.userEmail;
    }

    @Override
    protected RotatableAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void activateActionMode() {
        Timber.d("activateActionMode");
    }

    @Override
    public void multipleItemClick(int position) {
        adapter.toggleSelection(position);
    }

    @Override
    public void reselectUnHandledSingleItem(int position) {
    }

    @Override
    protected void updateActionModeTitle() {
    }
}
