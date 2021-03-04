package mega.privacy.android.app.fragments.managerFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.databinding.FragmentMyAccountBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

class MyAccountFragment : BaseFragment(), Scrollable {

    private lateinit var binding: FragmentMyAccountBinding

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
}