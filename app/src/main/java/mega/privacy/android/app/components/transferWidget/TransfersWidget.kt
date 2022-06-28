package mega.privacy.android.app.components.transferWidget

import android.content.Context
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import android.widget.RelativeLayout
import mega.privacy.android.app.globalmanagement.TransfersManagement
import nz.mega.sdk.MegaApiAndroid
import android.widget.ImageButton
import android.widget.ProgressBar
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.R
import nz.mega.sdk.MegaTransfer
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.entity.TransfersInfo
import mega.privacy.android.app.utils.Util
import kotlin.math.roundToInt

/**
 * Class which allows to show a widget for informing about transfers progress and state.
 *
 * @property context                Required for get resources and check current section.
 * @property megaApi                Required for get transfers progress. Note that this is temporary and should
 *                                  be removed as soon as it is implemented in app side.
 * @property transfersWidget        Required to show the transfers widget.
 * @property transfersManagement    Required for transfers management.
 */
class TransfersWidget(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val transfersWidget: RelativeLayout,
    private val transfersManagement: TransfersManagement,
) {
    private val button: ImageButton
    private val progressBar: ProgressBar
    private val status: ImageView

    /**
     * Hides the widget.
     */
    fun hide() {
        transfersWidget.isVisible = false
    }

    /**
     * Updates the view of the widget taking into account the type of the transfer.
     *
     * @param transferType  type of the transfer:
     *                          - NO_TYPE if no type
     *                          - MegaTransfer.TYPE_DOWNLOAD if download transfer
     *                          - MegaTransfer.TYPE_UPLOAD if upload transfer
     * @param transfersInfo Transfers info.
     */
    fun update(transferType: Int, transfersInfo: TransfersInfo) {
        if (context is ManagerActivity) {
            if (context.drawerItem === DrawerItem.TRANSFERS) {
                transfersManagement.setAreFailedTransfers(false)
            }

            if (!isOnFileManagementManagerSection) {
                hide()
                return
            }
        }

        when {
            transfersInfo.numPendingTransfers > 0 && !transfersManagement.shouldShowNetworkWarning -> {
                setProgress(
                    transfersInfo = transfersInfo,
                    progress = getProgress(),
                    typeTransfer = transferType)

                updateState()
            }
            (transfersInfo.numPendingTransfers > 0 && transfersManagement.shouldShowNetworkWarning)
                    || transfersManagement.getAreFailedTransfers() -> {
                setFailedTransfers(transfersInfo)
            }
            else -> {
                hide()
            }
        }
    }

    /**
     * Checks if the widget is on a file management section in ManagerActivity.
     *
     * @return True if the widget is on a file management section in ManagerActivity, false otherwise.
     */
    private val isOnFileManagementManagerSection: Boolean
        get() {
            val drawerItem = (context as ManagerActivity).drawerItem

            return drawerItem !== DrawerItem.TRANSFERS
                    && drawerItem !== DrawerItem.NOTIFICATIONS
                    && drawerItem !== DrawerItem.CHAT
                    && drawerItem !== DrawerItem.RUBBISH_BIN
                    && drawerItem !== DrawerItem.PHOTOS
                    && !context.isInImagesPage
        }

    /**
     * Updates the state of the widget.
     */
    fun updateState() {
        when {
            transfersManagement.areTransfersPaused() -> setPausedTransfers()
            isOverQuota -> setOverQuotaTransfers()
            else -> setProgressTransfers()
        }
    }

    /**
     * Sets the state of the widget as in progress.
     * If some transfer failed, a warning icon indicates it.
     */
    private fun setProgressTransfers() {
        when {
            transfersManagement.getAreFailedTransfers() -> {
                updateStatus(getDrawable(R.drawable.ic_transfers_error))
            }
            isOverQuota -> {
                updateStatus(getDrawable(R.drawable.ic_transfers_overquota))
            }
            status.isVisible -> {
                status.isVisible = false
            }
        }

        progressBar.progressDrawable = getDrawable(R.drawable.thin_circular_progress_bar)
    }

    /**
     * Checks if should show transfer or storage over quota state.
     *
     * @return True if should show over quota state, false otherwise.
     */
    private val isOverQuota: Boolean
        get() {
            val isTransferOverQuota = transfersManagement.isOnTransferOverQuota()
            val isStorageOverQuota = transfersManagement.isStorageOverQuota()

            @Suppress("DEPRECATION")
            return (isTransferOverQuota && (megaApi.numPendingUploads <= 0 || isStorageOverQuota))
                    || (isStorageOverQuota && megaApi.numPendingDownloads <= 0)
        }

    /**
     * Sets the state of the widget as paused.
     */
    private fun setPausedTransfers() {
        if (isOverQuota) return

        progressBar.progressDrawable = getDrawable(R.drawable.thin_circular_progress_bar)
        updateStatus(getDrawable(R.drawable.ic_transfers_paused))
    }

    /**
     * Sets the state of the widget as over quota.
     */
    private fun setOverQuotaTransfers() {
        progressBar.progressDrawable = getDrawable(R.drawable.thin_circular_over_quota_progress_bar)
        updateStatus(getDrawable(R.drawable.ic_transfers_overquota))
    }

    /**
     * Sets the state of the widget as failed.
     *
     * @param transfersInfo Transfers info.
     */
    private fun setFailedTransfers(transfersInfo: TransfersInfo) {
        if (isOverQuota) return

        if (transfersWidget.visibility != View.VISIBLE) {
            transfersWidget.visibility = View.VISIBLE
        }

        setProgress(transfersInfo = transfersInfo, progress = getProgress(), typeTransfer = NO_TYPE)
        progressBar.progressDrawable = getDrawable(R.drawable.thin_circular_warning_progress_bar)
        updateStatus(getDrawable(R.drawable.ic_transfers_error))
    }

    /**
     * Sets the progress of the transfers.
     *
     * @param progress The progress to set.
     */
    private fun setProgress(progress: Int) {
        if (transfersManagement.hasNotToBeShowDueToTransferOverQuota()) return

        transfersWidget.isVisible = true
        progressBar.progress = progress
    }

    /**
     * Sets the progress of the transfers in the progress bar taking into account the type of transfer.
     *
     * @param transfersInfo Transfers info.
     * @param progress      the progress of the transfers
     * @param typeTransfer  type of the transfer:
     * - NO_TYPE if no type
     * - MegaTransfer.TYPE_DOWNLOAD if download transfer
     * - MegaTransfer.TYPE_UPLOAD if upload transfer
     */
    fun setProgress(transfersInfo: TransfersInfo, progress: Int, typeTransfer: Int) {
        setProgress(progress)
        val pendingDownloads = transfersInfo.numPendingDownloadsNonBackground > 0
        val pendingUploads = transfersInfo.numPendingUploads > 0

        val downloadIcon: Boolean =
            if (typeTransfer == MegaTransfer.TYPE_UPLOAD && pendingUploads) {
                false
            } else {
                (typeTransfer == MegaTransfer.TYPE_DOWNLOAD && pendingDownloads)
                        || (pendingDownloads && !pendingUploads)
                        || transfersInfo.numPendingDownloadsNonBackground > transfersInfo.numPendingUploads
            }

        button.setImageDrawable(
            getDrawable(
                if (downloadIcon) R.drawable.ic_transfers_download
                else R.drawable.ic_transfers_upload
            )
        )
    }

    /**
     * Gets a drawable from its identifier.
     *
     * @param drawable  identifier of the drawable
     * @return  The Drawable which has the drawable value as identifier.
     */
    private fun getDrawable(drawable: Int): Drawable? = ContextCompat.getDrawable(context, drawable)

    /**
     * Gets the progress of the transfers.
     *
     * @return The transfers progress.
     */
    @Suppress("DEPRECATION")
    private fun getProgress(): Int {
        val totalSizePendingTransfer = megaApi.totalDownloadBytes + megaApi.totalUploadBytes
        val totalSizeTransferred = megaApi.totalDownloadedBytes + megaApi.totalUploadedBytes
        val doubleProgress = totalSizeTransferred.toDouble() / totalSizePendingTransfer * 100

        return if (!doubleProgress.isNaN()) doubleProgress.roundToInt() else 0
    }

    /**
     * Updates the status of the widget.
     *
     * @param drawable  Drawable to set as status image.
     */
    private fun updateStatus(drawable: Drawable?) {
        if (status.visibility != View.VISIBLE) {
            status.visibility = View.VISIBLE
        }

        status.setImageDrawable(drawable)
    }

    companion object {
        const val NO_TYPE = -1
    }

    init {
        this.transfersWidget.visibility = View.GONE
        button = transfersWidget.findViewById(R.id.transfers_button)
        progressBar = transfersWidget.findViewById(R.id.transfers_progress)
        status = transfersWidget.findViewById(R.id.transfers_status)

        if (Util.isDarkMode(context)) {
            val color = getColorForElevation(context, 6f)

            (transfersWidget.findViewById<View>(R.id.transfers_relative_layout)
                .background as GradientDrawable).setColor(color)

            (button.background as GradientDrawable).setColor(color)
        }
    }
}