package mega.privacy.android.app.activities.settingsActivities;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CLEAR_OFFLINE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_RESET_VERSION_INFO_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CACHE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FILE_VERSIONS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_OFFLINE_SIZE_SETTING;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RB_SCHEDULER;
import static mega.privacy.android.app.constants.BroadcastConstants.CACHE_SIZE;
import static mega.privacy.android.app.constants.BroadcastConstants.DAYS_COUNT;
import static mega.privacy.android.app.constants.BroadcastConstants.OFFLINE_SIZE;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS;
import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;
import static mega.privacy.android.app.utils.Constants.UPDATE_ACCOUNT_DETAILS;
import static mega.privacy.android.app.utils.Util.getScaleW;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.content.BroadcastReceiver;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsFileManagementFragment;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.listeners.SetAttrUserListener;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.main.tasks.ManageOfflineTask;
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity;
import mega.privacy.android.app.utils.AlertDialogUtil;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaAccountDetails;
import timber.log.Timber;

@AndroidEntryPoint
public class FileManagementPreferencesActivity extends PreferencesBaseActivity {

    private static final int RB_SCHEDULER_MINIMUM_PERIOD = 6;
    private static final int RB_SCHEDULER_MAXIMUM_PERIOD = 31;

    private static final String CLEAR_OFFLINE_SHOWN = "CLEAR_OFFLINE_SHOWN";

    @Inject
    MyAccountInfo myAccountInfo;

    private SettingsFileManagementFragment sttFileManagement;
    private AlertDialog clearOfflineDialog;
    private AlertDialog clearRubbishBinDialog;
    private AlertDialog newFolderDialog;
    private AlertDialog generalDialog;

