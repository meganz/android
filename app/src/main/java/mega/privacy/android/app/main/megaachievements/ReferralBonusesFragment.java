package mega.privacy.android.app.main.megaachievements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.listeners.GetAchievementsListener;
import mega.privacy.android.data.qualifier.MegaApi;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

@AndroidEntryPoint
public class ReferralBonusesFragment extends Fragment implements GetAchievementsListener.DataCallback {
    RelativeLayout parentRelativeLayout;
    RecyclerView recyclerView;
    LinearLayoutManager mLayoutManager;
    MegaReferralBonusesAdapter adapter;

    @Inject
    @MegaApi
    MegaApiAndroid megaApi;

    @Inject
    GetAchievementsListener getAchievementsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView");

        boolean enabledAchievements = megaApi.isAchievementsEnabled();
        Timber.d("The achievements are: %s", enabledAchievements);

        View v = inflater.inflate(R.layout.fragment_referral_bonuses, container, false);

        parentRelativeLayout = (RelativeLayout) v.findViewById(R.id.referral_bonuses_relative_layout);

        recyclerView = (RecyclerView) v.findViewById(R.id.referral_bonuses_recycler_view);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(requireContext()));
        mLayoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Activity actionbar has been created which might be accessed by UpdateUI().
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_referral_bonuses));
        }

        // The root view has been created, fill it with the data when data ready
        getAchievementsListener.setDataCallback(this);
    }

    private void updateUI() {
        requireContext();

        ArrayList<ReferralBonus> bonuses = getAchievementsListener.getReferralBonuses();
        if (!bonuses.isEmpty()) {
            if (adapter == null) {
                adapter = new MegaReferralBonusesAdapter(requireActivity(), this, bonuses, recyclerView);
            } else {
                adapter.setReferralBonuses(bonuses);
            }

            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onAchievementsReceived() {
        updateUI();
    }
}
