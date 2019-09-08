package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaPricing;

import static mega.privacy.android.app.utils.Constants.EXTRA_SHOULD_SHOW_SMS_DIALOG;

public class ChooseAccountFragmentLollipop extends Fragment implements View.OnClickListener {

    Context context;
    static int HEIGHT_ACCOUNT_LAYOUT=109;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    ActionBar aB;
    Toolbar tB;

    public ArrayList<Product> accounts;

    private LinearLayout mainLinearLayout;
    private ScrollView scrollView;

    //free elements:
    private RelativeLayout freeLayout;
    private TextView titleFree;
    private TextView storageSectionFree;
    private TextView bandwidthSectionFree;
    private TextView achievementsSectionFree;

    //pro lite elements:
    private RelativeLayout proLiteLayout;
    private TextView titleProLite;
    private TextView monthSectionProLite;
    private TextView storageSectionProLite;
    private TextView bandwidthSectionProLite;

    //pro i elements:
    private RelativeLayout pro1Layout;
    private TextView titlePro1;
    private TextView monthSectionPro1;
    private TextView storageSectionPro1;
    private TextView bandwidthSectionPro1;

    //pro ii elements:
    private RelativeLayout pro2Layout;
    private TextView titlePro2;
    private TextView monthSectionPro2;
    private TextView storageSectionPro2;
    private TextView bandwidthSectionPro2;

    //pro iii elements:
    private RelativeLayout pro3Layout;
    private TextView titlePro3;
    private TextView monthSectionPro3;
    private TextView storageSectionPro3;
    private TextView bandwidthSectionPro3;

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        final DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;

        float scaleW = Util.getScaleW(outMetrics, density);
        float scaleH = Util.getScaleH(outMetrics, density);

        if(megaApi==null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(megaChatApi==null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(((Activity)context).getApplicationContext());
        if (dbH.getCredentials() == null){
//            megaApi.localLogout();
//            AccountController aC = new AccountController(context);
//            aC.logout(context, megaApi, megaChatApi, false);
            //Show Login Fragment
            ((LoginActivityLollipop)context).showFragment(Constants.LOGIN_FRAGMENT);
        }

        accounts = new ArrayList<Product>();

        View v = inflater.inflate(R.layout.fragment_choose_account, container, false);

        tB = (Toolbar) v.findViewById(R.id.toolbar_choose_account);
        ((LoginActivityLollipop) context).showAB(tB);

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_choose_account);
        new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollView.canScrollVertically(-1)){
                    tB.setElevation(Util.px2dp(4, outMetrics));
                }
                else {
                    tB.setElevation(0);
                }
            }
        });

        mainLinearLayout = (LinearLayout) v.findViewById(R.id.choose_account_main_linear_layout);

