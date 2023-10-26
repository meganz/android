package mega.privacy.android.app.myAccount

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.FragmentMyAccountUsageBinding
import mega.privacy.android.app.databinding.MyAccountPaymentInfoContainerBinding
import mega.privacy.android.app.databinding.MyAccountUsageContainerBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.ActiveFragment
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.businessUpdate
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.setRenewalDateForProFlexi
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.update
import mega.privacy.android.app.myAccount.util.MyAccountViewUtil.updateBusinessOrProFlexi
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Fragment for my account usage
 */
@AndroidEntryPoint
class MyAccountUsageFragment : Fragment(), Scrollable {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    private val viewModel: MyAccountViewModel by activityViewModels()

    private val isBusinessAccount
        get() = viewModel.state.value.isBusinessAccount

    private lateinit var binding: FragmentMyAccountUsageBinding
    private lateinit var usageBinding: MyAccountUsageContainerBinding
    private lateinit var paymentAlertBinding: MyAccountPaymentInfoContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyAccountUsageBinding.inflate(layoutInflater)
        usageBinding = binding.usageViewLayout
        paymentAlertBinding = binding.paymentAlert
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupObservers()
        binding.upgradeButton.setOnClickListener {
            startActivity(Intent(requireActivity(), UpgradeAccountActivity::class.java))
            viewModel.setOpenUpgradeFrom()
        }
    }

    private fun setupView() {
        viewModel.getFileVersionsOption()
        binding.usageLayout.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        usageBinding.root.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.background_usage_storage_transfer
        )

        paymentAlertBinding.renewExpiryText.setTextAppearance(
            R.style.TextAppearance_Mega_Body1_Grey87White54,
        )

        binding.rubbishSeparator.isVisible = false
        binding.previousVersionsStorageContainer.isVisible = false

        setupAccountDetails()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized)
            return

        val withElevation = binding.usageLayout.canScrollVertically(SCROLLING_UP_DIRECTION)
        viewModel.setElevation(withElevation)
    }

    private fun setupObservers() {
        viewLifecycleOwner.collectFlow(viewModel.state) {
            refreshVersionsInfo(it.versionsInfo, it.isFileVersioningEnabled)
        }
        viewModel.onUpdateAccountDetails().observe(viewLifecycleOwner) { setupAccountDetails() }
    }

    /**
     * Updates versions info depending on if is enabled or not.
     *
     * @param versionsInfo Text to show as versions info.
     */
    private fun refreshVersionsInfo(versionsInfo: String?, isFileVersioningEnabled: Boolean) {
        if (isFileVersioningEnabled) {
            binding.rubbishSeparator.isVisible = true
            binding.previousVersionsStorageContainer.isVisible = true
            binding.previousVersionsText.text = versionsInfo
        } else {
            binding.rubbishSeparator.isVisible = false
            binding.previousVersionsStorageContainer.isVisible = false
        }
    }

    private fun setupAccountDetails() {
        if (isBusinessAccount) {
            usageBinding.updateBusinessOrProFlexi(
                requireContext(),
                viewModel.getUsedStorage(),
                viewModel.getUsedTransfer()
            )
            paymentAlertBinding.businessUpdate(
                requireContext(),
                viewModel.getRenewTime(),
                viewModel.getExpirationTime(),
                viewModel.hasRenewableSubscription(),
                viewModel.hasExpirableSubscription(),
                megaApi,
                true,
                ActiveFragment.MY_ACCOUNT_USAGE
            )
            paymentAlertBinding.root.isVisible = true
            binding.upgradeButton.isVisible = false
        } else if (viewModel.isProFlexiAccount()) {
            usageBinding.updateBusinessOrProFlexi(
                requireContext(),
                viewModel.getUsedStorage(),
                viewModel.getUsedTransfer()
            )
            paymentAlertBinding.setRenewalDateForProFlexi(viewModel)
            binding.upgradeButton.isVisible = false
        } else {
            usageBinding.update(
                requireContext(),
                viewModel.isFreeAccount(),
                viewModel.getTotalStorage(),
                viewModel.getTotalTransfer(),
                viewModel.getUsedStorage(),
                viewModel.getUsedStoragePercentage(),
                viewModel.getUsedTransfer(),
                viewModel.getUsedTransferPercentage()
            )
            paymentAlertBinding.root.isVisible = paymentAlertBinding.update(
                viewModel.getRenewTime(),
                viewModel.getExpirationTime(),
                viewModel.hasRenewableSubscription(),
                viewModel.hasExpirableSubscription(),
                ActiveFragment.MY_ACCOUNT_USAGE
            )
            binding.upgradeButton.isVisible = true
        }

        binding.cloudStorageText.text = viewModel.getCloudStorage()

        val backupsStorage = viewModel.getBackupsStorage()
        if (backupsStorage.isEmpty()) {
            binding.backupsStorageContainer.isVisible = false
        } else {
            binding.backupsStorageContainer.isVisible = true
            binding.backupsStorageText.text = backupsStorage
        }

        binding.incomingStorageText.text = viewModel.getIncomingStorage()
        binding.rubbishStorageText.text = viewModel.getRubbishStorage()
    }
}