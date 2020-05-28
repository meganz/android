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

    public void hide() {
        transfersWidget.setVisibility(GONE);
    }

    public void update() {
        update(-1);
    }

    public void update(int transferType) {
        if (!isOnline(context)) return;

        if (context instanceof ManagerActivityLollipop && ManagerActivityLollipop.getDrawerItem() == ManagerActivityLollipop.DrawerItem.TRANSFERS) {
            MegaApplication.getTransfersManagement().setFailedTransfers(false);
        }

        if (!isOnFileManagementManagerSection()) {
            hide();
            return;
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

    private boolean isOnFileManagementManagerSection() {
        return !(context instanceof ManagerActivityLollipop)
                || (ManagerActivityLollipop.getDrawerItem() != ManagerActivityLollipop.DrawerItem.TRANSFERS
                && ManagerActivityLollipop.getDrawerItem() != ManagerActivityLollipop.DrawerItem.CONTACTS
                && ManagerActivityLollipop.getDrawerItem() != ManagerActivityLollipop.DrawerItem.ACCOUNT
                && ManagerActivityLollipop.getDrawerItem() != ManagerActivityLollipop.DrawerItem.SETTINGS
                && ManagerActivityLollipop.getDrawerItem() != ManagerActivityLollipop.DrawerItem.NOTIFICATIONS);
    }

    public void updateState() {
        if (megaApi.areTransfersPaused(TYPE_DOWNLOAD) || megaApi.areTransfersPaused(TYPE_UPLOAD)) {
            setPausedTransfers();
        } else if (isOnTransferOverQuota()){
            setOverQuotaTransfers();
        } else {
            setProgressTransfers();
        }
    }

    private void setProgressTransfers() {
        if (MegaApplication.getTransfersManagement().thereAreFailedTransfers()) {
            status.setVisibility(VISIBLE);
            status.setImageDrawable(getDrawable(R.drawable.ic_transfers_error));
        } else {
            status.setVisibility(GONE);
        }

        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_progress_bar));
    }

    private void setPausedTransfers() {
        if (isOnTransferOverQuota()) return;

        status.setVisibility(VISIBLE);
        status.setImageDrawable(getDrawable(R.drawable.ic_transfers_paused));
    }

    private void setOverQuotaTransfers() {
        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_over_quota_progress_bar));
        status.setVisibility(VISIBLE);
        status.setImageDrawable(getDrawable(R.drawable.ic_transfers_overquota));
    }

    private void setFailedTransfers() {
        if (isOnTransferOverQuota()) return;

        progressBar.setProgressDrawable(getDrawable(R.drawable.thin_circular_warning_progress_bar));
        status.setVisibility(VISIBLE);
        status.setImageDrawable(getDrawable(R.drawable.ic_transfers_error));
    }

    private void setProgress(int progress) {
        if (MegaApplication.getTransfersManagement().hasNotToBeShowDueToTransferOverQuota()) return;

        if (transfersWidget.getVisibility() != VISIBLE) {
            transfersWidget.setVisibility(VISIBLE);
        }

        progressBar.setProgress(progress);
    }

    public void setProgress(int progress, int typeTransfer) {
        setProgress(progress);

        int numPendingDownloads = megaApi.getNumPendingDownloads();
        int numPendingUploads = megaApi.getNumPendingUploads();
        boolean pendingDownloads = numPendingDownloads > 0;
        boolean pendingUploads = numPendingUploads > 0;
        boolean downloadIcon = typeTransfer == TYPE_DOWNLOAD || (pendingDownloads && !pendingUploads) || numPendingDownloads > numPendingUploads;

        button.setImageDrawable(getDrawable(downloadIcon ? R.drawable.ic_transfers_download : R.drawable.ic_transfers_upload));
    }

    private Drawable getDrawable(int drawable) {
        return ContextCompat.getDrawable(context, drawable);
    }

    private int getPendingTransfers() {
        return megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();
    }

    private int getProgress() {
        long totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApi.getTotalUploadBytes();
        long totalSizeTransfered = megaApi.getTotalDownloadedBytes() + megaApi.getTotalUploadedBytes();

        return (int) Math.round((double) totalSizeTransfered / totalSizePendingTransfer * 100);
    }
}
