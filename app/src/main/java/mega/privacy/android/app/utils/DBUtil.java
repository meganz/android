package mega.privacy.android.app.utils;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;

import static mega.privacy.android.app.utils.LogUtil.*;

public class DBUtil {

    private static DatabaseHandler dbH;
    private static MegaAttributes attributes;

    public static boolean callToPricing (){
        logDebug("callToPricing");
        dbH = MegaApplication.getInstance().getDbH();

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getPricingTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    if(oldTimestamp.equals("-1")){
                        logDebug("First call!! - API call getPricing");
                        return true;
                    }
                    else{
                        long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                        logDebug("Last call made: "+ timestampMinDifference + " min ago");
                        if(timestampMinDifference > Constants.PRICING_MIN_DIFFERENCE){
                            logDebug("API call getPricing");
                            return true;
                        }
                        else{
                            logDebug("NOT call getPricing");
                            return false;
                        }
                    }
                }
                else{
                    logDebug("Not valid value - API call getPricing");
                    return true;
                }
            }
            else{
                logDebug("Attributes is NULL - API call getPricing");
                return true;
            }

        }else{
            logDebug("DatabaseHandler is NULL - API call getPricing");
            return true;
        }
    }

    public static boolean callToPaymentMethods () {
        logDebug("callToPaymentMethods");
        dbH = MegaApplication.getInstance().getDbH();

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getPaymentMethodsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    logDebug("Last call made: "+ timestampMinDifference + " min ago");
                    if(timestampMinDifference > Constants.PAYMENT_METHODS_MIN_DIFFERENCE){
                        logDebug("API call getPaymentMethods");
                        return true;
                    }
                    else{
                        logDebug("NOT call getPaymentMethods");
                        return false;
                    }
                }
                else{
                    logDebug("Not valid value - API call getPaymentMethods");
                    return true;
                }
            }
            else{
                logDebug("Attributes is NULL - API call getPaymentMethods");
                return true;
            }

        }else{
            logDebug("DatabaseHandler is NULL - API call getPaymentMethods");
            return true;
        }
    }

    public static boolean callToAccountDetails () {
        logDebug("callToAccountDetails");
        dbH = MegaApplication.getInstance().getDbH();

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getAccountDetailsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    logDebug("Last call made: "+ timestampMinDifference + " min ago");
                    if(timestampMinDifference > Constants.ACCOUNT_DETAILS_MIN_DIFFERENCE){
                        logDebug("API call getAccountDetails");
                        return true;
                    }
                    else{
                        logDebug("NOT call getAccountDetails");
                        return false;
                    }
                }
                else{
                    logDebug("Not valid value - API call getAccountDetails");
                    return true;
                }
            }
            else{
                logDebug("Attributes is NULL - API call getAccountDetails");
                return true;
            }

        }else{
            logDebug("DatabaseHandler is NULL - API call getAccountDetails");
            return true;
        }
    }

    public static void resetAccountDetailsTimeStamp() {
        logDebug("resetAccountDetailsTimeStamp");
        dbH = MegaApplication.getInstance().getDbH();
        if(dbH == null) return;
        dbH.resetAccountDetailsTimeStamp();
    }

    public static boolean callToExtendedAccountDetails () {
        logDebug("callToExtendedAccountDetails");
        dbH = MegaApplication.getInstance().getDbH();

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getExtendedAccountDetailsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    if(oldTimestamp.equals("-1")){
                        logDebug("First call!! - API call getExtendedAccountDetails");
                        return true;
                    }
                    else{
                        long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                        logDebug("Last call made: "+ timestampMinDifference + " min ago");
                        if(timestampMinDifference > Constants.EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE){
                            logDebug("API call getExtendedAccountDetails");
                            return true;
                        }
                        else{
                            logDebug("NOT call getExtendedAccountDetails");
                            return false;
                        }
                    }
                }
                else{
                    logDebug("Not valid value - API call getExtendedAccountDetails");
                    return true;
                }
            }
            else{
                logDebug("Attributes is NULL - API call getExtendedAccountDetails");
                return true;
            }

        }else{
            logDebug("DatabaseHandler is NULL - API call getExtendedAccountDetails");
            return true;
        }
    }

    public static boolean isSendOriginalAttachments() {
        dbH = MegaApplication.getInstance().getDbH();

        if (dbH != null) {
            ChatSettings chatSettings = dbH.getChatSettings();

            if (chatSettings != null) {
                return Boolean.parseBoolean(chatSettings.getSendOriginalAttachments());
            } else {
                return false;
            }
        }

        return false;
    }
}
