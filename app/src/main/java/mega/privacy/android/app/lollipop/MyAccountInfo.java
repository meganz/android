package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.managerSections.CentiliFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.FortumoFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MonthlyAnnualyFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyStorageFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.UpgradeAccountFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.billing.Purchase;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaAccountSession;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

public class MyAccountInfo implements MegaRequestListenerInterface {

    int usedPerc = 0;
    int accountType = -1;
    MegaAccountDetails accountInfo = null;
    BitSet paymentBitSet = null;
    long numberOfSubscriptions = -1;
    long usedGbStorage = -1;
    String usedFormatted = "";
    String totalFormatted = "";
    String formattedUsedCloud = "";
    String formattedUsedInbox = "";
    String formattedUsedIncoming = "";
    String formattedUsedRubbish = "";
    String formattedAvailableSpace = "";
    int levelInventory = -1;
    int levelAccountDetails = -1;

    boolean inventoryFinished = false;
    boolean accountDetailsFinished = false;
    boolean getPaymentMethodsBoolean = false;

    MegaApiAndroid megaApi;

    String firstNameText = "";
    String lastNameText = "";
    boolean lastName = false;
    boolean firstName = false;
    String firstLetter;
    String fullName = "";

    String lastSessionFormattedDate;
    long createSessionTimeStamp = -1;

    DatabaseHandler dbH;
    Context context;
    MegaUser myUser = null;

    public ArrayList<Product> productAccounts;

    Purchase proLiteMonthly = null;
    Purchase proLiteYearly = null;
    Purchase proIMonthly = null;
    Purchase proIYearly = null;
    Purchase proIIMonthly = null;
    Purchase proIIYearly = null;
    Purchase proIIIMonthly = null;
    Purchase proIIIYearly = null;

