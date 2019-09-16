package mega.privacy.android.app.utils;


import android.content.Context;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import nz.mega.sdk.MegaApiAndroid;

public class DBUtil {

    static DatabaseHandler dbH;
    static MegaAttributes attributes;

    public static boolean callToPricing (Context context){
        LogUtil.logDebug("callToPricing");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getPricingTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    if(oldTimestamp.equals("-1")){
                        LogUtil.logDebug("First call!! - API call getPricing");
                        return true;
                    }
                    else{
                        long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                        LogUtil.logDebug("Last call made: "+ timestampMinDifference + " min ago");
                        if(timestampMinDifference > Constants.PRICING_MIN_DIFFERENCE){
                            LogUtil.logDebug("API call getPricing");
                            return true;
                        }
                        else{
                            LogUtil.logDebug("NOT call getPricing");
                            return false;
                        }
                    }
                }
                else{
                    LogUtil.logDebug("Not valid value - API call getPricing");
                    return true;
                }
            }
            else{
                LogUtil.logDebug("Attributes is NULL - API call getPricing");
                return true;
            }

        }else{
            LogUtil.logDebug("DatabaseHandler is NULL - API call getPricing");
            return true;
        }
    }

    public static boolean callToPaymentMethods (Context context) {
        LogUtil.logDebug("callToPaymentMethods");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getPaymentMethodsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    LogUtil.logDebug("Last call made: "+ timestampMinDifference + " min ago");
                    if(timestampMinDifference > Constants.PAYMENT_METHODS_MIN_DIFFERENCE){
                        LogUtil.logDebug("API call getPaymentMethods");
                        return true;
                    }
                    else{
                        LogUtil.logDebug("NOT call getPaymentMethods");
                        return false;
                    }
                }
                else{
                    LogUtil.logDebug("Not valid value - API call getPaymentMethods");
                    return true;
                }
            }
            else{
                LogUtil.logDebug("Attributes is NULL - API call getPaymentMethods");
                return true;
            }

        }else{
            LogUtil.logDebug("DatabaseHandler is NULL - API call getPaymentMethods");
            return true;
        }
    }

    public static boolean callToAccountDetails (Context context) {
        LogUtil.logDebug("callToAccountDetails");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getAccountDetailsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    LogUtil.logDebug("Last call made: "+ timestampMinDifference + " min ago");
                    if(timestampMinDifference > Constants.ACCOUNT_DETAILS_MIN_DIFFERENCE){
                        LogUtil.logDebug("API call getAccountDetails");
                        return true;
                    }
                    else{
                        LogUtil.logDebug("NOT call getAccountDetails");
                        return false;
                    }
                }
                else{
                    LogUtil.logDebug("Not valid value - API call getAccountDetails");
                    return true;
                }
            }
            else{
                LogUtil.logDebug("Attributes is NULL - API call getAccountDetails");
                return true;
            }

        }else{
            LogUtil.logDebug("DatabaseHandler is NULL - API call getAccountDetails");
            return true;
        }
    }

    public static void resetAccountDetailsTimeStamp(Context context) {
        LogUtil.logDebug("resetAccountDetailsTimeStamp");
        dbH = DatabaseHandler.getDbHandler(context);
        if(dbH == null) return;
        dbH.resetAccountDetailsTimeStamp();
    }

    public static boolean callToExtendedAccountDetails (Context context) {
        LogUtil.logDebug("callToExtendedAccountDetails");
        dbH = DatabaseHandler.getDbHandler(context);

        if(dbH!=null){
            attributes = dbH.getAttributes();
            if(attributes!=null){
                String oldTimestamp = attributes.getExtendedAccountDetailsTimeStamp();
                if((oldTimestamp!=null)&&(oldTimestamp.trim()!="")&&(!oldTimestamp.isEmpty())){
                    if(oldTimestamp.equals("-1")){
                        LogUtil.logDebug("First call!! - API call getExtendedAccountDetails");
                        return true;
                    }
                    else{
                        long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                        LogUtil.logDebug("Last call made: "+ timestampMinDifference + " min ago");
                        if(timestampMinDifference > Constants.EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE){
                            LogUtil.logDebug("API call getExtendedAccountDetails");
                            return true;
                        }
                        else{
                            LogUtil.logDebug("NOT call getExtendedAccountDetails");
                            return false;
                        }
                    }
                }
                else{
                    LogUtil.logDebug("Not valid value - API call getExtendedAccountDetails");
                    return true;
                }
            }
            else{
                LogUtil.logDebug("Attributes is NULL - API call getExtendedAccountDetails");
                return true;
            }

        }else{
            LogUtil.logDebug("DatabaseHandler is NULL - API call getExtendedAccountDetails");
            return true;
        }
    }

    public static boolean isSendOriginalAttachments (Context context){
        LogUtil.logDebug("isSendOriginalAttachments");
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
}
