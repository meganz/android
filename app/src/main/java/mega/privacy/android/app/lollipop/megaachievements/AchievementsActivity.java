package mega.privacy.android.app.lollipop.megaachievements;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class AchievementsActivity extends PinActivityLollipop implements MegaRequestListenerInterface {

    FrameLayout fragmentContainer;
    Toolbar tB;
    ActionBar aB;
    int visibleFragment;
    private AchievementsFragment achievementsFragment;
    private ReferralBonusesFragment referralBonusesFragment;
    private InviteFriendsFragment inviteFriendsFragment;
    private InfoAchievementsFragment infoAchievementsFragment;

    public MegaAchievementsDetails megaAchievements;
    public ArrayList<ReferralBonus> referralBonuses;

    MegaApiAndroid megaApi;

    private android.support.v7.app.AlertDialog successDialog;

    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
		super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        setContentView(R.layout.activity_achievements);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_achievements);
        tB = (Toolbar) findViewById(R.id.toolbar_achievements);
        tB.setTitle(getString(R.string.achievements_title));
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        log("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        visibleFragment = Constants.ACHIEVEMENTS_FRAGMENT;
        achievementsFragment = new AchievementsFragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_achievements, achievementsFragment, "achievementsFragment");
        ft.commitNow();

        referralBonuses = new ArrayList<>();
        megaApi.getAccountAchievements(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                if(visibleFragment==Constants.ACHIEVEMENTS_FRAGMENT){
                    finish();
                }
                else{
                    showFragment(Constants.ACHIEVEMENTS_FRAGMENT, -1);
                }

                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void showFragment(int fragment, int achievementType){
        log("showFragment: "+fragment+" type: "+achievementType);
        visibleFragment = fragment;

        if(visibleFragment==Constants.ACHIEVEMENTS_FRAGMENT){

            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

            aB.setTitle(getString(R.string.achievements_title));
            if(achievementsFragment==null){
                achievementsFragment = new AchievementsFragment();
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, achievementsFragment, "achievementsFragment");
            ft.commitNow();

            achievementsFragment.updateValues();
        }
        else if(visibleFragment==Constants.INVITE_FRIENDS_FRAGMENT){

            aB.setTitle(getString(R.string.button_invite_friends));
            if(inviteFriendsFragment==null){
                inviteFriendsFragment = new InviteFriendsFragment();
            }

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, inviteFriendsFragment, "inviteFriendsFragment");
            ft.commitNow();
        }
        else if(visibleFragment==Constants.BONUSES_FRAGMENT){

            if(referralBonusesFragment==null) {
                referralBonusesFragment = new ReferralBonusesFragment();
            }

            aB.setTitle(getString(R.string.title_referral_bonuses));
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, referralBonusesFragment, "referralBonusesFragment");
            ft.commitNow();
        }
        else if(visibleFragment==Constants.INFO_ACHIEVEMENTS_FRAGMENT){
            Bundle bundle = new Bundle();
            bundle.putInt("achievementType", achievementType);

            infoAchievementsFragment = new InfoAchievementsFragment();
            infoAchievementsFragment.setArguments(bundle);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container_achievements, infoAchievementsFragment, "infoAchievementsFragment");
            ft.commitNow();
        }
    }

    @Override
    public void onBackPressed() {
        log("onBackPressedLollipop");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
        if(visibleFragment==Constants.ACHIEVEMENTS_FRAGMENT){
            super.onBackPressed();
        }
        else{
            showFragment(Constants.ACHIEVEMENTS_FRAGMENT, -1);
        }
    }


    public static void log(String message) {
        Util.log("AchievementsActivity", message);
    }


    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: "+request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish: "+request.getRequestString()+"__"+e.getErrorCode());

        if(request.getType()==MegaRequest.TYPE_GET_ACHIEVEMENTS){
            if (e.getErrorCode() == MegaError.API_OK){

                megaAchievements=request.getMegaAchievementsDetails();
                if(megaAchievements!=null){
                    calculateReferralBonuses();
                    if(visibleFragment==Constants.ACHIEVEMENTS_FRAGMENT){
                        if(achievementsFragment.isAdded()){
                            achievementsFragment.updateValues();
                        }
                    }
                }
            }
            else{
                Snackbar.make(fragmentContainer, getString(R.string.cancel_subscription_error), Snackbar.LENGTH_LONG).show();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
            log("MegaRequest.TYPE_INVITE_CONTACT finished: "+request.getNumber());

            if (e.getErrorCode() == MegaError.API_OK){
                log("OK INVITE CONTACT: "+request.getEmail());
                showInviteConfirmationDialog();
            }
            else{
                log("Code: "+e.getErrorString());
                if(e.getErrorCode()==MegaError.API_EEXIST)
                {
                    showSnackbar(getString(R.string.context_contact_already_exists, request.getEmail()));
                }
                else{
                    showSnackbar(getString(R.string.general_error));
                }
                log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
            }
        }
    }

    public void inviteFriends(ArrayList<String> mails){
        log("inviteFriends");
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        showFragment(Constants.ACHIEVEMENTS_FRAGMENT, -1);

        ContactController cC = new ContactController(this);
        cC.inviteMultipleContacts(mails);
    }

    public void calculateReferralBonuses() {
        log("calculateReferralBonuses");

        long count = megaAchievements.getAwardsCount();

        for (int i = 0; i < count; i++) {
            int type = megaAchievements.getAwardClass(i);

            int awardId = megaAchievements.getAwardId(i);

            int rewardId = megaAchievements.getRewardAwardId(awardId);
            log("AWARD ID: " + awardId + " REWARD id: " + rewardId);

            if (type == MegaAchievementsDetails.MEGA_ACHIEVEMENT_INVITE) {

                ReferralBonus rBonus = new ReferralBonus();

                rBonus.setEmails(megaAchievements.getAwardEmails(i));

                long daysLeft = megaAchievements.getAwardExpirationTs(i);
                log("Registration AwardExpirationTs: " + daysLeft);

                Calendar start = Util.calculateDateFromTimestamp(daysLeft);
                Calendar end = Calendar.getInstance();
                Date startDate = start.getTime();
                Date endDate = end.getTime();
                long startTime = startDate.getTime();
                long endTime = endDate.getTime();
                long diffTime = startTime - endTime;
                long diffDays = diffTime / (1000 * 60 * 60 * 24);

                rBonus.setDaysLeft(diffDays);

                rBonus.setStorage(megaAchievements.getRewardStorageByAwardId(awardId));
                rBonus.setTransfer(megaAchievements.getRewardTransferByAwardId(awardId));

                referralBonuses.add(rBonus);
            } else {
                log("MEGA_ACHIEVEMENT: " + type);
            }
        }
    }

    public void showInviteConfirmationDialog(){
        log("showInviteConfirmationDialog");

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.dialog_invite_friends_achievement, null);
        builder.setView(dialoglayout);
        successDialog = builder.create();
        successDialog.show();
    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
