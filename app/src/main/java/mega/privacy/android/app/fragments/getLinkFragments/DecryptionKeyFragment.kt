package mega.privacy.android.app.fragments.getLinkFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mega.privacy.android.app.databinding.FragmentDecryptionKeyBinding

class DecryptionKeyFragment : Fragment() {

    private lateinit var binding: FragmentDecryptionKeyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDecryptionKeyBinding.inflate(layoutInflater)
        return binding.root
    }
}