package mega.privacy.android.app.utils;

import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.data.model.MegaAttributes;
import timber.log.Timber;

public class DBUtil {

    private static DatabaseHandler dbH;
    private static MegaAttributes attributes;

    public static boolean callToPricing() {
        Timber.d("callToPricing");
        dbH = MegaApplication.getInstance().getDbH();

        attributes = dbH.getAttributes();
        if (attributes != null) {
            String oldTimestamp = attributes.getPricingTimeStamp();
            if ((oldTimestamp != null) && (oldTimestamp.trim() != "") && (!oldTimestamp.isEmpty())) {
                if (oldTimestamp.equals("-1")) {
                    Timber.d("First call!! - API call getPricing");
                    return true;
                } else {
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    Timber.d("Last call made: %d min ago", timestampMinDifference);
                    if (timestampMinDifference > Constants.PRICING_MIN_DIFFERENCE) {
                        Timber.d("API call getPricing");
                        return true;
                    } else {
                        Timber.d("NOT call getPricing");
                        return false;
                    }
                }
            } else {
                Timber.d("Not valid value - API call getPricing");
                return true;
            }
        } else {
            Timber.d("Attributes is NULL - API call getPricing");
            return true;
        }
    }

    public static boolean callToPaymentMethods() {
        Timber.d("callToPaymentMethods");
        dbH = MegaApplication.getInstance().getDbH();

        attributes = dbH.getAttributes();
        if (attributes != null) {
            String oldTimestamp = attributes.getPaymentMethodsTimeStamp();
            if ((oldTimestamp != null) && (oldTimestamp.trim() != "") && (!oldTimestamp.isEmpty())) {
                long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                Timber.d("Last call made: %d min ago", timestampMinDifference);
                if (timestampMinDifference > Constants.PAYMENT_METHODS_MIN_DIFFERENCE) {
                    Timber.d("API call getPaymentMethods");
                    return true;
                } else {
                    Timber.d("NOT call getPaymentMethods");
                    return false;
                }
            } else {
                Timber.d("Not valid value - API call getPaymentMethods");
                return true;
            }
        } else {
            Timber.d("Attributes is NULL - API call getPaymentMethods");
            return true;
        }
    }

    public static void resetAccountDetailsTimeStamp() {
        Timber.d("resetAccountDetailsTimeStamp");
        dbH = MegaApplication.getInstance().getDbH();
        dbH.resetAccountDetailsTimeStamp();
    }

    public static boolean callToExtendedAccountDetails() {
        Timber.d("callToExtendedAccountDetails");
        dbH = MegaApplication.getInstance().getDbH();

        attributes = dbH.getAttributes();
        if (attributes != null) {
            String oldTimestamp = attributes.getExtendedAccountDetailsTimeStamp();
            if ((oldTimestamp != null) && (oldTimestamp.trim() != "") && (!oldTimestamp.isEmpty())) {
                if (oldTimestamp.equals("-1")) {
                    Timber.d("First call!! - API call getExtendedAccountDetails");
                    return true;
                } else {
                    long timestampMinDifference = Util.calculateTimestampMinDifference(oldTimestamp);
                    Timber.d("Last call made: %d min ago", timestampMinDifference);
                    if (timestampMinDifference > Constants.EXTENDED_ACCOUNT_DETAILS_MIN_DIFFERENCE) {
                        Timber.d("API call getExtendedAccountDetails");
                        return true;
                    } else {
                        Timber.d("NOT call getExtendedAccountDetails");
                        return false;
                    }
                }
            } else {
                Timber.d("Not valid value - API call getExtendedAccountDetails");
                return true;
            }
        } else {
            Timber.d("Attributes is NULL - API call getExtendedAccountDetails");
            return true;
        }
    }
}
