package mega.privacy.android.app.myAccount.util

import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.myAccount.MyAccountViewModel
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava

object MyAccountViewUtil {

    private val gettingInfo = getString(R.string.recovering_info)

    /**
     * Updates the views related to usage of storage and transfers
     * for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.update(viewModel: MyAccountViewModel) {
        storageProgressPercentage.isVisible = true
        storageProgressBar.isVisible = true
        businessStorageImage.isVisible = false
        transferProgressPercentage.isVisible = true
        transferProgressBar.isVisible = true
        businessTransferImage.isVisible = false

        if (viewModel.getUsedStorage().isEmpty()) {
            storageProgressPercentage.isVisible = false
            storageProgressBar.progress = 0
            storageProgress.text = gettingInfo
        } else {
            storageProgressPercentage.isVisible = true
            storageProgressPercentage.text = getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedStoragePercentage()
            )

            storageProgressBar.progress =
                viewModel.getUsedStoragePercentage()
            storageProgress.text = getString(
                R.string.used_storage_transfer,
                viewModel.getUsedStorage(),
                viewModel.getTotalStorage()
            )
        }

        transferLayout.isVisible = !viewModel.isFreeAccount()

        if (viewModel.getUsedTransfer().isEmpty()) {
            transferProgressPercentage.isVisible = false
            transferProgressBar.progress = 0
            transferProgress.text = gettingInfo
        } else {
            transferProgressPercentage.isVisible = true
            transferProgressPercentage.text = getString(
                R.string.used_storage_transfer_percentage,
                viewModel.getUsedTransferPercentage()
            )

            transferProgressBar.progress =
                viewModel.getUsedTransferPercentage()
            transferProgress.text = getString(
                R.string.used_storage_transfer,
                viewModel.getUsedTransfer(),
                viewModel.getTotalTransfer()
            )
        }
    }

    /**
     * Updates the views related to usage of storage and transfers
     * for only business accounts.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.businessUpdate(viewModel: MyAccountViewModel) {
        storageProgressPercentage.isVisible = false
        storageProgressBar.isVisible = false
        businessStorageImage.isVisible = true

        storageProgress.text =
            if (viewModel.getUsedStorage().isEmpty()) gettingInfo
            else viewModel.getUsedStorage()

        transferProgressPercentage.isVisible = false
        transferProgressBar.isVisible = false
        businessTransferImage.isVisible = true

        transferProgress.text =
            if (viewModel.getUsedTransfer().isEmpty()) gettingInfo
            else viewModel.getUsedTransfer()
    }

    /**
     * Updates the views related to payments for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountPaymentInfoContainerBinding.update(viewModel: MyAccountViewModel): Boolean {
        businessStatusText.isVisible = false

        return if (viewModel.hasRenewableSubscription() || viewModel.hasExpirableSubscription()) {
            renewExpiryText.isVisible = true
            renewExpiryDateText.isVisible = true

            renewExpiryText.text = getString(
                if (viewModel.hasRenewableSubscription()) R.string.renews_on else R.string.expires_on
            )

            renewExpiryDateText.text = TimeUtils.formatDate(
                if (viewModel.hasRenewableSubscription()) viewModel.getRenewTime() else viewModel.getExpirationTime(),
                TimeUtils.DATE_MM_DD_YYYY_FORMAT
            )

            true
        } else false
    }

    /**
     * Updates the views related to payments for only business accounts.
     *
     * @param megaApi MegaApiAndroid to check business status.
     */
    fun MyAccountPaymentInfoContainerBinding.businessUpdate(megaApi: MegaApiAndroid) {
        renewExpiryText.isVisible = false
        renewExpiryDateText.isVisible = false
        businessStatusText.apply {
            isVisible = true

            text = getString(
                if (megaApi.businessStatus == MegaApiJava.BUSINESS_STATUS_EXPIRED) R.string.payment_overdue_label
                else R.string.payment_required_label
            )
        }
    }
}