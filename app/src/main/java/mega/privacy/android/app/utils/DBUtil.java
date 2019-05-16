package mega.privacy.android.app.utils;


import android.content.Context;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;

public class DBUtil {

    static DatabaseHandler dbH;
    static MegaAttributes attributes;

    public static boolean callToPricing (Context context){
        log("callToPricing");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getPricingTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    if(oldTimestamp.equals("-1")){
                        log("First call!! - API call getPricing");
                        return true;
                    }
                    else{
                        long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                        log("Last call made: "+ timestampMinDifference + " min ago");
                        if(timestampMinDifference > Constants.PRICING_MIN_DIFFERENCE){
                            log("API call getPricing");
                            return true;
                        }
                        else{
                            log("NOT call getPricing");
                            return false;
                        }
                    }
                }
                else{
                    log("Not valid value - API call getPricing");
                    return true;
                }
            }
            else{
                log("Attributes is NULL - API call getPricing");
                return true;
            }

        }else{
            log("DatabaseHandler is NULL - API call getPricing");
            return true;
        }
    }

    public static boolean callToPaymentMethods (Context context) {
        log("callToPaymentMethods");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getPaymentMethodsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    log("Last call made: "+ timestampMinDifference + " min ago");
                    if(timestampMinDifference > Constants.PAYMENT_METHODS_MIN_DIFFERENCE){
                        log("API call getPaymentMethods");
                        return true;
                    }
                    else{
                        log("NOT call getPaymentMethods");
                        return false;
                    }
                }
                else{
                    log("Not valid value - API call getPaymentMethods");
                    return true;
                }
            }
            else{
                log("Attributes is NULL - API call getPaymentMethods");
                return true;
            }

        }else{
            log("DatabaseHandler is NULL - API call getPaymentMethods");
            return true;
        }
    }

    public static boolean callToAccountDetails (Context context) {
        log("callToAccountDetails");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getAccountDetailsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    log("Last call made: "+ timestampMinDifference + " min ago");
                    if(timestampMinDifference > Constants.ACCOUNT_DETAILS_MIN_DIFFERENCE){
                        log("API call getAccountDetails");
                        return true;
                    }
                    else{
                        log("NOT call getAccountDetails");
                        return false;
                    }
                }
                else{
                    log("Not valid value - API call getAccountDetails");
                    return true;
                }
            }
            else{
                log("Attributes is NULL - API call getAccountDetails");
                return true;
            }

        }else{
            log("DatabaseHandler is NULL - API call getAccountDetails");
            return true;
        }
    }

    public static void resetAccountDetailsTimeStamp(Context context) {
        log("resetAccountDetailsTimeStamp");
        dbH = DatabaseHandler.getDbHandler(context);
        if(dbH == null) return;
        dbH.resetAccountDetailsTimeStamp();
    }

    public static boolean callToExtendedAccountDetails (Context context) {
        log("callToExtendedAccountDetails");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getExtendedAccountDetailsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    if(oldTimestamp.equals("-1")){
                        log("First call!! - API call getExtendedAccountDetails");
                        return true;
                    }
                    else{
                        long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                        log("Last call made: "+ timestampMinDifference + " min ago");
                        if(timestampMinDifference > Constants.EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE){
                            log("API call getExtendedAccountDetails");
                            return true;
                        }
                        else{
                            log("NOT call getExtendedAccountDetails");
                            return false;
                        }
                    }
                }
                else{
                    log("Not valid value - API call getExtendedAccountDetails");
                    return true;
                }
            }
            else{
                log("Attributes is NULL - API call getExtendedAccountDetails");
                return true;
            }

        }else{
            log("DatabaseHandler is NULL - API call getExtendedAccountDetails");
            return true;
        }
    }

    public static boolean isSendOriginalAttachments (Context context){
        log("isSendOriginalAttachments");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            ChatSettings chatSettings = dbH.getChatSettings();
            boolean sendOriginalAttachments;

            if(chatSettings!=null){
                if(chatSettings.getEnabled()!=null){
                    sendOriginalAttachments = Boolean.parseBoolean(chatSettings.getSendOriginalAttachments());
                    return sendOriginalAttachments;
                }
                else{
                    sendOriginalAttachments=false;
                    return sendOriginalAttachments;
                }
            }
            else{
                sendOriginalAttachments=false;
                return sendOriginalAttachments;
            }
        }
        else{
            return false;
        }
    }

    private static void log(String message) {
        Util.log("DBUtil", message);
    }
}
