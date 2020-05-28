package mega.privacy.android.app.components.transferWidget;

import mega.privacy.android.app.MegaApplication;

public class TransfersManagement {
    private static final long INVALID_VALUE = -1;
    private static final int WAIT_TIME_TO_SHOW_WARNING = 60000;

    private long transferOverQuotaTimestamp;
    private boolean hasNotToBeShowDueToTransferOverQuota;
    private boolean isCurrentTransferOverQuota;
    private boolean isOnTransfersSection;
    private boolean failedTransfers;
    private boolean transferOverQuotaNotificationShown;

    public TransfersManagement() {
        resetTransferOverQuotaTimestamp();
    }

    public void setTransferOverQuotaTimestamp() {
        this.transferOverQuotaTimestamp = System.currentTimeMillis();
    }

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

    public static boolean isOnTransferOverQuota() {
        return MegaApplication.getInstance().getMegaApi().getBandwidthOverquotaDelay() > 0;
    }

    public void setCurrentTransferOverQuota(boolean currentTransferOverQuota) {
        isCurrentTransferOverQuota = currentTransferOverQuota;
    }

    public boolean isCurrentTransferOverQuota() {
        return isCurrentTransferOverQuota;
    }

    public void setHasNotToBeShowDueToTransferOverQuota(boolean hasNotToBeShowDueToTransferOverQuota) {
        this.hasNotToBeShowDueToTransferOverQuota = hasNotToBeShowDueToTransferOverQuota;
    }

    /**
     * Checks if the transfers widget has to be shown.
     * If the widget does not have to be shown means that the user is in transfer over quota
     * and they already opened the transfers section by clicking the widget.
     *
     * @return True if the widget does not have to be shown, false otherwise
     */
    public boolean hasNotToBeShowDueToTransferOverQuota() {
        return hasNotToBeShowDueToTransferOverQuota;
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
}
