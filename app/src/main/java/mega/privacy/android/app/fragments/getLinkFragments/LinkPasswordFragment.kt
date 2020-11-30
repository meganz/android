package mega.privacy.android.app.fragments.getLinkFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mega.privacy.android.app.databinding.FragmentSetLinkPasswordBinding
import mega.privacy.android.app.interfaces.GetLinkInterface

class LinkPasswordFragment(private val getLinkInterface: GetLinkInterface) : Fragment() {

    private lateinit var binding: FragmentSetLinkPasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetLinkPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        super.onViewCreated(view, savedInstanceState)
    }
}