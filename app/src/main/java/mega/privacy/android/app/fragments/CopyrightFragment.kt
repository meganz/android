package mega.privacy.android.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.databinding.FragmentCopyrightBinding
import mega.privacy.android.app.lollipop.GetLinkActivityLollipop
import mega.privacy.android.app.utils.Constants.GET_LINK_FRAGMENT

class CopyrightFragment : BaseFragment() {

    private lateinit var binding: FragmentCopyrightBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCopyrightBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.agreeButton.setOnClickListener {
            dbH.setShowCopyright(false)
            (activity as GetLinkActivityLollipop).showFragment(GET_LINK_FRAGMENT)
        }

        binding.disagreeButton.setOnClickListener {
            dbH.setShowCopyright(true)
            activity?.finish()
        }

        super.onViewCreated(view, savedInstanceState)
    }
}