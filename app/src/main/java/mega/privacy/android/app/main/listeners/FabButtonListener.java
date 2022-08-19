package mega.privacy.android.app.main.listeners;

import static mega.privacy.android.app.modalbottomsheet.UploadBottomSheetDialogFragment.GENERAL_UPLOAD;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_FAB;
import static mega.privacy.android.app.utils.Util.isOnline;

import android.content.Context;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.ContactFileListActivity;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.utils.Util;
import timber.log.Timber;

public class FabButtonListener implements FloatingActionButton.OnClickListener {

    Context context;
    DrawerItem drawerItem;

    public FabButtonListener(Context context) {
        Timber.d("FabButtonListener created");
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        Timber.d("FabButtonListener");
        switch (v.getId()) {
            case R.id.floating_button: {
                Timber.d("Floating Button click!");
                if (context instanceof ManagerActivity) {
                    drawerItem = ((ManagerActivity) context).getDrawerItem();
                    switch (drawerItem) {
                        case CLOUD_DRIVE:
                            Timber.d("Cloud Drive SECTION");
                            if (!isOnline(context)) {
                                if (context instanceof ManagerActivity) {
                                    ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                                }
                                return;
                            }
                            ((ManagerActivity) context).showUploadPanelForBackup(GENERAL_UPLOAD, ACTION_BACKUP_FAB);
                            break;
                        case SEARCH:
                        case SHARED_ITEMS: {
                            Timber.d("Cloud Drive SECTION");
                            if (!isOnline(context)) {
                                if (context instanceof ManagerActivity) {
                                    ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                                }
                                return;
                            }
                            ((ManagerActivity) context).showUploadPanel();
                            break;
                        }
                        case CHAT: {
                            Timber.d("Create new chat");
                            if (!Util.isFastDoubleClick()) {
                                if (((ManagerActivity) context).isMeetingTabShown()) {
                                    ((ManagerActivity) context).showMeetingOptionsPanel(true);
                                } else {
                                    ((ManagerActivity) context).chooseAddContactDialog();
                                }
                            }
                            break;
                        }
                    }
                }
                break;
            }
            case R.id.floating_button_contact_file_list: {
                if (!isOnline(context)) {
                    if (context instanceof ContactFileListActivity) {
                        ((ContactFileListActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
                    }
                    return;
                }
                if (context instanceof ContactFileListActivity) {
                    ((ContactFileListActivity) context).showUploadPanel();
                }
                break;
            }
        }
    }
}