//        //Replace elevation
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
//            mainLinearLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
//        }

        //FREE ACCOUNT
        freeLayout = (RelativeLayout) v.findViewById(R.id.choose_account_free_layout);
        freeLayout.setOnClickListener(this);
        titleFree = (TextView) v.findViewById(R.id.choose_account_free_title_text);
        titleFree.setText(getString(R.string.free_account).toUpperCase());
        storageSectionFree = (TextView) v.findViewById(R.id.storage_free);
        bandwidthSectionFree = (TextView) v.findViewById(R.id.bandwidth_free);
        achievementsSectionFree = (TextView) v.findViewById(R.id.achievements_free);
        //END -- PRO LITE ACCOUNT

        //PRO LITE ACCOUNT
        proLiteLayout = (RelativeLayout) v.findViewById(R.id.choose_account_prolite_layout);
        proLiteLayout.setOnClickListener(this);
        titleProLite = (TextView) v.findViewById(R.id.choose_account_prolite_title_text);
        titleProLite.setText(getString(R.string.prolite_account).toUpperCase());
        monthSectionProLite = (TextView) v.findViewById(R.id.month_lite);
        storageSectionProLite = (TextView) v.findViewById(R.id.storage_lite);
        bandwidthSectionProLite = (TextView) v.findViewById(R.id.bandwidth_lite);
        //END -- PRO LITE ACCOUNT

        //PRO I ACCOUNT
        pro1Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_i_layout);
        pro1Layout.setOnClickListener(this);
        titlePro1 = (TextView) v.findViewById(R.id.choose_account_pro_i_title_text);
        titlePro1.setText(getString(R.string.pro1_account).toUpperCase());
        monthSectionPro1 = (TextView) v.findViewById(R.id.month_pro_i);
        storageSectionPro1 = (TextView) v.findViewById(R.id.storage_pro_i);
        bandwidthSectionPro1 = (TextView) v.findViewById(R.id.bandwidth_pro_i);

        //END -- PRO I ACCOUNT

        //PRO II ACCOUNT
        pro2Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_ii_layout);
        pro2Layout.setOnClickListener(this);
        titlePro2 = (TextView) v.findViewById(R.id.choose_account_pro_ii_title_text);
        titlePro2.setText(getString(R.string.pro2_account).toUpperCase());
        monthSectionPro2 = (TextView) v.findViewById(R.id.month_pro_ii);
        storageSectionPro2 = (TextView) v.findViewById(R.id.storage_pro_ii);
        bandwidthSectionPro2 = (TextView) v.findViewById(R.id.bandwidth_pro_ii);
        //END -- PRO II ACCOUNT

        //PRO III ACCOUNT
        pro3Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_iii_layout);
        pro3Layout.setOnClickListener(this);
        titlePro3 = (TextView) v.findViewById(R.id.choose_account_pro_iii_title_text);
        titlePro3.setText(getString(R.string.pro3_account).toUpperCase());
        monthSectionPro3 = (TextView) v.findViewById(R.id.month_pro_iii);
        storageSectionPro3 = (TextView) v.findViewById(R.id.storage_pro_iii);
        bandwidthSectionPro3 = (TextView) v.findViewById(R.id.bandwidth_pro_iii);

        //END -- PRO III ACCOUNT

        setPricingInfo();
        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.choose_account_free_layout:{
                onFreeClick(v);
                break;
            }
            case R.id.choose_account_prolite_layout:{
                onUpgradeLiteClick(v);
                break;
            }
            case R.id.choose_account_pro_i_layout:{
                onUpgrade1Click(v);
                break;
            }
            case R.id.choose_account_pro_ii_layout:{
                onUpgrade2Click(v);
                break;
            }
            case R.id.choose_account_pro_iii_layout:{
                onUpgrade3Click(v);
                break;
            }
        }
    }


    public void onFreeClick (View view){
        log("onFreeClick");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("firstLogin", true);
        intent.putExtra(EXTRA_SHOULD_SHOW_SMS_DIALOG, true);
        intent.putExtra("upgradeAccount", false);
        intent.putExtra("newAccount", true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgrade1Click(View view) {
//		((ManagerActivity)context).showpF(1, accounts);
        log("onUpgrade1Click");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_I);
        intent.putExtra("newAccount", true);
        intent.putExtra(EXTRA_SHOULD_SHOW_SMS_DIALOG, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgrade2Click(View view) {
//		((ManagerActivity)context).showpF(2, accounts);
        log("onUpgrade2Click");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_II);
        intent.putExtra("newAccount", true);
        intent.putExtra(EXTRA_SHOULD_SHOW_SMS_DIALOG, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgrade3Click(View view) {
//		((ManagerActivity)context).showpF(3, accounts);
        log("onUpgrade3Click");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_III);
        intent.putExtra("newAccount", true);
        intent.putExtra(EXTRA_SHOULD_SHOW_SMS_DIALOG, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgradeLiteClick(View view){
//		((ManagerActivity)context).showpF(4, accounts);
        log("onUpgradeLiteClick");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_LITE);
        intent.putExtra("newAccount", true);
        intent.putExtra(EXTRA_SHOULD_SHOW_SMS_DIALOG, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public String sizeTranslation(long size, int type) {
        switch(type){
            case 0:{
                //From GB to TB
                if(size!=1024){
                    size=size/1024;
                }

                String value = new DecimalFormat("#").format(size);
                return value;
            }
        }
        return null;
    }

    public void setPricingInfo(){
        log("setPricingInfo");

        DecimalFormat df = new DecimalFormat("#.##");
        MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();

        if(myAccountInfo==null)
            return;

        MegaPricing p = myAccountInfo.getPricing();
        if(p==null){
            log("Return - getPricing NULL");
            return;
        }


        String textToShowFreeStorage = "[A] 50 GB [/A]"+getString(R.string.label_storage_upgrade_account)+" ";
        try{
            textToShowFreeStorage = textToShowFreeStorage.replace("[A]", "<font color=\'#000000\'>");
            textToShowFreeStorage = textToShowFreeStorage.replace("[/A]", "</font>");
        }
        catch (Exception e){}

        Spanned resultFreeStorage = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultFreeStorage = Html.fromHtml(textToShowFreeStorage+"<sup><small><font color=\'#ff333a\'>1</font></small></sup>",Html.FROM_HTML_MODE_LEGACY);

        }else {
            resultFreeStorage = Html.fromHtml(textToShowFreeStorage+"<sup><small><font color=\'#ff333a\'>1</font></small></sup>");
        }
        storageSectionFree.setText(resultFreeStorage);

        String textToShowFreeBandwidth = "[A] "+getString(R.string.limited_bandwith).toUpperCase()+"[/A] "+getString(R.string.label_transfer_quota_upgrade_account);
        try{
            textToShowFreeBandwidth = textToShowFreeBandwidth.replace("[A]", "<font color=\'#000000\'>");
            textToShowFreeBandwidth = textToShowFreeBandwidth.replace("[/A]", "</font>");
        }catch (Exception e){}
        Spanned resultFreeBandwidth = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultFreeBandwidth = Html.fromHtml(textToShowFreeBandwidth, Html.FROM_HTML_MODE_LEGACY);
        }else {
            resultFreeBandwidth = Html.fromHtml(textToShowFreeBandwidth);
        }
        bandwidthSectionFree.setText(resultFreeBandwidth);

        String textToShowFreeAchievements = " "+getString(R.string.footnote_achievements);
        Spanned resultFreeAchievements = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultFreeAchievements = Html.fromHtml("<sup><small><font color=\'#ff333a\'>1</font></small></sup>"+textToShowFreeAchievements, Html.FROM_HTML_MODE_LEGACY);
        }else {
            resultFreeAchievements = Html.fromHtml("<sup><small><font color=\'#ff333a\'>1</font></small></sup>"+textToShowFreeAchievements);
        }
        achievementsSectionFree.setText(resultFreeAchievements);

        //Pro
        for (int i=0;i<p.getNumProducts();i++){
            log("p["+ i +"] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

            Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));

            if(account.getLevel()==1&&account.getMonths()==1){
                log("PRO1: "+account.getStorage());
                double price = account.getAmount() / 100.00;
                String priceString = df.format(price);
                String[] s = priceString.split("\\.");

                String textMonth = "";
                if (s.length == 1) {
                    String[] s1 = priceString.split(",");
                    if (s1.length == 1) {
                        textMonth = s1[0];
                    } else if (s1.length == 2) {
                        textMonth = s1[0]+","+s1[1]+" €";
                    }
                }else if (s.length == 2) {
                    textMonth = s[0]+","+s[1]+" €";
                }

                String textToShowPro1Month = getString(R.string.type_month, textMonth);
                try{
                    textToShowPro1Month = textToShowPro1Month.replace("[A]", "<font color=\'#ff333a\'>");
                    textToShowPro1Month = textToShowPro1Month.replace("[/A]", "</font>");
                 }catch (Exception e){}
                Spanned resultPro1Month = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro1Month = Html.fromHtml(textToShowPro1Month,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro1Month = Html.fromHtml(textToShowPro1Month);
                }
                monthSectionPro1.setText(resultPro1Month);

                String textToShowPro1Storage = "[A] "+(account.getStorage() / 1024)+" TB [/A] "+getString(R.string.label_storage_upgrade_account);
                try{
                    textToShowPro1Storage = textToShowPro1Storage.replace("[A]", "<font color=\'#000000\'>");
                    textToShowPro1Storage = textToShowPro1Storage.replace("[/A]", "</font>");
                }
                catch (Exception e){}
                Spanned resultPro1Storage = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro1Storage = Html.fromHtml(textToShowPro1Storage,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro1Storage = Html.fromHtml(textToShowPro1Storage);
                }
                storageSectionPro1.setText(resultPro1Storage);


                String textToShowPro1Bandwidth = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.label_transfer_quota_upgrade_account);
                try{
                    textToShowPro1Bandwidth = textToShowPro1Bandwidth.replace("[A]", "<font color=\'#000000\'>");
                    textToShowPro1Bandwidth = textToShowPro1Bandwidth.replace("[/A]", "</font>");
                }catch (Exception e){}
                Spanned resultPro1Bandwidth = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro1Bandwidth = Html.fromHtml(textToShowPro1Bandwidth,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro1Bandwidth = Html.fromHtml(textToShowPro1Bandwidth);
                }
                bandwidthSectionPro1.setText(resultPro1Bandwidth);

            }
            else if(account.getLevel()==2&&account.getMonths()==1){
                log("PRO2: "+account.getStorage());

                double price = account.getAmount() / 100.00;
                String priceString = df.format(price);
                String[] s = priceString.split("\\.");

                String textMonth = "";
                if (s.length == 1) {
                    String[] s1 = priceString.split(",");
                    if (s1.length == 1) {
                        textMonth = s1[0];
                    } else if (s1.length == 2) {
                        textMonth = s1[0]+","+s1[1]+" €";
                    }
                }else if (s.length == 2) {
                    textMonth = s[0]+","+s[1]+" €";
                }

                String textToShowPro2Month = getString(R.string.type_month, textMonth);
                try{
                    textToShowPro2Month = textToShowPro2Month.replace("[A]", "<font color=\'#ff333a\'>");
                    textToShowPro2Month = textToShowPro2Month.replace("[/A]", "</font>");
                    textToShowPro2Month = textToShowPro2Month.replace("[B]", "<font color=\'#ff333a\'>");
                    textToShowPro2Month = textToShowPro2Month.replace("[/B]", "</font>");
                }catch (Exception e){}
                Spanned resultPro2Month = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro2Month = Html.fromHtml(textToShowPro2Month,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro2Month = Html.fromHtml(textToShowPro2Month);
                }
                monthSectionPro2.setText(resultPro2Month);

                String textToShowPro2Storage = "[A] "+(sizeTranslation(account.getStorage(), 0))+" TB [/A] "+getString(R.string.label_storage_upgrade_account);
                try{
                    textToShowPro2Storage = textToShowPro2Storage.replace("[A]", "<font color=\'#000000\'>");
                    textToShowPro2Storage = textToShowPro2Storage.replace("[/A]", "</font>");
                }
                catch (Exception e){}
                Spanned resultPro2Storage = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro2Storage = Html.fromHtml(textToShowPro2Storage,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro2Storage = Html.fromHtml(textToShowPro2Storage);
                }
                storageSectionPro2.setText(resultPro2Storage);


                String textToShowPro2Bandwidth = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.label_transfer_quota_upgrade_account);
                try{
                    textToShowPro2Bandwidth = textToShowPro2Bandwidth.replace("[A]", "<font color=\'#000000\'>");
                    textToShowPro2Bandwidth = textToShowPro2Bandwidth.replace("[/A]", "</font>");
                }catch (Exception e){}
                Spanned resultPro2Bandwidth  = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro2Bandwidth  = Html.fromHtml(textToShowPro2Bandwidth,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro2Bandwidth  = Html.fromHtml(textToShowPro2Bandwidth);
                }
                bandwidthSectionPro2.setText(resultPro2Bandwidth );
            }
            else if(account.getLevel()==3&&account.getMonths()==1){
                log("PRO3: "+account.getStorage());

                double price = account.getAmount() / 100.00;
                String priceString = df.format(price);
                String[] s = priceString.split("\\.");

                String textMonth = "";
                if (s.length == 1) {
                    String[] s1 = priceString.split(",");
                    if (s1.length == 1) {
                        textMonth = s1[0];
                    } else if (s1.length == 2) {
                        textMonth = s1[0]+","+s1[1]+" €";
                    }
                }else if (s.length == 2) {
                    textMonth = s[0]+","+s[1]+" €";
                }

                String textToShowPro3Month = getString(R.string.type_month, textMonth);
                try{
                    textToShowPro3Month = textToShowPro3Month.replace("[A]", "<font color=\'#ff333a\'>");
                    textToShowPro3Month = textToShowPro3Month.replace("[/A]", "</font>");
                    textToShowPro3Month = textToShowPro3Month.replace("[B]", "<font color=\'#ff333a\'>");
                    textToShowPro3Month = textToShowPro3Month.replace("[/B]", "</font>");
                }catch (Exception e){}
                Spanned resultPro3Month = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro3Month = Html.fromHtml(textToShowPro3Month,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro3Month = Html.fromHtml(textToShowPro3Month);
                }
                monthSectionPro3.setText(resultPro3Month);

                String textToShowPro3Storage = "[A] "+(sizeTranslation(account.getStorage(), 0))+" TB [/A] "+getString(R.string.label_storage_upgrade_account);
                try{
                    textToShowPro3Storage = textToShowPro3Storage.replace("[A]", "<font color=\'#000000\'>");
                    textToShowPro3Storage = textToShowPro3Storage.replace("[/A]", "</font>");
                }
                catch (Exception e){}
                Spanned resultPro3Storage = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro3Storage = Html.fromHtml(textToShowPro3Storage,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro3Storage = Html.fromHtml(textToShowPro3Storage);
                }
                storageSectionPro3.setText(resultPro3Storage);


                String textToShowPro3Bandwidth = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.label_transfer_quota_upgrade_account);
                try{
                    textToShowPro3Bandwidth = textToShowPro3Bandwidth.replace("[A]", "<font color=\'#000000\'>");
                    textToShowPro3Bandwidth = textToShowPro3Bandwidth.replace("[/A]", "</font>");
                }catch (Exception e){}
                Spanned resultPro3Bandwidth = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultPro3Bandwidth = Html.fromHtml(textToShowPro3Bandwidth,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultPro3Bandwidth = Html.fromHtml(textToShowPro3Bandwidth);
                }
                bandwidthSectionPro3.setText(resultPro3Bandwidth);
            }
            else if (account.getLevel()==4&&account.getMonths()==1){
                log("Lite: "+account.getStorage());

                double price = account.getAmount() / 100.00;
                String priceString = df.format(price);
                String[] s = priceString.split("\\.");
                String textMonth = "";
                if (s.length == 1) {
                    String[] s1 = priceString.split(",");
                    if (s1.length == 1) {
                        textMonth = s1[0];
                    } else if (s1.length == 2) {
                        textMonth = s1[0]+","+s1[1]+" €";
                    }
                }else if (s.length == 2) {
                    textMonth = s[0]+","+s[1]+" €";
                }

                String textToShowLiteMonth = getString(R.string.type_month, textMonth);
                try{
                    textToShowLiteMonth = textToShowLiteMonth.replace("[A]", "<font color=\'#ffa500\'>");
                    textToShowLiteMonth = textToShowLiteMonth.replace("[/A]", "</font>");
                    textToShowLiteMonth = textToShowLiteMonth.replace("[B]", "<font color=\'#ff333a\'>");
                    textToShowLiteMonth = textToShowLiteMonth.replace("[/B]", "</font>");
                }catch (Exception e){}
                Spanned resultLiteMonth = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultLiteMonth = Html.fromHtml(textToShowLiteMonth,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultLiteMonth = Html.fromHtml(textToShowLiteMonth);
                }
                monthSectionProLite.setText(resultLiteMonth);

                String textToShowLiteStorage = "[A] "+account.getStorage()+" GB [/A] "+getString(R.string.label_storage_upgrade_account);
                try{
                    textToShowLiteStorage = textToShowLiteStorage.replace("[A]", "<font color=\'#000000\'>");
                    textToShowLiteStorage = textToShowLiteStorage.replace("[/A]", "</font>");
                }
                catch (Exception e){}
                Spanned resultLiteStorage = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultLiteStorage = Html.fromHtml(textToShowLiteStorage,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultLiteStorage = Html.fromHtml(textToShowLiteStorage);
                }
                storageSectionProLite.setText(resultLiteStorage);


                String textToShowLiteBandwidth = "[A] "+(account.getTransfer() / 1024)+" TB [/A] "+getString(R.string.label_transfer_quota_upgrade_account);
                try{
                    textToShowLiteBandwidth = textToShowLiteBandwidth.replace("[A]", "<font color=\'#000000\'>");
                    textToShowLiteBandwidth = textToShowLiteBandwidth.replace("[/A]", "</font>");
                }catch (Exception e){}
                Spanned resultLiteBandwidth = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultLiteBandwidth = Html.fromHtml(textToShowLiteBandwidth,Html.FROM_HTML_MODE_LEGACY);
                }else {
                    resultLiteBandwidth = Html.fromHtml(textToShowLiteBandwidth);
                }
                bandwidthSectionProLite.setText(resultLiteBandwidth);

            }
            accounts.add(account);
        }
        //			/*RESULTS
//            p[0] = 1560943707714440503__999___500___1___1___1024 - PRO 1 montly
//    		p[1] = 7472683699866478542__9999___500___12___1___12288 - PRO 1 annually
//    		p[2] = 7974113413762509455__1999___2048___1___2___4096  - PRO 2 montly
//    		p[3] = 370834413380951543__19999___2048___12___2___49152 - PRO 2 annually
//    		p[4] = -2499193043825823892__2999___4096___1___3___8192 - PRO 3 montly
//    		p[5] = 7225413476571973499__29999___4096___12___3___98304 - PRO 3 annually*/
    }



    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        log("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public static void log(String message) {
        Util.log("ChooseAccountFragmentLollipop", message);
    }

}
