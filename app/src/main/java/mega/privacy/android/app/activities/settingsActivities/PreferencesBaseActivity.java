package mega.privacy.android.app.activities.settingsActivities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.SettingsBaseFragment;
import mega.privacy.android.app.lollipop.PinActivityLollipop;

import static mega.privacy.android.app.utils.Util.*;

public class PreferencesBaseActivity extends PinActivityLollipop {

    protected FrameLayout fragmentContainer;
    protected Toolbar tB;
    protected ActionBar aB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        changeStatusBarColor(this, getWindow(), R.color.lollipop_dark_primary_color);

        setContentView(R.layout.activity_settings);

        fragmentContainer = findViewById(R.id.fragment_container);

        tB = findViewById(R.id.toolbar_settings);
        if (tB == null) {
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if (aB != null) {
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black);
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }

        changeStatusBarColor(this, getWindow(), R.color.dark_primary_color);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

    protected void replaceFragment (SettingsBaseFragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.commit();
    }
}
