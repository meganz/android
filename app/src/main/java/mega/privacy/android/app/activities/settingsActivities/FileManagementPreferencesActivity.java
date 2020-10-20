package mega.privacy.android.app.activities.settingsActivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.FileManagementSettingsFragment;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MyAccountPageAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyStorageFragmentLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.constants.IntentConstants.EXTRA_UPGRADE_ACCOUNT;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.resetAccountDetailsTimeStamp;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.Util.*;

public class FileManagementPreferencesActivity extends PreferencesBaseActivity implements MegaRequestListenerInterface, MegaGlobalListenerInterface {

    static FileManagementPreferencesActivity activity = null;
    private FileManagementSettingsFragment sttFileManagment;
    private AlertDialog clearRubbishBinDialog;
    private AlertDialog newFolderDialog;
    private AlertDialog generalDialog;

    private BroadcastReceiver cacheSizeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagment == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_CACHE_SIZE_SETTING)) {
                String size = intent.getStringExtra(CACHE_SIZE);
                sttFileManagment.setCacheSize(size);
            }
        }
    };

    private BroadcastReceiver setVersionInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagment == null)
                return;

            if (intent.getAction().equals(ACTION_SET_VERSION_INFO_SETTING)) {
                sttFileManagment.setVersionsInfo();
            }
        }
    };

    private BroadcastReceiver resetVersionInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagment == null)
                return;

            if (intent.getAction().equals(ACTION_RESET_VERSION_INFO_SETTING)) {
                sttFileManagment.resetVersionsInfo();
            }
        }
    };


    private BroadcastReceiver offlineSizeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagment == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_OFFLINE_SIZE_SETTING)) {
                String size = intent.getStringExtra(OFFLINE_SIZE);
                sttFileManagment.setOfflineSize(size);
            }
        }
    };

    private BroadcastReceiver getSizeOfflineReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagment == null)
                return;

            if (intent.getAction().equals(ACTION_GET_SIZE_OFFLINE_SETTING)) {
                sttFileManagment.taskGetSizeOffline();
            }
        }
    };

    private BroadcastReceiver updateMyAccountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null || intent.getAction() == null || sttFileManagment == null)
                return;

            if (intent.getAction().equals(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)) {
                int actionType = intent.getIntExtra(ACTION_TYPE, INVALID_VALUE);
                if (actionType == UPDATE_ACCOUNT_DETAILS) {
                    if (!isFinishing()) {
                        sttFileManagment.setRubbishInfo();
                    }
                }
            }
        }
    };

    private BroadcastReceiver offlineReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagment == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_ONLINE_OPTIONS_SETTING)) {
                boolean isOnline = intent.getBooleanExtra(ONLINE_OPTION, false);
                sttFileManagment.setOnlineOptions(isOnline);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;

        aB.setTitle(getString(R.string.settings_file_management_category).toUpperCase());

        sttFileManagment = new FileManagementSettingsFragment();
        replaceFragment(sttFileManagment);
        registerReceiver(cacheSizeUpdateReceiver, new IntentFilter(ACTION_UPDATE_CACHE_SIZE_SETTING));
        registerReceiver(offlineSizeUpdateReceiver, new IntentFilter(ACTION_UPDATE_OFFLINE_SIZE_SETTING));
        registerReceiver(offlineReceiver, new IntentFilter(ACTION_UPDATE_ONLINE_OPTIONS_SETTING));
        registerReceiver(updateMyAccountReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS));
        registerReceiver(getSizeOfflineReceiver, new IntentFilter(ACTION_GET_SIZE_OFFLINE_SETTING));
        registerReceiver(setVersionInfoReceiver, new IntentFilter(ACTION_SET_VERSION_INFO_SETTING));
        registerReceiver(resetVersionInfoReceiver, new IntentFilter(ACTION_SET_VERSION_INFO_SETTING));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cacheSizeUpdateReceiver);
        unregisterReceiver(offlineSizeUpdateReceiver);
        unregisterReceiver(offlineReceiver);
        unregisterReceiver(updateMyAccountReceiver);
        unregisterReceiver(getSizeOfflineReceiver);
        unregisterReceiver(setVersionInfoReceiver);
        unregisterReceiver(resetVersionInfoReceiver);
    }

    public void showClearRubbishBinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.context_clear_rubbish));
        builder.setMessage(getString(R.string.clear_rubbish_confirmation));

        builder.setPositiveButton(getString(R.string.general_clear),
                (dialog, whichButton) -> {
                    NodeController nC = new NodeController(activity);
                    nC.cleanRubbishBin();
                });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        clearRubbishBinDialog = builder.create();
        clearRubbishBinDialog.show();
    }

    public void showConfirmationClearAllVersions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.settings_file_management_delete_versions));
        builder.setMessage(getString(R.string.text_confirmation_dialog_delete_versions));

        builder.setPositiveButton(getString(R.string.context_delete),
                (dialog, whichButton) -> {
                    NodeController nC = new NodeController(activity);
                    nC.clearAllVersions();
                });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        clearRubbishBinDialog = builder.create();
        clearRubbishBinDialog.show();
    }

    public void showRBNotDisabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_two_vertical_buttons, null);
        builder.setView(v);

        TextView title = v.findViewById(R.id.dialog_title);
        title.setText(getString(R.string.settings_rb_scheduler_enable_title));
        TextView text = v.findViewById(R.id.dialog_text);
        text.setText(getString(R.string.settings_rb_scheduler_alert_disabling));

        Button firstButton = v.findViewById(R.id.dialog_first_button);
        firstButton.setText(getString(R.string.button_plans_almost_full_warning));
        firstButton.setOnClickListener(v1 -> generalDialog.dismiss());

        Button secondButton = v.findViewById(R.id.dialog_second_button);
        secondButton.setText(getString(R.string.button_not_now_rich_links));
        secondButton.setOnClickListener(v12 -> generalDialog.dismiss());

        generalDialog = builder.create();
        generalDialog.show();
    }

    public void setRBSchedulerValue(String value) {
        logDebug("Value: " + value);
        int intValue = Integer.parseInt(value);

        if (megaApi != null) {
            megaApi.setRubbishBinAutopurgePeriod(intValue, this);
        }
    }

    public void showRbSchedulerValueDialog(final boolean isEnabling) {
        logDebug("showRbSchedulerValueDialog");
        DisplayMetrics outMetrics = getOutMetrics();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(input, params);

        input.setSingleLine();
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        input.setHint(getString(R.string.hint_days));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String value = v.getText().toString().trim();
                if (value.length() == 0) {
                    return true;
                }

                try {
                    int daysCount = Integer.parseInt(value);
                    if (((MegaApplication) getApplication()).getMyAccountInfo().getAccountType() > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                        if (daysCount > 6) {
                            setRBSchedulerValue(value);
                            newFolderDialog.dismiss();
                        } else {
                            input.setText("");
                            input.requestFocus();
                        }
                    } else if (daysCount > 6 && daysCount < 31) {
                        setRBSchedulerValue(value);
                        newFolderDialog.dismiss();
                    } else {
                        input.setText("");
                        input.requestFocus();
                    }
                } catch (Exception e) {
                    input.setText("");
                    input.requestFocus();
                }

                return true;
            }
            return false;
        });
        input.setImeActionLabel(getString(R.string.general_create), EditorInfo.IME_ACTION_DONE);
        input.requestFocus();

        final TextView text = new TextView(FileManagementPreferencesActivity.this);
        if (((MegaApplication) getApplication()).getMyAccountInfo().getAccountType() > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
            text.setText(getString(R.string.settings_rb_scheduler_enable_period_PRO));
        } else {
            text.setText(getString(R.string.settings_rb_scheduler_enable_period_FREE));
        }

        float density = getResources().getDisplayMetrics().density;
        float scaleW = getScaleW(outMetrics, density);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, (11 * scaleW));
        layout.addView(text);

        LinearLayout.LayoutParams params_text_error = (LinearLayout.LayoutParams) text.getLayoutParams();
        params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.setMargins(scaleWidthPx(25, outMetrics), 0, scaleWidthPx(25, outMetrics), 0);
        text.setLayoutParams(params_text_error);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.settings_rb_scheduler_select_days_title));
        builder.setPositiveButton(getString(R.string.general_ok),
                (dialog, whichButton) -> {

                });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
            if (isEnabling && sttFileManagment != null) {
                sttFileManagment.updateRBScheduler(0);
            }
        });
        builder.setView(layout);
        newFolderDialog = builder.create();
        newFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        newFolderDialog.show();

        newFolderDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.length() == 0) {
                return;
            }

            try {
                int daysCount = Integer.parseInt(value);
                if (((MegaApplication) getApplication()).getMyAccountInfo().getAccountType() > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                    if (daysCount > 6) {
                        setRBSchedulerValue(value);
                        newFolderDialog.dismiss();
                    } else {
                        input.setText("");
                        input.requestFocus();
                    }
                } else {
                    if (daysCount > 6 && daysCount < 31) {
                        setRBSchedulerValue(value);
                        newFolderDialog.dismiss();
                    } else {
                        input.setText("");
                        input.requestFocus();
                    }
                }
            } catch (Exception e) {
                input.setText("");
                input.requestFocus();
            }
        });
    }

    @Override
    protected void onPostResume() {
        logDebug("onPostResume");
        super.onPostResume();
        activity = this;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        switch (request.getType()) {
            case MegaRequest.TYPE_CLEAN_RUBBISH_BIN:
                if (e.getErrorCode() == MegaError.API_OK) {
                    Util.showSnackbar(activity, getString(R.string.rubbish_bin_emptied));
                    resetAccountDetailsTimeStamp();
                    if (sttFileManagment != null) {
                        sttFileManagment.resetRubbishInfo();
                    }
                } else {
                    Util.showSnackbar(activity, getString(R.string.rubbish_bin_no_emptied));
                }
                break;

            case MegaRequest.TYPE_SET_ATTR_USER:
                if (request.getParamType() == MegaApiJava.USER_ATTR_RUBBISH_TIME) {
                    if (e.getErrorCode() == MegaError.API_OK) {
                        if (sttFileManagment != null) {
                            sttFileManagment.updateRBScheduler(request.getNumber());
                        }
                    } else {
                        Util.showSnackbar(activity, getString(R.string.error_general_nodes));
                    }
                }
                if (request.getParamType() == MegaApiJava.USER_ATTR_DISABLE_VERSIONS) {
                    MegaApplication.setDisableFileVersions(Boolean.valueOf(request.getText()));
                    if (e.getErrorCode() == MegaError.API_OK) {
                        logDebug("File versioning attribute changed correctly");
                    }
                    if (sttFileManagment != null) {
                        sttFileManagment.updateEnabledFileVersions();
                    }
                }
                break;

            case MegaRequest.TYPE_GET_ATTR_USER:
                if (request.getParamType() == MegaApiJava.USER_ATTR_RUBBISH_TIME && sttFileManagment != null) {
                    if (e.getErrorCode() == MegaError.API_ENOENT) {
                        if (((MegaApplication) getApplication()).getMyAccountInfo().getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                            sttFileManagment.updateRBScheduler(30);
                        } else {
                            sttFileManagment.updateRBScheduler(90);
                        }
                    } else {
                        sttFileManagment.updateRBScheduler(request.getNumber());
                    }
                }

                if (request.getParamType() == MegaApiJava.USER_ATTR_DISABLE_VERSIONS && sttFileManagment != null) {
                    MegaApplication.setDisableFileVersions(request.getFlag());
                    sttFileManagment.updateEnabledFileVersions();
                }
                break;
        }

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
        logDebug("onUsersUpdateLollipop");

        if (users != null) {
            logDebug("users.size(): " + users.size());
            for (int i = 0; i < users.size(); i++) {
                MegaUser user = users.get(i);

                if (user != null) {
                    // 0 if the change is external.
                    // >0 if the change is the result of an explicit request
                    // -1 if the change is the result of an implicit request made by the SDK internally

                    if (user.isOwnChange() > 0) {
                        logDebug("isOwnChange!!!: " + user.getEmail());
                    } else {
                        logDebug("Changes: " + user.getChanges());

                        if (megaApi.getMyUser() != null) {
                            if (user.getHandle() == megaApi.getMyUser().getHandle()) {
                                if (user.hasChanged(MegaUser.CHANGE_TYPE_RUBBISH_TIME)) {
                                    megaApi.getRubbishBinAutopurgePeriod(this);
                                }

                                if (user.hasChanged(MegaUser.CHANGE_TYPE_DISABLE_VERSIONS)) {
                                    megaApi.getFileVersionsOption(this);
                                }
                            }
                        }
                    }
                } else {
                    continue;
                }
            }
        }
    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {

    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {

    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {

    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {

    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }
}
