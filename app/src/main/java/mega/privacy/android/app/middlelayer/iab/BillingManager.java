package mega.privacy.android.app.middlelayer.iab;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import static mega.privacy.android.app.utils.billing.PaymentUtils.*;

/**
 * Defines generic methods to handle with in-app purchase.
 */
public interface BillingManager {

    /** For HMS only, the value should be OrderStatusCode.ORDER_STATE_SUCCESS */
    int ORDER_STATE_SUCCESS = 0;

    /**
     * Available subscriptions, subscription id depends on current platform GMS or HMS.
     */
    List<String> IN_APP_SKUS = Arrays.asList(
            SKU_PRO_I_MONTH,
            SKU_PRO_I_YEAR,
            SKU_PRO_II_MONTH,
            SKU_PRO_II_YEAR,
            SKU_PRO_III_MONTH,
            SKU_PRO_III_YEAR,
            SKU_PRO_LITE_MONTH,
            SKU_PRO_LITE_YEAR);

    /**
     * For HMS resolution
     */
    interface RequestCode {

        /**
         * requestCode for pull up the pmsPay page
         */
        int REQ_CODE_BUY = 4002;

        /**
         * requestCode for pull up the login page or agreement page for createPurchaseIntentWithPrice interface
         */
        int REQ_CODE_BUYWITHPRICE_CONTINUE = 4005;

        /**
         * requestCode for pull up the login page for isEnvReady interface
         */
        int REQ_CODE_LOGIN = 2001;
    }

    /**
     * Check if the purchase is purchased.
     *
     * @param purchase In-app subsciption object.
     * @return true if purchased, false otherwise.
     */
    boolean isPurchased(MegaPurchase purchase);

    /**
     * Launch a purchase flow.
     *
     * @param oldSku Subscription id of current active subscription.
     *               Currentlt it's only useful on GMS for downgrade purchase.
     * @param purchaseToken Subscription token of current active subscription.
     *                      Currentlt it's only useful on GMS for downgrade purchase.
     * @param skuDetails The new subscription about to purchase.
     */
    void initiatePurchaseFlow(@Nullable String oldSku, @Nullable String purchaseToken, @NonNull MegaSku skuDetails);

    /**
     * Execute when Acrivity destorys. Implementation depends on current platform, GMS or HMS.
     */
    void destroy();

    /**
     * Get currently available subscriptions from platform.
     *
     * @param callback Get subscriptions callback.
     * @see QuerySkuListCallback
     */
    void getInventory(QuerySkuListCallback callback);

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener.
     * This method executes every time when BillingManager initialize to get current subscrptions.
     */
    void queryPurchases();

    /**
     * Refresh current subscriptions after a purchase is made.
     */
    void updatePurchase();

    /**
     * Verify signature of a purchase.
     *
     * @param signedData The content of a purchase.
     * @param signature  The signature of a purchase.
     * @return If the purchase is valid.
     */
    boolean verifyValidSignature(String signedData, String signature);

    /**
     * For HMS only.
     * The purchase result is wrapped in an Intent object returned by HMS Activity.
     *
     * @param data Returned data contains purchase result.
     * @return Purchase result.
     */
    int getPurchaseResult(Intent data);
}