    public MyAccountInfo(Context context){
        log("MyAccountInfo created");

        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void setAccountDetails(){

        if(accountInfo==null){
            log("Error because account info is NUll in setAccountDetails");
            return;
        }

        long totalStorage = accountInfo.getStorageMax();
        long usedStorage = 0;
        long usedCloudDrive = 0;
        long usedInbox = 0;
        long usedRubbish = 0;
        long usedIncoming = 0;
        boolean totalGb = false;

        //Check size of the different nodes
        if(megaApi.getRootNode()!=null){
            usedCloudDrive = accountInfo.getStorageUsed(megaApi.getRootNode().getHandle());
            formattedUsedCloud = Util.getSizeString(usedCloudDrive);
        }

        if(megaApi.getInboxNode()!=null){
            usedInbox = accountInfo.getStorageUsed(megaApi.getInboxNode().getHandle());
            formattedUsedInbox = Util.getSizeString(usedInbox);
        }

        if(megaApi.getRubbishNode()!=null){
            usedRubbish = accountInfo.getStorageUsed(megaApi.getRubbishNode().getHandle());
            formattedUsedRubbish = Util.getSizeString(usedRubbish);
        }

        ArrayList<MegaNode> nodes=megaApi.getInShares();
        if(nodes!=null){
            for(int i=0;i<nodes.size();i++){
                MegaNode nodeIn = nodes.get(i);
                usedIncoming = usedIncoming + accountInfo.getStorageUsed(nodeIn.getHandle());
            }
        }

        formattedUsedIncoming = Util.getSizeString(usedIncoming);

        totalFormatted = Util.getSizeString(totalStorage);

        usedStorage = usedCloudDrive+usedInbox+usedIncoming+usedRubbish;
        usedFormatted=Util.getSizeString(usedStorage);

        usedPerc = 0;
        if (totalStorage != 0){
            usedPerc = (int)((100 * usedStorage) / totalStorage);
        }

        long availableSpace = totalStorage - usedStorage;
        if (availableSpace < 0) {
            formattedAvailableSpace = Util.getSizeString(0);
        }
        else{
            formattedAvailableSpace = Util.getSizeString(availableSpace);
        }


//        totalStorage = ((totalStorage / 1024) / 1024) / 1024;
//        totalFormatted="";
//
//        if (totalStorage >= 1024){
//            totalStorage = totalStorage / 1024;
//            totalFormatted = totalFormatted + totalStorage + " TB";
//        }
//        else{
//            totalFormatted = totalFormatted + totalStorage + " GB";
//            totalGb = true;
//        }
//
//        usedStorage = ((usedStorage / 1024) / 1024) / 1024;
//        usedFormatted="";
//
//        if(totalGb){
//            usedGbStorage = usedStorage;
//            usedFormatted = usedFormatted + usedStorage + " GB";
//        }
//        else{
//            if (usedStorage >= 1024){
//                usedGbStorage = usedStorage;
//                usedStorage = usedStorage / 1024;
//
//                usedFormatted = usedFormatted + usedStorage + " TB";
//            }
//            else{
//                usedGbStorage = usedStorage;
//                usedFormatted = usedFormatted + usedStorage + " GB";
//            }
//        }

        accountDetailsFinished = true;

        accountType = accountInfo.getProLevel();

        switch (accountType){
            case 0:{
                levelAccountDetails = -1;
                break;
            }
            case 1:{
                levelAccountDetails = 1;
                break;
            }
            case 2:{
                levelAccountDetails = 2;
                break;
            }
            case 3:{
                levelAccountDetails = 3;
                break;
            }
            case 4:{
                levelAccountDetails = 0;
                break;
            }
        }

        log("LEVELACCOUNTDETAILS: " + levelAccountDetails + "; LEVELINVENTORY: " + levelInventory + "; INVENTORYFINISHED: " + inventoryFinished);

    }

    public MegaAccountDetails getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(MegaAccountDetails accountInfo) {
        this.accountInfo = accountInfo;
    }

    public int getAccountType() {
        return accountType;
    }

    public void setAccountType(int accountType) {
        this.accountType = accountType;
    }

    public long getNumberOfSubscriptions() {
        return numberOfSubscriptions;
    }

    public void setNumberOfSubscriptions(long numberOfSubscriptions) {
        this.numberOfSubscriptions = numberOfSubscriptions;
    }

    public BitSet getPaymentBitSet() {
        return paymentBitSet;
    }

    public void setPaymentBitSet(BitSet paymentBitSet) {
        this.paymentBitSet = paymentBitSet;
    }

    public long getUsedGbStorage() {
        return usedGbStorage;
    }

    public void setUsedGbStorage(long usedGbStorage) {
        this.usedGbStorage = usedGbStorage;
    }

    public int getUsedPerc() {
        return usedPerc;
    }

    public void setUsedPerc(int usedPerc) {
        this.usedPerc = usedPerc;
    }

    public String getTotalFormatted() {
        return totalFormatted;
    }

    public void setTotalFormatted(String totalFormatted) {
        this.totalFormatted = totalFormatted;
    }

    public String getUsedFormatted() {
        return usedFormatted;
    }

    public void setUsedFormatted(String usedFormatted) {
        this.usedFormatted = usedFormatted;
    }

    public int getLevelInventory() {
        return levelInventory;
    }

    public void setLevelInventory(int levelInventory) {
        this.levelInventory = levelInventory;
    }

    public int getLevelAccountDetails() {
        return levelAccountDetails;
    }

    public void setLevelAccountDetails(int levelAccountDetails) {
        this.levelAccountDetails = levelAccountDetails;
    }


    public boolean isAccountDetailsFinished() {
        return accountDetailsFinished;
    }

    public void setAccountDetailsFinished(boolean accountDetailsFinished) {
        this.accountDetailsFinished = accountDetailsFinished;
    }

    public boolean isInventoryFinished() {
        return inventoryFinished;
    }

    public void setInventoryFinished(boolean inventoryFinished) {
        this.inventoryFinished = inventoryFinished;

        ManagerActivityLollipop.DrawerItem drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
        if(drawerItem== ManagerActivityLollipop.DrawerItem.ACCOUNT) {
            if (((ManagerActivityLollipop) context).getAccountFragment() == Constants.UPGRADE_ACCOUNT_FRAGMENT) {
                UpgradeAccountFragmentLollipop upAFL = ((ManagerActivityLollipop) context).getUpgradeAccountFragment();
                if (upAFL != null) {
                    upAFL.setPricing();
                }
            } else if (((ManagerActivityLollipop) context).getAccountFragment() == Constants.MONTHLY_YEARLY_FRAGMENT) {
                MonthlyAnnualyFragmentLollipop myFL = ((ManagerActivityLollipop) context).getMonthlyAnnualyFragment();
                if (myFL != null) {
                    myFL.setPricing();
                }
            }
        }
    }

    public String getLastSessionFormattedDate() {
        return lastSessionFormattedDate;
    }

    public void setLastSessionFormattedDate(String lastSessionFormattedDate) {
        this.lastSessionFormattedDate = lastSessionFormattedDate;
    }

    public static void log(String message) {
        Util.log("MyAccountInfo", message);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    public void setFullName(){
        log("setFullName");
        if (firstNameText.trim().length() <= 0){
            fullName = lastNameText;
        }
        else{
            fullName = firstNameText + " " + lastNameText;
        }

        if (fullName.trim().length() <= 0){
            log("Put email as fullname");
            String email = myUser.getEmail();
            String[] splitEmail = email.split("[@._]");
            fullName = splitEmail[0];
        }

        if (fullName.trim().length() <= 0){
            fullName = context.getString(R.string.name_text)+" "+context.getString(R.string.lastname_text);
            log("Full name set by default: "+fullName);
        }

        firstLetter = fullName.charAt(0) + "";
        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish: " + request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
            log("paramType: "+request.getParamType());
            boolean avatarExists = false;
            if (e.getErrorCode() == MegaError.API_OK){
                if(request.getParamType()==MegaApiJava.USER_ATTR_AVATAR){
                    log("(0)request avatar");
                    ((ManagerActivityLollipop) context).setProfileAvatar();

                    //refresh MyAccountFragment if visible
                    ManagerActivityLollipop.DrawerItem drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                    if(drawerItem== ManagerActivityLollipop.DrawerItem.ACCOUNT){
                        log("Update the account fragment");
                        if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.MY_ACCOUNT_FRAGMENT){
                            MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
                            if(mAF!=null){
                                mAF.updateAvatar(false);
                            }
                        }
                    }
                }
                else if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
                    log("(1)request.getText(): "+request.getText());
                    firstNameText=request.getText();
                    firstName=true;
                    dbH.saveMyFirstName(firstNameText);
                }
                else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
                    log("(2)request.getText(): "+request.getText());
                    lastNameText = request.getText();
                    lastName = true;
                    dbH.saveMyLastName(lastNameText);
                }
                else if(request.getParamType() == MegaApiJava.USER_ATTR_PWD_REMINDER){
                    log("PasswordReminderFromMyAccount: "+((ManagerActivityLollipop)context).getPasswordReminderFromMyAccount());
                    if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT){
                        log("New value of attribute USER_ATTR_PWD_REMINDER: " +request.getText());
                        if (request.getFlag()){
                            ((ManagerActivityLollipop) context).showRememberPasswordDialog(true);
                        }
                        else if (((ManagerActivityLollipop)context).getPasswordReminderFromMyAccount()){
                            AccountController aC = new AccountController((ManagerActivityLollipop)context);
                            aC.logout((ManagerActivityLollipop)context, megaApi);
                        }
                    }
                    ((ManagerActivityLollipop) context).setPasswordReminderFromMyAccount(false);

                }
                if(firstName && lastName){
                    log("Name and First Name received!");

                    setFullName();
                    ((ManagerActivityLollipop) context).updateUserNameNavigationView(fullName, firstLetter);

                    firstName= false;
                    lastName = false;

                    //refresh MyAccountFragment if visible
                    ManagerActivityLollipop.DrawerItem drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                    if(drawerItem== ManagerActivityLollipop.DrawerItem.ACCOUNT){
                        log("Update the account fragment");
                        if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.MY_ACCOUNT_FRAGMENT){
                            log("MyAccount is selected!");
                            MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
                            if(mAF!=null){
                                mAF.updateNameView(fullName);
                            }
                            else{
                                log("MyAccount is Null");
                            }
                        }
                    }
                }
            }
            else{
                log("ERRR:R " + e.getErrorString() + "_" + e.getErrorCode());

                if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
                    log("ERROR - (1)request.getText(): "+request.getText());
                    firstNameText = "";
                    firstName=true;
                }
                else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
                    log("ERROR - (2)request.getText(): "+request.getText());
                    lastNameText = "";
                    lastName = true;
                }
                else if(request.getParamType()==MegaApiJava.USER_ATTR_AVATAR) {
                    if(e.getErrorCode()==MegaError.API_ENOENT) {
                        ((ManagerActivityLollipop) context).setDefaultAvatar();
                    }

                    if(e.getErrorCode()==MegaError.API_EARGS){
                        log("Error changing avatar: ");
                        if(request.getFile()!=null){
                            log("DESTINATION FILE: "+request.getFile());
                        }
                        if(request.getEmail()!=null){
                            log("email: "+request.getEmail());
                        }
                    }

                    //refresh MyAccountFragment if visible
                    ManagerActivityLollipop.DrawerItem drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                    if(drawerItem== ManagerActivityLollipop.DrawerItem.ACCOUNT){
                        log("Update the account fragment");
                        if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.MY_ACCOUNT_FRAGMENT){
                            MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
                            if(mAF!=null){
                                mAF.updateAvatar(false);
                            }
                        }
                    }
                    return;
                }
                if(firstName && lastName){
                    log("ERROR - Name and First Name received!");

                    setFullName();
                    ((ManagerActivityLollipop) context).updateUserNameNavigationView(fullName, firstLetter);

                    firstName= false;
                    lastName = false;

                    //refresh MyAccountFragment if visible
                    ManagerActivityLollipop.DrawerItem drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                    if(drawerItem== ManagerActivityLollipop.DrawerItem.ACCOUNT){
                        log("Update the account fragment");
                        if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.MY_ACCOUNT_FRAGMENT){
                            MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
                            if(mAF!=null){
                                mAF.updateNameView(fullName);
                            }
                        }
                    }
                }
            }
        }
        else if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
            log ("account_details request");
            if (e.getErrorCode() == MegaError.API_OK){

                dbH.setAccountDetailsTimeStamp();

                setAccountInfo(request.getMegaAccountDetails());

                setAccountDetails();

                if(request.getMegaAccountDetails()!=null){
                    log("getMegaAccountDetails not Null");

                    MegaAccountSession megaAccountSession = request.getMegaAccountDetails().getSession(0);

                    if(megaAccountSession!=null){
                        log("getMegaAccountSESSION not Null");
                        dbH.setExtendedAccountDetailsTimestamp();
                        long mostRecentSession = megaAccountSession.getMostRecentUsage();
                        log("The last session: "+mostRecentSession);
                        java.text.DateFormat df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault());
                        Date date = new Date(mostRecentSession * 1000);
                        Calendar cal = Calendar.getInstance();
                        TimeZone tz = cal.getTimeZone();
                        df.setTimeZone(tz);
                        lastSessionFormattedDate = df.format(date);
                        log("Formatted date: "+lastSessionFormattedDate);

                        createSessionTimeStamp = megaAccountSession.getCreationTimestamp();
                        log("createSessionTimeStamp: " + createSessionTimeStamp);
                        date = new Date(createSessionTimeStamp * 1000);
                        cal = Calendar.getInstance();
                        tz = cal.getTimeZone();
                        df.setTimeZone(tz);
                        log("createSessionTimeStamp: " + df.format(date));

                    }
                }

                if(!((ManagerActivityLollipop)context).isFinishing()){

                    ((ManagerActivityLollipop)context).updateAccountDetailsVisibleInfo();

                    //Check if myAccount section is visible
                    ManagerActivityLollipop.DrawerItem drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                    if(drawerItem== ManagerActivityLollipop.DrawerItem.ACCOUNT){
                        if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.MY_ACCOUNT_FRAGMENT){
                            MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
                            if(mAF!=null){
                                if(mAF.isAdded()){
                                    mAF.setAccountDetails();
                                }
                            }
                            MyStorageFragmentLollipop mStorageF = ((ManagerActivityLollipop) context).getMyStorageFragment();
                            if(mStorageF!=null){
                                if(mStorageF.isAdded()){
                                    mStorageF.setAccountDetails();
                                }
                            }
                        }
                        else if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.UPGRADE_ACCOUNT_FRAGMENT) {
                            UpgradeAccountFragmentLollipop upAFL = ((ManagerActivityLollipop) context).getUpgradeAccountFragment();
                            if(upAFL!=null){
                                upAFL.showAvailableAccount();
                            }
                        }
                    }
                }

                log("onRequest TYPE_ACCOUNT_DETAILS: "+getUsedPerc());
            }
        }
        else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS){
            log ("payment methods request");
            getPaymentMethodsBoolean=true;
            if (e.getErrorCode() == MegaError.API_OK){
                dbH.setPaymentMethodsTimeStamp();
                setPaymentBitSet(Util.convertToBitSet(request.getNumber()));
            }
        }
        else if(request.getType() == MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS){
            if (e.getErrorCode() == MegaError.API_OK){
                setNumberOfSubscriptions(request.getNumber());
                log("NUMBER OF SUBS: " + getNumberOfSubscriptions());
                ((ManagerActivityLollipop) context).updateCancelSubscriptions();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_GET_PRICING){
            MegaPricing p = request.getPricing();
            productAccounts = new ArrayList<Product>();

            if (e.getErrorCode() == MegaError.API_OK) {
                dbH.setPricingTimestamp();
                for (int i = 0; i < p.getNumProducts(); i++) {
                    log("p[" + i + "] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

                    Product account = new Product(p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));

                    productAccounts.add(account);
                }
                //			/*RESULTS
                //	            p[0] = 1560943707714440503__999___500___1___1___1024 - PRO 1 montly
                //        		p[1] = 7472683699866478542__9999___500___12___1___12288 - PRO 1 annually
                //        		p[2] = 7974113413762509455__1999___2048___1___2___4096  - PRO 2 montly
                //        		p[3] = 370834413380951543__19999___2048___12___2___49152 - PRO 2 annually
                //        		p[4] = -2499193043825823892__2999___4096___1___3___8192 - PRO 3 montly
                //        		p[5] = 7225413476571973499__29999___4096___12___3___98304 - PRO 3 annually*/

                //Check if myAccount section is visible
                ManagerActivityLollipop.DrawerItem drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
                if(drawerItem== ManagerActivityLollipop.DrawerItem.ACCOUNT){
                    if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.UPGRADE_ACCOUNT_FRAGMENT) {
                        UpgradeAccountFragmentLollipop upAFL = ((ManagerActivityLollipop) context).getUpgradeAccountFragment();
                        if(upAFL!=null){
                            upAFL.setPricing();
                        }
                    }
                    else if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.MONTHLY_YEARLY_FRAGMENT) {
                        MonthlyAnnualyFragmentLollipop myFL = ((ManagerActivityLollipop) context).getMonthlyAnnualyFragment();
                        if(myFL!=null){
                            myFL.setPricing();
                        }
                    }
                    else if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.CENTILI_FRAGMENT) {
                        CentiliFragmentLollipop ctFL = ((ManagerActivityLollipop) context).getCentiliFragment();
                        if(ctFL!=null){
                            ctFL.getPaymentId();
                        }
                    }
                    else if(((ManagerActivityLollipop) context).getAccountFragment()== Constants.FORTUMO_FRAGMENT) {
                        FortumoFragmentLollipop fFL = ((ManagerActivityLollipop) context).getFortumoFragment();
                        if(fFL!=null){
                            fFL.getPaymentId();
                        }
                    }
                }
            }

        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    public ArrayList<Product> getProductAccounts() {
        return productAccounts;
    }

    public void setProductAccounts(ArrayList<Product> productAccounts) {
        this.productAccounts = productAccounts;
    }


    public boolean isFirstName() {
        return firstName;
    }

    public void setFirstName(boolean firstName) {
        this.firstName = firstName;
    }

    public String getFirstNameText() {
        return firstNameText;
    }

    public void setFirstNameText(String firstNameText) {
        this.firstNameText = firstNameText;
        setFullName();
    }

    public boolean isLastName() {
        return lastName;
    }

    public void setLastName(boolean lastName) {
        this.lastName = lastName;
    }

    public String getLastNameText() {
        return lastNameText;
    }

    public void setLastNameText(String lastNameText) {
        this.lastNameText = lastNameText;
        setFullName();
    }

    public MegaUser getMyUser() {
        return myUser;
    }

    public void setMyUser(MegaUser myUser) {
        this.myUser = myUser;
    }


    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFirstLetter() {
        return firstLetter;
    }

    public String getFormattedUsedCloud() {
        return formattedUsedCloud;
    }

    public void setFormattedUsedCloud(String formattedUsedCloud) {
        this.formattedUsedCloud = formattedUsedCloud;
    }

    public String getFormattedUsedInbox() {
        return formattedUsedInbox;
    }

    public void setFormattedUsedInbox(String formattedUsedInbox) {
        this.formattedUsedInbox = formattedUsedInbox;
    }

    public String getFormattedUsedIncoming() {
        return formattedUsedIncoming;
    }

    public void setFormattedUsedIncoming(String formattedUsedIncoming) {
        this.formattedUsedIncoming = formattedUsedIncoming;
    }

    public String getFormattedUsedRubbish() {
        return formattedUsedRubbish;
    }

    public void setFormattedUsedRubbish(String formattedUsedRubbish) {
        this.formattedUsedRubbish = formattedUsedRubbish;
    }

    public String getFormattedAvailableSpace() {
        return formattedAvailableSpace;
    }

    public void setFormattedAvailableSpace(String formattedAvailableSpace) {
        this.formattedAvailableSpace = formattedAvailableSpace;
    }

    public void setFirstLetter(String firstLetter) {
        this.firstLetter = firstLetter;
    }

    public void setProLiteMonthly(Purchase proLiteMonthly) {
        this.proLiteMonthly = proLiteMonthly;
    }

    public void setProLiteYearly(Purchase proLiteYearly) {
        this.proLiteYearly = proLiteYearly;
    }

    public void setProIMonthly(Purchase proIMonthly) {
        this.proIMonthly = proIMonthly;
    }

    public void setProIYearly(Purchase proIYearly) {
        this.proIYearly = proIYearly;
    }

    public void setProIIMonthly(Purchase proIIMonthly) {
        this.proIIMonthly = proIIMonthly;
    }

    public void setProIIYearly(Purchase proIIYearly) {
        this.proIIYearly = proIIYearly;
    }

    public void setProIIIMonthly(Purchase proIIIMonthly) {
        this.proIIIMonthly = proIIIMonthly;
    }

    public void setProIIIYearly(Purchase proIIIYearly) {
        this.proIIIYearly = proIIIYearly;
    }

    public Purchase getProLiteMonthly() {
        return proLiteMonthly;
    }

    public Purchase getProLiteYearly() {
        return proLiteYearly;
    }

    public Purchase getProIMonthly() {
        return proIMonthly;
    }

    public Purchase getProIYearly() {
        return proIYearly;
    }

    public Purchase getProIIMonthly() {
        return proIIMonthly;
    }

    public Purchase getProIIYearly() {
        return proIIYearly;
    }

    public Purchase getProIIIMonthly() {
        return proIIIMonthly;
    }

    public Purchase getProIIIYearly() {
        return proIIIYearly;
    }
}
