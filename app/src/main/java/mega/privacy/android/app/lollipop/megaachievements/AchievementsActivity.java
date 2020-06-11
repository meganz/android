package mega.privacy.android.app.lollipop.megaachievements;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.Constants.ACHIEVEMENTS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.BONUSES_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.INFO_ACHIEVEMENTS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.INVITE_FRIENDS_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_GET_CONTACTS;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.Util.hideKeyboard;

public class AchievementsActivity extends PinActivityLollipop {
    private static final String TAG_ACHIEVEMENTS = "achievementsFragment";

    FrameLayout fragmentContainer;
    Toolbar tB;
    ActionBar aB;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    public static AchievementsFetcher sFetcher;
    private androidx.appcompat.app.AlertDialog successDialog;

    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
		super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if(megaApi==null||megaApi.getRootNode()==null){
            logDebug("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
        }

        if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
            logDebug("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_achievements);

        fragmentContainer = findViewById(R.id.fragment_container_achievements);
        tB = findViewById(R.id.toolbar_achievements);
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        logDebug("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container_achievements, new AchievementsFragment(), TAG_ACHIEVEMENTS);
            ft.commitNow();

            sFetcher = new AchievementsFetcher();
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
        logDebug("onOptionsItemSelected");

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

    public void showFragment(int fragmentName, int type){
        logDebug("showFragment: " + fragmentName + " type: " + type);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment fragment = null;
        String tag = "";

        switch (fragmentName) {
            case ACHIEVEMENTS_FRAGMENT:
                hideKeyboard(this, InputMethodManager.HIDE_NOT_ALWAYS);
                aB.setTitle(getString(R.string.achievements_title));
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
        logDebug("onBackPressedLollipop");

        if(getSupportFragmentManager().findFragmentById(R.id.fragment_container_achievements) instanceof AchievementsFragment){
            // GC the fetcher as user manually leave the activity
            sFetcher.setDataCallback(null);
            sFetcher = null;
        }

        super.onBackPressed();
    }

    public void showInviteConfirmationDialog(String contentText){
        logDebug("showInviteConfirmationDialog");

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
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

    public void showSnackbar(String s){
        showSnackbar(fragmentContainer, s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_GET_CONTACTS && resultCode == RESULT_OK && data != null) {
            String email = data.getStringExtra(InviteContactActivity.KEY_SENT_EMAIL);
            int sentNumber = data.getIntExtra(InviteContactActivity.KEY_SENT_NUMBER, 1);
            if (sentNumber > 1) {
                showInviteConfirmationDialog(getString(R.string.invite_sent_text_multi));
            } else {
                showInviteConfirmationDialog(getString(R.string.invite_sent_text, email));
            }
        }
    }
}
