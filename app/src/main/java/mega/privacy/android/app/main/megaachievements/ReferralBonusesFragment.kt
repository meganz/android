package mega.privacy.android.app.main.megaachievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentReferralBonusesBinding
import mega.privacy.android.app.listeners.GetAchievementsListener
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * ReferralBonusesFragment
 */
@AndroidEntryPoint
class ReferralBonusesFragment : Fragment(), GetAchievementsListener.DataCallback {
    private var _binding: FragmentReferralBonusesBinding? = null
    private val binding get() = _binding!!

    private var mAdapter: MegaReferralBonusesAdapter? = null

    /**
     * [MegaApiAndroid] injection
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    /**
     * ioDispatcher as [CoroutineDispatcher] injection
     */
    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    /**
     * [GetAchievementsListener] injection
     */
    @Inject
    lateinit var getAchievementsListener: GetAchievementsListener

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReferralBonusesBinding.inflate(layoutInflater)
        binding.referralBonusesRecyclerView.apply {
            addItemDecoration(SimpleDividerItemDecoration(requireContext()))
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
        }
        return binding.root
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Activity actionbar has been created which might be accessed by UpdateUI().
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.title_referral_bonuses)

        // The root view has been created, fill it with the data when data ready
        getAchievementsListener.setDataCallback(this)
    }

    /**
     * onDestroyView
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUI() {
        val bonuses = getAchievementsListener.referralBonuses

        if (mAdapter == null) {
            mAdapter = MegaReferralBonusesAdapter(
                requireActivity(),
                this,
                bonuses,
                binding.referralBonusesRecyclerView
            )
        } else {
            mAdapter?.setReferralBonuses(bonuses)
        }

        binding.referralBonusesRecyclerView.adapter = mAdapter
    }

    override fun onAchievementsReceived() {
        updateUI()
    }
}