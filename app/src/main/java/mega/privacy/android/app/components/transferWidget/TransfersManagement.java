package mega.privacy.android.app.components.transferWidget;

import mega.privacy.android.app.MegaApplication;

public class TransfersManagement {
    private static final long INVALID_VALUE = -1;
    private static final int WAIT_TIME_TO_SHOW_WARNING = 60000;

    private long transferOverQuotaTimestamp;
    private boolean hasNotToBeShowDueToTransferOverQuota;
    private boolean isCurrentTransferOverQuota;

    public TransfersManagement() {
        transferOverQuotaTimestamp = INVALID_VALUE;
    }

    public void setTransferOverQuotaTimestamp() {
        this.transferOverQuotaTimestamp = System.currentTimeMillis();
    }

    /**
     * Checks if a transfer over quota warning has to be shown.
     * It will be shown if transferOverQuotaTimestamp has not been initialized yet
     * or if more than a minute has passed since the last time it was shown.
     *
     * @return  True if the warning has to be shown, false otherwise
     */
    public boolean shouldShowTransferOverQuotaWarning() {
        boolean notInitialized = transferOverQuotaTimestamp == INVALID_VALUE;
        boolean minorthan1minute = transferOverQuotaTimestamp - System.currentTimeMillis() > WAIT_TIME_TO_SHOW_WARNING;

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
}
