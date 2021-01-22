package mega.privacy.android.app.fragments.getLinkFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.activities.GetLinkActivity.Companion.GET_LINK_FRAGMENT
import mega.privacy.android.app.databinding.FragmentCopyrightBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.interfaces.GetLinkInterface

class CopyrightFragment(private val getLinkInterface: GetLinkInterface) : BaseFragment() {

    private lateinit var binding: FragmentCopyrightBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCopyrightBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.agreeButton.setOnClickListener {
            dbH.setShowCopyright(false)
            getLinkInterface.showFragment(GET_LINK_FRAGMENT)
        }

        binding.disagreeButton.setOnClickListener {
            dbH.setShowCopyright(true)
            activity?.finish()
        }

        super.onViewCreated(view, savedInstanceState)
    }
}