    private final BroadcastReceiver cacheSizeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagement == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_CACHE_SIZE_SETTING)) {
                String size = intent.getStringExtra(CACHE_SIZE);
                sttFileManagement.setCacheSize(size);
            }
        }
    };

    private final BroadcastReceiver resetVersionInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagement == null)
                return;

            if (intent.getAction().equals(ACTION_RESET_VERSION_INFO_SETTING)) {
                sttFileManagement.resetVersionsInfo();
            }
        }
    };

    private final BroadcastReceiver offlineSizeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagement == null)
                return;

            if (intent.getAction().equals(ACTION_UPDATE_OFFLINE_SIZE_SETTING)) {
                String size = intent.getStringExtra(OFFLINE_SIZE);
                sttFileManagement.setOfflineSize(size);
            }
        }
    };

    private final BroadcastReceiver updateCUSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagement == null)
                return;

            if (intent.getAction().equals(ACTION_REFRESH_CLEAR_OFFLINE_SETTING)) {
                sttFileManagement.taskGetSizeOffline();
            }
        }
    };

    private final BroadcastReceiver updateMyAccountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagement == null)
                return;

            if (intent.getAction().equals(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)) {
                int actionType = intent.getIntExtra(ACTION_TYPE, INVALID_VALUE);
                if (actionType == UPDATE_ACCOUNT_DETAILS) {
                    if (!isFinishing()) {
                        sttFileManagement.setRubbishInfo();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver updateRBSchedulerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagement == null)
                return;

            long daysCount = intent.getLongExtra(DAYS_COUNT, INVALID_VALUE);

            if (daysCount != INVALID_VALUE) {
                sttFileManagement.updateRBScheduler(daysCount);
            }
        }
    };

    private final BroadcastReceiver updateFileVersionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || sttFileManagement == null)
                return;

            sttFileManagement.updateEnabledFileVersions();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings_file_management_category);

        if (savedInstanceState == null) {
            sttFileManagement = new SettingsFileManagementFragment();
            replaceFragment(sttFileManagement);
        } else {
            sttFileManagement = (SettingsFileManagementFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        }

        registerReceiver(cacheSizeUpdateReceiver,
                new IntentFilter(ACTION_UPDATE_CACHE_SIZE_SETTING));

        registerReceiver(offlineSizeUpdateReceiver,
                new IntentFilter(ACTION_UPDATE_OFFLINE_SIZE_SETTING));

        registerReceiver(updateMyAccountReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS));

        IntentFilter filterUpdateCUSettings =
                new IntentFilter(BROADCAST_ACTION_INTENT_SETTINGS_UPDATED);
        filterUpdateCUSettings.addAction(ACTION_REFRESH_CLEAR_OFFLINE_SETTING);
        registerReceiver(updateCUSettingsReceiver, filterUpdateCUSettings);

        registerReceiver(resetVersionInfoReceiver,
                new IntentFilter(ACTION_RESET_VERSION_INFO_SETTING));

        registerReceiver(updateRBSchedulerReceiver,
                new IntentFilter(ACTION_UPDATE_RB_SCHEDULER));

        registerReceiver(updateFileVersionsReceiver,
                new IntentFilter(ACTION_UPDATE_FILE_VERSIONS));

        if (savedInstanceState != null && savedInstanceState.getBoolean(CLEAR_OFFLINE_SHOWN, false)) {
            showClearOfflineDialog();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(CLEAR_OFFLINE_SHOWN, AlertDialogUtil.isAlertDialogShown(clearOfflineDialog));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(cacheSizeUpdateReceiver);
        unregisterReceiver(offlineSizeUpdateReceiver);
        unregisterReceiver(updateMyAccountReceiver);
        unregisterReceiver(updateCUSettingsReceiver);
        unregisterReceiver(resetVersionInfoReceiver);
        unregisterReceiver(updateRBSchedulerReceiver);
        unregisterReceiver(updateFileVersionsReceiver);

        AlertDialogUtil.dismissAlertDialogIfExists(clearOfflineDialog);
    }

    /**
     * Show Clear Offline confirmation dialog.
     */
    public void showClearOfflineDialog() {
        clearOfflineDialog = new MaterialAlertDialogBuilder(this)
                .setMessage(StringResourcesUtils.getString(R.string.clear_offline_confirmation))
                .setPositiveButton(StringResourcesUtils.getString(R.string.general_clear), (dialog, whichButton) ->
                        new ManageOfflineTask(true).execute())
                .setNegativeButton(StringResourcesUtils.getString(R.string.general_dismiss), null)
                .create();

        clearOfflineDialog.show();
    }

    /**
     * Show Clear Rubbish Bin dialog.
     */
    public void showClearRubbishBinDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(StringResourcesUtils.getString(R.string.context_clear_rubbish));
        builder.setMessage(StringResourcesUtils.getString(R.string.clear_rubbish_confirmation));

        builder.setPositiveButton(StringResourcesUtils.getString(R.string.general_clear),
                (dialog, whichButton) -> {
                    NodeController nC = new NodeController(this);
                    nC.cleanRubbishBin();
                });
        builder.setNegativeButton(StringResourcesUtils.getString(android.R.string.cancel), null);
        clearRubbishBinDialog = builder.create();
        clearRubbishBinDialog.show();
    }

    /**
     * Show confirmation clear all versions dialog.
     */
    public void showConfirmationClearAllVersions() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(StringResourcesUtils.getString(R.string.settings_file_management_delete_versions));
        builder.setMessage(StringResourcesUtils.getString(R.string.text_confirmation_dialog_delete_versions));

        builder.setPositiveButton(StringResourcesUtils.getString(R.string.context_delete),
                (dialog, whichButton) -> {
                    NodeController nC = new NodeController(this);
                    nC.clearAllVersions();
                });
        builder.setNegativeButton(StringResourcesUtils.getString(android.R.string.cancel), null);
        clearRubbishBinDialog = builder.create();
        clearRubbishBinDialog.show();
    }

    /**
     * Show Rubbish bin not disabled dialog.
     */
    public void showRBNotDisabledDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_two_vertical_buttons, null);
        builder.setView(v);

        TextView title = v.findViewById(R.id.dialog_title);
        title.setText(StringResourcesUtils.getString(R.string.settings_rb_scheduler_enable_title));
        TextView text = v.findViewById(R.id.dialog_text);
        text.setText(StringResourcesUtils.getString(R.string.settings_rb_scheduler_alert_disabling));

        Button firstButton = v.findViewById(R.id.dialog_first_button);
        firstButton.setText(StringResourcesUtils.getString(R.string.button_plans_almost_full_warning));
        firstButton.setOnClickListener(v1 -> {
            generalDialog.dismiss();
            startActivity(new Intent(this, UpgradeAccountActivity.class));
            myAccountInfo.setUpgradeOpenedFrom(MyAccountInfo.UpgradeFrom.SETTINGS);
        });

        Button secondButton = v.findViewById(R.id.dialog_second_button);
        secondButton.setText(StringResourcesUtils.getString(R.string.button_not_now_rich_links));
        secondButton.setOnClickListener(v12 -> generalDialog.dismiss());

        generalDialog = builder.create();
        generalDialog.show();
    }

    /**
     * Update the Rubbish bin Scheduler value.
     *
     * @param value the new value.
     */
    public void setRBSchedulerValue(String value) {
        Timber.d("Value: %s", value);
        int intValue = Integer.parseInt(value);

        if (megaApi != null) {
            megaApi.setRubbishBinAutopurgePeriod(intValue, new SetAttrUserListener(this));
        }
    }

    /**
     * Method for controlling the selected option on the RbSchedulerValueDialog.
     *
     * @param value The value.
     * @param input The EditText.
     */
    private void controlOptionOfRbSchedulerValueDialog(String value, final EditText input) {
        if (value.length() == 0) {
            return;
        }

        try {
            int daysCount = Integer.parseInt(value);
            boolean isNotFree = myAccountInfo.getAccountType() > MegaAccountDetails.ACCOUNT_TYPE_FREE;

            if ((isNotFree && daysCount > RB_SCHEDULER_MINIMUM_PERIOD)
                    || (daysCount > RB_SCHEDULER_MINIMUM_PERIOD && daysCount < RB_SCHEDULER_MAXIMUM_PERIOD)) {
                setRBSchedulerValue(value);
                newFolderDialog.dismiss();
            } else {
                clearInputText(input);
            }
        } catch (Exception e) {
            clearInputText(input);
        }
    }

    /**
     * Method for resetting the EditText values
     *
     * @param input The EditText.
     */
    private void clearInputText(final EditText input) {
        input.setText("");
        input.requestFocus();
    }

    /**
     * Method required to reset the rubbish bin info.
     */
    public void resetRubbishInfo() {
        if (sttFileManagement != null) {
            sttFileManagement.resetRubbishInfo();
        }
    }

    /**
     * Show Rubbish bin scheduler value dialog.
     */
    public void showRbSchedulerValueDialog(final boolean isEnabling) {
        DisplayMetrics outMetrics = getOutMetrics();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(input, params);

        input.setSingleLine();
        input.setTextColor(ColorUtils.getThemeColor(this, android.R.attr.textColorSecondary));
        input.setHint(StringResourcesUtils.getString(R.string.hint_days));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                controlOptionOfRbSchedulerValueDialog(v.getText().toString().trim(), input);
                return true;
            }
            return false;
        });
        input.setImeActionLabel(StringResourcesUtils.getString(R.string.general_create), EditorInfo.IME_ACTION_DONE);
        input.requestFocus();

        final TextView text = new TextView(FileManagementPreferencesActivity.this);
        if (myAccountInfo.getAccountType() > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
            text.setText(StringResourcesUtils.getString(R.string.settings_rb_scheduler_enable_period_PRO));
        } else {
            text.setText(StringResourcesUtils.getString(R.string.settings_rb_scheduler_enable_period_FREE));
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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(StringResourcesUtils.getString(R.string.settings_rb_scheduler_select_days_title));
        builder.setPositiveButton(StringResourcesUtils.getString(R.string.general_ok),
                (dialog, whichButton) -> {

                });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
            if (isEnabling && sttFileManagement != null) {
                sttFileManagement.updateRBScheduler(0);
            }
        });
        builder.setView(layout);
        newFolderDialog = builder.create();
        newFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        newFolderDialog.show();

        newFolderDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> controlOptionOfRbSchedulerValueDialog(input.getText().toString().trim(), input));
    }
}
