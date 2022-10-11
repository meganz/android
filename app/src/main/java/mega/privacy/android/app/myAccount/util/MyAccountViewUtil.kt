package mega.privacy.android.app.myAccount.util

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.myAccount.MyAccountViewModel
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.StyleUtils.setTextStyle
import mega.privacy.android.app.utils.TimeUtils
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava

object MyAccountViewUtil {

    /**
     * Enum class defining the active fragment.
     */
    enum class ActiveFragment {
        MY_ACCOUNT, MY_ACCOUNT_USAGE
    }

    private val gettingInfo by lazy { getString(R.string.recovering_info) }

    /**
     * Updates the views related to usage of storage and transfers
     * for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.update(viewModel: MyAccountViewModel) {
        storageProgressBar.isVisible = true
        noPercentageStorageImage.isVisible = false
        transferProgressBar.isVisible = true
        noPercentageTransferImage.isVisible = false

        if (viewModel.getUsedStorage().isEmpty()) {
            storageProgressPercentage.isVisible = false
            storageProgressBar.progress = 0
            storageProgress.text = gettingInfo
        } else {
            val usedStorage = viewModel.getUsedStoragePercentage()
            val isStorageOverQuota = usedStorage >= 100

            storageProgressPercentage.apply {
                isVisible = true
                text = getString(
                    R.string.used_storage_transfer_percentage,
                    usedStorage
                )

                setTextStyle(textAppearance =
                if (isStorageOverQuota) {
                    R.style.TextAppearance_Mega_Body2_Medium_Red600Red300
                } else {
                    R.style.TextAppearance_Mega_Body2_Medium_Accent
                }
                )
            }

            storageProgressBar.apply {
                progress = usedStorage
                progressDrawable =
                    ContextCompat.getDrawable(context,
                        if (isStorageOverQuota) {
                            R.drawable.storage_transfer_circular_progress_bar_warning
                        } else {
                            R.drawable.storage_transfer_circular_progress_bar
                        })
            }

            storageProgress.text = getString(
                R.string.used_storage_transfer,
                viewModel.getUsedStorage(),
                viewModel.getTotalStorage()
            )?.let { text ->
                if (isStorageOverQuota) {
                    text.formatColorTag(storageProgress.context, 'A', R.color.red_600_red_300)
                        .toSpannedHtmlText()
                } else {
                    text.replace("[A]", "")
                        .replace("[/A]", "")
                }
            }
        }

        transferLayout.isVisible = !viewModel.isFreeAccount()

        if (viewModel.getUsedTransfer().isEmpty()) {
            transferProgressPercentage.isVisible = false
            transferProgressBar.progress = 0
            transferProgress.text = gettingInfo
        } else {
            transferProgressPercentage.apply {
                isVisible = true
                text = getString(
                    R.string.used_storage_transfer_percentage,
                    viewModel.getUsedTransferPercentage()
                )
            }

            transferProgressBar.progress =
                viewModel.getUsedTransferPercentage()

            transferProgress.text = getString(
                R.string.used_storage_transfer,
                viewModel.getUsedTransfer(),
                viewModel.getTotalTransfer()
            ).replace("[A]", "")
                .replace("[/A]", "")
        }

        root.post { checkImagesOrProgressBarVisibility(viewModel.isFreeAccount()) }
    }

    /**
     * Updates the views related to usage of storage and transfers
     * for only business accounts.
     *
     * @param viewModel MyAccountViewModel to check the data.
     */
    fun MyAccountUsageContainerBinding.updateBusinessOrProFlexi(viewModel: MyAccountViewModel) {
        storageProgressPercentage.isVisible = false
        storageProgressBar.isVisible = false
        noPercentageStorageImage.isVisible = true

        storageProgress.text = viewModel.getUsedStorage().ifEmpty { gettingInfo }

        transferProgressPercentage.isVisible = false
        transferProgressBar.isVisible = false
        noPercentageTransferImage.isVisible = true

        transferProgress.text = viewModel.getUsedTransfer().ifEmpty { gettingInfo }

        root.post { checkImagesOrProgressBarVisibility(false) }
    }

    /**
     * Updates the views related to payments for all type of accounts except business ones.
     *
     * @param viewModel MyAccountViewModel to check the data.
     * @param fragment Value from `ActiveFragment` enum indicating what is the active fragment.
     */
    fun MyAccountPaymentInfoContainerBinding.update(
        viewModel: MyAccountViewModel,
        fragment: ActiveFragment,
    ): Boolean {
        businessStatusText.isVisible = false
        val renewable = viewModel.hasRenewableSubscription()

        return if (renewable || viewModel.hasExpirableSubscription()) {
            setRenewOrExpiryDate(viewModel, renewable, fragment)
            true
        } else false
    }

