package mega.privacy.android.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RelativeLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mega.privacy.android.app.components.transferWidget.TransferWidget;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;

import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.PENDING_TAB;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.TRANSFERS_TAB;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class TransfersManagementActivity extends PinActivityLollipop {

    protected TransferWidget transfersWidget;

    /**
     * Broadcast to update the transfers widget.
     */
    protected BroadcastReceiver transfersUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWidget(intent);
        }
    };

    /**
     * Registers the transfers BroadcastReceiver.
     */
    protected void registerTransfersReceiver() {
        registerReceiver(transfersUpdateReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE));
    }

    /**
     * Sets a view as transfers widget.
     *
     * @param transfersWidgetLayout RelativeLayout view to set
     */
    protected void setTransfersWidgetLayout(RelativeLayout transfersWidgetLayout) {
        setTransfersWidgetLayout(transfersWidgetLayout, null);
    }

    /**
     * Sets a view as transfers widget.
     *
     * @param transfersWidgetLayout RelativeLayout view to set
     * @param context               current Context, only used to identify if the view belongs to the ManagerActivity
     */
    protected void setTransfersWidgetLayout(RelativeLayout transfersWidgetLayout, Context context) {
        transfersWidget = new TransferWidget(this, transfersWidgetLayout);
        transfersWidgetLayout.findViewById(R.id.transfers_button).setOnClickListener(v -> {
            if (context instanceof ManagerActivityLollipop) {
                ManagerActivityLollipop.setDrawerItem(ManagerActivityLollipop.DrawerItem.TRANSFERS);
                ((ManagerActivityLollipop) context).selectDrawerItemLollipop(ManagerActivityLollipop.getDrawerItem());
            } else {
                openTransfersSection();
            }
            if (isOnTransferOverQuota()) {
                MegaApplication.getTransfersManagement().setHasNotToBeShowDueToTransferOverQuota(true);
            }
        });
    }

    /**
     * Defines the click action of the transfers widget.
     * Launches an Intent to navigate to In progress tab in Transfers section.
     */
    protected void openTransfersSection() {
        if (megaApi.isLoggedIn() == 0 || dbH.getCredentials() == null) {
            logWarning("No logged in, no action.");
            return;
        }

        Intent intent = new Intent(this, ManagerActivityLollipop.class);
        intent.setAction(ACTION_SHOW_TRANSFERS);
        intent.putExtra(TRANSFERS_TAB, PENDING_TAB);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Updates the state of the transfers widget when the correspondent LocalBroadcast has been received.
     *
     * @param intent    Intent received in the LocalBroadcast
     */
    protected void updateWidget(Intent intent) {
        if (intent == null) return;

        if (transfersWidget != null) {
            int transferType = intent.getIntExtra(TRANSFER_TYPE, INVALID_VALUE);
            if (transferType == INVALID_VALUE) {
                transfersWidget.update();
            } else {
                transfersWidget.update(transferType);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (transfersWidget != null) {
            transfersWidget.update();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            //If transfersUpdateReceiver is not registered, it throws an IllegalArgumentException
            unregisterReceiver(transfersUpdateReceiver);
        } catch (IllegalArgumentException e) {
            logWarning("IllegalArgumentException unregistering transfersUpdateReceiver", e);
        }
    }
}
