package mega.privacy.android.app.lollipop;

import java.util.BitSet;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;

/**
 * Created by carol on 6/28/16.
 */
public class AccountAttributes {

    int usedPerc = 0;
    int accountType = -1;
    MegaAccountDetails accountInfo = null;
    BitSet paymentBitSet = null;
    long numberOfSubscriptions = -1;
    long usedGbStorage = -1;
    String usedFormatted = "";
    String totalFormatted = "";
    int levelInventory = -1;
    int levelAccountDetails = -1;

    boolean inventoryFinished = false;
    boolean accountDetailsFinished = false;

    public void setAccountDetails(){
        long totalStorage = accountInfo.getStorageMax();
        long usedStorage = accountInfo.getStorageUsed();;
        boolean totalGb = false;

        usedPerc = 0;
        if (totalStorage != 0){
            usedPerc = (int)((100 * usedStorage) / totalStorage);
        }

        totalStorage = ((totalStorage / 1024) / 1024) / 1024;

        if (totalStorage >= 1024){
            totalStorage = totalStorage / 1024;
            totalFormatted = totalFormatted + totalStorage + " TB";
        }
        else{
            totalFormatted = totalFormatted + totalStorage + " GB";
            totalGb = true;
        }

        usedStorage = ((usedStorage / 1024) / 1024) / 1024;

        if(totalGb){
            usedGbStorage = usedStorage;
            usedFormatted = usedFormatted + usedStorage + " GB";
        }
        else{
            if (usedStorage >= 1024){
                usedGbStorage = usedStorage;
                usedStorage = usedStorage / 1024;

                usedFormatted = usedFormatted + usedStorage + " TB";
            }
            else{
                usedGbStorage = usedStorage;
                usedFormatted = usedFormatted + usedStorage + " GB";
            }
        }

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

    public static void log(String message) {
        Util.log("AccountAttributes", message);
    }
}