    /**
     * Updates the views related to payments for only business accounts.
     *
     * @param megaApi       MegaApiAndroid to check business status.
     * @param viewModel     MyAccountViewModel to check the data.
     * @param expandedView  True if the binding is in the expanded view, false otherwise.
     * @param fragment      Value from `ActiveFragment` enum indicating what is the active fragment.
     */
    fun MyAccountPaymentInfoContainerBinding.businessUpdate(
        megaApi: MegaApiAndroid,
        viewModel: MyAccountViewModel,
        expandedView: Boolean,
        fragment: ActiveFragment,
    ) {
        if (!megaApi.isMasterBusinessAccount) {
            return
        }

        val businessStatus = megaApi.businessStatus
        val renewable = viewModel.hasRenewableSubscription()
        val expirable = viewModel.hasExpirableSubscription()

        when {
            businessStatus == MegaApiJava.BUSINESS_STATUS_EXPIRED -> {
                setBusinessAlert(true, expandedView)
            }
            businessStatus == MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD -> {
                setBusinessAlert(false, expandedView)
            }
            renewable || expirable -> {
                setRenewOrExpiryDate(viewModel, renewable, fragment)
            }
        }
    }

    /**
     * Updates the views related to payments for all type of accounts.
     *
     * @param viewModel MyAccountViewModel to check the data.
     * @param renewable True if the subscriptions is renewable, false otherwise.
     * @param fragment  Value from `ActiveFragment` enum indicating what is the active fragment.
     */
    private fun MyAccountPaymentInfoContainerBinding.setRenewOrExpiryDate(
        viewModel: MyAccountViewModel,
        renewable: Boolean,
        fragment: ActiveFragment,
    ) {
        businessStatusText.isVisible = false

        renewExpiryText.apply {
            isVisible = true
            text = getString(
                if (renewable) R.string.account_info_renews_on else R.string.account_info_expires_on,
                TimeUtils.formatDate(
                    if (renewable) viewModel.getRenewTime() else viewModel.getExpirationTime(),
                    TimeUtils.DATE_MM_DD_YYYY_FORMAT
                )
            )

            text = when (fragment) {
                ActiveFragment.MY_ACCOUNT -> {
                    text.toString()
                        .replace("[A]", "<font face='sans-serif'>")
                        .replace("[/A]", "</font>")
                        .replace("[B]", "<big><font face='sans-serif-medium'>")
                        .replace("[/B]", "</font></big>")
                        .toSpannedHtmlText()
                }
                ActiveFragment.MY_ACCOUNT_USAGE -> {
                    text.toString()
                        .replace("[A]", "")
                        .replace("[/A]", "")
                        .formatColorTag(context, 'B', R.color.grey_087_white)
                        .toSpannedHtmlText()
                }
            }
        }
    }

    /**
     * Updates the business alert view by setting the expired or grace period message.
     *
     * @param expired True if the account is expired, false otherwise.
     * @param expandedView  True if the binding is in the expanded view, false otherwise.
     */
    private fun MyAccountPaymentInfoContainerBinding.setBusinessAlert(
        expired: Boolean,
        expandedView: Boolean,
    ) {
        renewExpiryText.isVisible = false

        businessStatusText.apply {
            isVisible = true
            text = getString(
                if (expired) R.string.payment_overdue_label
                else R.string.payment_required_label
            )

            setTextStyle(
                textAppearance = when {
                    expandedView && expired -> R.style.TextAppearance_Mega_Body2_Red400Red300
                    expandedView -> R.style.TextAppearance_Mega_Body2_Amber800Amber700
                    expired -> R.style.TextAppearance_Mega_Body2_Red400
                    else -> R.style.TextAppearance_Mega_Body2_Amber400
                }
            )
        }
    }

    /**
     * Checks if should show the images of storage and transfer in case of business accounts
     * and the ProgressBars in case of non business accounts.
     * If some of the text storageProgress, storageLabel, transferProgress or transferLabel
     * occupies more than one line, then they should be hidden.
     *
     * @param isFreeAccount True if is a free account, false otherwise.
     */
    private fun MyAccountUsageContainerBinding.checkImagesOrProgressBarVisibility(isFreeAccount: Boolean) {
        val visible = when {
            isFreeAccount -> storageProgress.lineCount == 1 && storageLabel.lineCount == 1
            else -> storageProgress.lineCount == 1 && storageLabel.lineCount == 1
                    && transferProgress.lineCount == 1 && transferLabel.lineCount == 1
        }

        storageProgressLayout.isVisible = visible
        transferProgressLayout.isVisible = visible
    }


}