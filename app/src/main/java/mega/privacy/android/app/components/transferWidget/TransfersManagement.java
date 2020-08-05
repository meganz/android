package mega.privacy.android.app.components.transferWidget;

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD;
import static nz.mega.sdk.MegaTransfer.TYPE_UPLOAD;

public class TransfersManagement {
    private static final long INVALID_VALUE = -1;
    private static final int WAIT_TIME_TO_SHOW_WARNING = 60000;

    private long transferOverQuotaTimestamp;
    private boolean hasNotToBeShowDueToTransferOverQuota;
    private boolean isCurrentTransferOverQuota;
    private boolean isOnTransfersSection;
    private boolean failedTransfers;
    private boolean transferOverQuotaNotificationShown;
    private boolean isTransferOverQuotaBannerShown;

    private ArrayList<String> pausedTransfers = new ArrayList<>();
    private Map<String, String> targetPaths = new HashMap<>();

    public TransfersManagement() {
        resetTransferOverQuotaTimestamp();
    }

    /**
     * Checks if the queue of transfers is paused or all the current in-progress transfers are individually.
     *
     * @return True if the queue of transfers or all the current in-progress transfers are paused, false otherwise.
     */
    public boolean areTransfersPaused() {
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        int totalTransfers = megaApi.getNumPendingDownloads() + megaApi.getNumPendingUploads();

        return megaApi.areTransfersPaused(TYPE_DOWNLOAD) || megaApi.areTransfersPaused(TYPE_UPLOAD) || totalTransfers == pausedTransfers.size();
    }

    /**
     * Removes a resumed transfer.
     *
     * @param transferTag   tag of the resumed transfer
     */
    public void removePausedTransfers(int transferTag) {
        pausedTransfers.remove(Integer.toString(transferTag));
    }

    /**
     * Adds a paused transfer.
     *
     * @param transferTag   tag of the paused transfer
     */
    public void addPausedTransfers(int transferTag) {
        pausedTransfers.add(Integer.toString(transferTag));
    }

    /**
     * Clears the paused transfers list.
     */
    public void resetPausedTransfers() {
        pausedTransfers.clear();
    }

    /**
     * Sets the current time as timestamp to avoid show duplicated transfer over quota warnings.
     */
    public void setTransferOverQuotaTimestamp() {
        this.transferOverQuotaTimestamp = System.currentTimeMillis();
    }

    /**
     * Sets the transfer over quota time stamp as invalid.
     */
    public void resetTransferOverQuotaTimestamp() {
        this.transferOverQuotaTimestamp = INVALID_VALUE;
    }

    /**
     * Checks if a transfer over quota warning has to be shown.
     * It will be shown if transferOverQuotaTimestamp has not been initialized yet
     * or if more than a minute has passed since the last time it was shown.
     *
     * @return  True if the warning has to be shown, false otherwise
     */
    public boolean shouldShowTransferOverQuotaWarning() {
        return transferOverQuotaTimestamp == INVALID_VALUE || transferOverQuotaTimestamp - System.currentTimeMillis() > WAIT_TIME_TO_SHOW_WARNING;
    }

    /**
     * Checks if it is on transfer over quota.
     *
     * @return True if it is on transfer over quota, false otherwise.
     */
    public static boolean isOnTransferOverQuota() {
        return MegaApplication.getInstance().getMegaApi().getBandwidthOverquotaDelay() > 0;
    }

    public void setCurrentTransferOverQuota(boolean currentTransferOverQuota) {
        isCurrentTransferOverQuota = currentTransferOverQuota;
    }

    /**
     * Checks if the transfer over quota has occurred at this moment
     * or it occurred in other past moment.
     *
     * @return  True if the transfer over quota has occurred at this moment, false otherwise.
     */
    public boolean isCurrentTransferOverQuota() {
        return isCurrentTransferOverQuota;
    }

    /**
     * Sets if the widget has to be shown depending on if it is on transfer over quota
     * and the Transfers section has been opened from the transfers widget.
     * Also sets if the "transfer over quota" banner has to be shown due to the same reason.
     *
     * @param hasNotToBeShowDueToTransferOverQuota  true if it is on transfer over quota and the Transfers section
     *                                              has been opened from the transfers widget, false otherwise
     */
    public void setHasNotToBeShowDueToTransferOverQuota(boolean hasNotToBeShowDueToTransferOverQuota) {
        this.hasNotToBeShowDueToTransferOverQuota = hasNotToBeShowDueToTransferOverQuota;
        setTransferOverQuotaBannerShown(hasNotToBeShowDueToTransferOverQuota);
    }

    /**
     * Checks if the transfers widget has to be shown.
     * If the widget does not have to be shown means that:
     * the user is in transfer over quota, there is not any upload transfer in progress
     * and they already opened the transfers section by clicking the widget.
     *
     * @return True if the widget does not have to be shown, false otherwise
     */
    public boolean hasNotToBeShowDueToTransferOverQuota() {
        return hasNotToBeShowDueToTransferOverQuota && MegaApplication.getInstance().getMegaApi().getNumPendingUploads() <= 0;
    }

    public void setIsOnTransfersSection(boolean isOnTransfersSection) {
        this.isOnTransfersSection = isOnTransfersSection;
    }

    public boolean isOnTransfersSection() {
        return isOnTransfersSection;
    }

    public void setFailedTransfers(boolean failedTransfers) {
        this.failedTransfers = failedTransfers;
    }

    public boolean thereAreFailedTransfers() {
        return failedTransfers;
    }

    public void setTransferOverQuotaNotificationShown(boolean transferOverQuotaNotificationShown) {
        this.transferOverQuotaNotificationShown = transferOverQuotaNotificationShown;
    }

    public boolean isTransferOverQuotaNotificationShown() {
        return transferOverQuotaNotificationShown;
    }

    public void setTransferOverQuotaBannerShown(boolean transferOverQuotaBannerShown) {
        isTransferOverQuotaBannerShown = transferOverQuotaBannerShown;
    }

    public boolean isTransferOverQuotaBannerShown() {
        return isTransferOverQuotaBannerShown;
    }

    /**
     * Sends a broadcast to update the transfer widget where needed.
     *
     * @param transferType  the transfer type.
     */
    public static void launchTransferUpdateIntent(int transferType) {
        MegaApplication.getInstance().sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE).putExtra(TRANSFER_TYPE, transferType));
    }

    public void setTargetPaths(Map<String, String> targetPaths) {
        this.targetPaths = targetPaths;
    }

    public Map<String, String> getTargetPaths() {
        return targetPaths;
    }
}
