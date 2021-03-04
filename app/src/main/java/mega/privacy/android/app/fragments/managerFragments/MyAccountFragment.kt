package mega.privacy.android.app.fragments.managerFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class MyAccountFragment : BaseFragment(), Scrollable {

    private lateinit var binding: FragmentMyAccountBinding

    companion object {
        @JvmStatic
        fun newInstance(): MyAccountFragment {
            return MyAccountFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyAccountBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun checkScroll() {
        (requireActivity() as ManagerActivityLollipop).changeAppBarElevation(
            binding.scrollView.canScrollVertically(
                -1
            )
        )
    }

    fun setAccountDetails() {

    }

    fun onBackPressed(): Int {
        return 0
    }

    fun resetPass() {

    }

    fun updateNameView(fullName: String) {

    }

    fun updateAvatar(retry: Boolean) {

    }

    fun refreshVersionsInfo() {

    }

    fun initCreateQR(request: MegaRequest, e: MegaError) {

    }

    fun updateContactsCount() {

    }

    fun updateView() {

    }

    fun updateMailView(email: String) {

    }

    fun checkLogoutWarnings() {

    }

    fun updateAddPhoneNumberLabel() {

    }
}