package mega.privacy.android.app.main.megaachievements;

import static mega.privacy.android.app.utils.Constants.ACHIEVEMENTS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.BONUSES_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.INFO_ACHIEVEMENTS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.INVITE_FRIENDS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_GET_CONTACTS;
import static mega.privacy.android.app.utils.Util.hideKeyboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.listeners.GetAchievementsListener;
import mega.privacy.android.app.main.InviteContactActivity;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import timber.log.Timber;

public class AchievementsActivity extends PasscodeActivity {
    private static final String TAG_ACHIEVEMENTS = "achievementsFragment";
    static final int INVALID_TYPE = -1;

    FrameLayout fragmentContainer;
    Toolbar tB;
    ActionBar aB;

    @SuppressLint("StaticFieldLeak")
    public static GetAchievementsListener sFetcher;
    private androidx.appcompat.app.AlertDialog successDialog;

    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        setContentView(R.layout.activity_achievements);

        fragmentContainer = findViewById(R.id.fragment_container_achievements);
        tB = findViewById(R.id.toolbar_achievements);
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        Timber.d("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(Util.isDarkMode(this) ? R.drawable.ic_arrow_back_white : R.drawable.ic_arrow_back_black);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container_achievements, new AchievementsFragment(), TAG_ACHIEVEMENTS);
            ft.commitNow();

            sFetcher = new GetAchievementsListener();
            sFetcher.setRequestCallback(() -> {
                showSnackbar(getString(R.string.cancel_subscription_error));
            });
            sFetcher.fetch();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sFetcher != null) {
            sFetcher.setRequestCallback(null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected");

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_achievements) instanceof AchievementsFragment) {
                    sFetcher.setDataCallback(null);
                    sFetcher = null;
                    finish();
                } else {
                    getSupportFragmentManager().popBackStack();
                }

                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void showFragment(int fragmentName, int type) {
        Timber.d("showFragment: %d type: %d", fragmentName, type);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;
        String tag = "";

        switch (fragmentName) {
            case ACHIEVEMENTS_FRAGMENT:
                hideKeyboard(this, InputMethodManager.HIDE_NOT_ALWAYS);
                aB.setTitle(StringResourcesUtils.getString(R.string.achievements_title));

                fragment = new AchievementsFragment();
                tag = "achievementsFragment";
                break;
            case INVITE_FRIENDS_FRAGMENT:
                fragment = new InviteFriendsFragment();
                tag = "inviteFriendsFragment";
                ft.addToBackStack(tag);
                break;
            case BONUSES_FRAGMENT:
                fragment = new ReferralBonusesFragment();
                tag = "referralBonusesFragment";
                ft.addToBackStack(tag);
                break;
            case INFO_ACHIEVEMENTS_FRAGMENT:
                Bundle bundle = new Bundle();
                bundle.putInt("achievementType", type);
                fragment = new InfoAchievementsFragment();
                fragment.setArguments(bundle);
                tag = "infoAchievementsFragment";
                ft.addToBackStack(tag);
                break;
            default:
                break;
        }

        if (fragment != null) {
            ft.replace(R.id.fragment_container_achievements, fragment, tag);
            ft.commit();
        }
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed");
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container_achievements) instanceof AchievementsFragment) {
            // GC the fetcher as user manually leave the activity
            sFetcher.setDataCallback(null);
            sFetcher = null;
        }

        super.onBackPressed();
    }

    public void showInviteConfirmationDialog(String contentText) {
        Timber.d("showInviteConfirmationDialog");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_invite_friends_achievement, null);
        TextView content = dialogLayout.findViewById(R.id.invite_content);
        content.setText(contentText);
        Button closeButton = dialogLayout.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                successDialog.dismiss();
            }
        });
        builder.setView(dialogLayout);
        successDialog = builder.create();
        successDialog.show();
    }

    public void showSnackbar(String s) {
        showSnackbar(fragmentContainer, s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_GET_CONTACTS && resultCode == RESULT_OK && data != null) {
            int sentNumber = data.getIntExtra(InviteContactActivity.KEY_SENT_NUMBER, 1);
            if (sentNumber > 1) {
                showInviteConfirmationDialog(getString(R.string.invite_sent_text_multi));
            } else {
                showInviteConfirmationDialog(getString(R.string.invite_sent_text));
            }
        }
    }
}
