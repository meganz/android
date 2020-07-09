package mega.privacy.android.app.components.transferWidget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaTransfer.*;

public class TransferWidget {

    private Context context;
    private MegaApiAndroid megaApi;

    private RelativeLayout transfersWidget;
    private ImageButton button;
    private ProgressBar progressBar;
    private ImageView status;

    public TransferWidget(Context context, RelativeLayout transfersWidget) {
        this.context = context;
        megaApi = MegaApplication.getInstance().getMegaApi();

        this.transfersWidget = transfersWidget;
        this.transfersWidget.setVisibility(GONE);
        button = transfersWidget.findViewById(R.id.transfers_button);
        progressBar = transfersWidget.findViewById(R.id.transfers_progress);
        status = transfersWidget.findViewById(R.id.transfers_status);
    }

    /**
     * Hides the widget.
     */
    public void hide() {
        transfersWidget.setVisibility(GONE);
    }

    /**
     * Updates the view of the widget without indicating the type of transfer.
     */
    public void update() {
        update(-1);
    }

    /**
     * Updates the view of the widget taking into account the type of the transfer.
     *
     * @param transferType  type of the transfer:
     *                          - -1 if no type
     *                          - MegaTransfer.TYPE_DOWNLOAD if download transfer
     *                          - MegaTransfer.TYPE_UPLOAD if upload transfer
     */
    public void update(int transferType) {
        if (!isOnline(context)) return;

        if (context instanceof ManagerActivityLollipop) {
            if (ManagerActivityLollipop.getDrawerItem() == ManagerActivityLollipop.DrawerItem.TRANSFERS) {
                MegaApplication.getTransfersManagement().setFailedTransfers(false);
            }

            if (!isOnFileManagementManagerSection()) {
                return;
            }
        }

        if (getPendingTransfers() > 0) {
            setProgress(getProgress(), transferType);
            updateState();
        } else if (MegaApplication.getTransfersManagement().thereAreFailedTransfers()) {
            setFailedTransfers();
        } else {
            hide();
        }
    }

    /**
     * Checks if the widget is on a file management section in ManagerActivity.
     *
     * @return True if the widget is on a file management section in ManagerActivity, false otherwise.
     */
    private boolean isOnFileManagementManagerSection() {
        ManagerActivityLollipop.DrawerItem drawerItem = ManagerActivityLollipop.getDrawerItem();

        return drawerItem != ManagerActivityLollipop.DrawerItem.TRANSFERS
                && drawerItem != ManagerActivityLollipop.DrawerItem.CONTACTS
                && drawerItem != ManagerActivityLollipop.DrawerItem.ACCOUNT
                && drawerItem != ManagerActivityLollipop.DrawerItem.SETTINGS
                && drawerItem != ManagerActivityLollipop.DrawerItem.NOTIFICATIONS;
    }

    /**
     * Updates the state of the widget.
     */
    public void updateState() {
        if (megaApi.areTransfersPaused(TYPE_DOWNLOAD) || megaApi.areTransfersPaused(TYPE_UPLOAD)) {
            setPausedTransfers();
        } else if (isOnTransferOverQuota() && megaApi.getNumPendingUploads() <= 0){
            setOverQuotaTransfers();
        } else {
            setProgressTransfers();
        }
    }

    /**
     * Sets the state of the widget as in progress.
     * If some transfer failed, a warning icon indicates it.
     */
    private void setProgressTransfers() {
        if (MegaApplication.getTransfersManagement().thereAreFailedTransfers()) {
            updateStatus(getDrawable(R.drawable.ic_transfers_error));
        } else if (isOnTransferOverQuota()) {
            updateStatus(getDrawable(R.drawable.ic_transfers_overquota));
        } else if (status.getVisibility() != GONE){
            status.setVisibility(GONE);
        }

        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_progress_bar));
    }

    /**
     * Sets the state of the widget as paused.
     */
    private void setPausedTransfers() {
        if (isOnTransferOverQuota()) return;

        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_progress_bar));
        updateStatus(getDrawable(R.drawable.ic_transfers_paused));
    }

    /**
     * Sets the state of the widget as over quota.
     */
    private void setOverQuotaTransfers() {
        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_over_quota_progress_bar));
        updateStatus(getDrawable(R.drawable.ic_transfers_overquota));
    }

    /**
     * Sets the state of the widget as failed.
     */
    private void setFailedTransfers() {
        if (isOnTransferOverQuota()) return;

        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_warning_progress_bar));
        updateStatus(getDrawable(R.drawable.ic_transfers_error));
    }

    /**
     * Sets the progress of the transfers in the progress bar without taking into account the type of transfer.
     *
     * @param progress  the progress of the transfers
     */
    private void setProgress(int progress) {
        if (MegaApplication.getTransfersManagement().hasNotToBeShowDueToTransferOverQuota()) return;

        if (transfersWidget.getVisibility() != VISIBLE) {
            transfersWidget.setVisibility(VISIBLE);
        }

        progressBar.setProgress(progress);
    }

    /**
     * Sets the progress of the transfers in the progress bar taking into account the type of transfer.
     *
     * @param progress      the progress of the transfers
     * @param typeTransfer  type of the transfer:
     *                          - -1 if no type
     *                          - MegaTransfer.TYPE_DOWNLOAD if download transfer
     *                          - MegaTransfer.TYPE_UPLOAD if upload transfer
     */
    public void setProgress(int progress, int typeTransfer) {
        setProgress(progress);

        int numPendingDownloads = megaApi.getNumPendingDownloads();
        int numPendingUploads = megaApi.getNumPendingUploads();
        boolean pendingDownloads = numPendingDownloads > 0;
        boolean pendingUploads = numPendingUploads > 0;
        boolean downloadIcon = typeTransfer == TYPE_DOWNLOAD || (pendingDownloads && !pendingUploads) || numPendingDownloads > numPendingUploads;

        button.setImageDrawable(getDrawable(downloadIcon ? R.drawable.ic_transfers_download : R.drawable.ic_transfers_upload));
    }

    /**
     * Gets a drawable from its identifier.
     *
     * @param drawable  identifier of the drawable
     * @return  The Drawable which has the drawable value as identifier.
     */
    private Drawable getDrawable(int drawable) {
        return ContextCompat.getDrawable(context, drawable);
    }

    /**
     * Gets the number of pending transfers.
     *
     * @return The number of pending transfers.
     */
    public int getPendingTransfers() {
        return megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();
    }

    /**
     * Gets the progress of the transfers.
     *
     * @return The progress of the transfers.
     */
    private int getProgress() {
        long totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApi.getTotalUploadBytes();
        long totalSizeTransfered = megaApi.getTotalDownloadedBytes() + megaApi.getTotalUploadedBytes();

        return (int) Math.round((double) totalSizeTransfered / totalSizePendingTransfer * 100);
    }

    /**
     * Updates the status of the widget.
     *
     * @param drawable  Drawable to set as status image.
     */
    private void updateStatus(Drawable drawable) {
        if (status.getVisibility() != VISIBLE) {
            status.setVisibility(VISIBLE);
        }

        status.setImageDrawable(drawable);
    }
}
