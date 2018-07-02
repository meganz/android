package mega.privacy.android.app.lollipop;

import android.content.Context;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.billing.Purchase;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPricing;

public class MyAccountInfo {

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

    public ArrayList<Product> productAccounts;

    Purchase proLiteMonthly = null;
    Purchase proLiteYearly = null;
    Purchase proIMonthly = null;
    Purchase proIYearly = null;
    Purchase proIIMonthly = null;
    Purchase proIIYearly = null;
    Purchase proIIIMonthly = null;
    Purchase proIIIYearly = null;

    MegaPricing pricing;

    public MyAccountInfo(Context context){
        log("MyAccountInfo created");

        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) context).getMegaApi();
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
    }

    public String getLastSessionFormattedDate() {
        return lastSessionFormattedDate;
    }

    public void setLastSessionFormattedDate(String lastSessionFormattedDate) {
        log("setLastSessionFormattedDate: "+lastSessionFormattedDate);
        this.lastSessionFormattedDate = lastSessionFormattedDate;
    }

    public static void log(String message) {
        Util.log("MyAccountInfo", message);
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
            String email = megaApi.getMyUser().getEmail();
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

    public void setProductAccounts(MegaPricing p){
        log("setProductAccounts");

        if(productAccounts==null){
            productAccounts = new ArrayList<Product>();
        }
        else{
            productAccounts.clear();
        }

        for (int i = 0; i < p.getNumProducts(); i++) {
            log("p[" + i + "] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

            Product account = new Product(p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));

            productAccounts.add(account);
        }
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

    public boolean isGetPaymentMethodsBoolean() {
        return getPaymentMethodsBoolean;
    }

    public void setGetPaymentMethodsBoolean(boolean getPaymentMethodsBoolean) {
        this.getPaymentMethodsBoolean = getPaymentMethodsBoolean;
    }

    public long getCreateSessionTimeStamp() {
        return createSessionTimeStamp;
    }

    public void setCreateSessionTimeStamp(long createSessionTimeStamp) {
        log("setCreateSessionTimeStamp: " + createSessionTimeStamp);
        this.createSessionTimeStamp = createSessionTimeStamp;
    }

    public MegaPricing getPricing() {
        return pricing;
    }

    public void setPricing(MegaPricing pricing) {
        this.pricing = pricing;
    }
}
