/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mega.privacy.android.app.utils.billing;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logInfo;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "iab"; //todo to be removed before release
    private BillingClient mBillingClient;
    private boolean mIsServiceConnected;
    private final BillingUpdatesListener mBillingUpdatesListener;
    private final Activity mActivity;
    private final List<Purchase> mPurchases = new ArrayList<>();
    private static final String BASE64_ENCODED_PUBLIC_KEY_1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0bZjbgdGRd6/hw5/J2FGTkdG";
    private static final String BASE64_ENCODED_PUBLIC_KEY_2 = "tDTMdR78hXKmrxCyZUEvQlE/DJUR9a/2ZWOSOoaFfi9XTBSzxrJCIa+gjj5wkyIwIrzEi";
    private static final String BASE64_ENCODED_PUBLIC_KEY_3 = "55k9FIh3vDXXTHJn4oM9JwFwbcZf1zmVLyes5ld7+G15SZ7QmCchqfY4N/a/qVcGFsfwqm";
    private static final String BASE64_ENCODED_PUBLIC_KEY_4 = "RU3VzOUwAYHb4mV/frPctPIRlJbzwCXpe3/mrcsAP+k6ECcd19uIUCPibXhsTkNbAk8CRkZ";
    private static final String BASE64_ENCODED_PUBLIC_KEY_5 = "KOy+czuZWfjWYx3Mp7srueyQ7xF6/as6FWrED0BlvmhJYj0yhTOTOopAXhGNEk7cUSFxqP2FKYX8e3pHm/uNZvKcSrLXbLUhQnULhn4WmKOQIDAQAB";
    private static final String BASE_64_ENCODED_PUBLIC_KEY = BASE64_ENCODED_PUBLIC_KEY_1 + BASE64_ENCODED_PUBLIC_KEY_2 + BASE64_ENCODED_PUBLIC_KEY_3 + BASE64_ENCODED_PUBLIC_KEY_4 + BASE64_ENCODED_PUBLIC_KEY_5;

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished();

        void onConsumeFinished(String token, @BillingResponseCode int result);

        void onPurchasesUpdated(int resultCode, List<Purchase> purchases);
    }

    public BillingManager(Activity activity, final BillingUpdatesListener updatesListener) {
        mActivity = activity;
        mBillingUpdatesListener = updatesListener;

        //must enable pending purchases to use billing library
        mBillingClient = BillingClient.newBuilder(mActivity).enablePendingPurchases().setListener(this).build();

        // Start setup. This is asynchronous and the specified listener will be called
        // once setup completes.
        // It also starts to report all the new purchases through onPurchasesUpdated() callback.
        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                log("service connected, query purchases");
                // Notifying the listener that billing client is ready
                mBillingUpdatesListener.onBillingClientSetupFinished();
                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                queryPurchases();
            }
        });
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        int resultCode = billingResult.getResponseCode();
        log("Purchases updated, response code is " + resultCode);
        if (resultCode == BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
            log("total purchased are: " + mPurchases.size());
        }
        mBillingUpdatesListener.onPurchasesUpdated(resultCode, mPurchases);
    }

    /**
     * Start a purchase or subscription replace flow
     */
    public void initiatePurchaseFlow(final String oldSku, final SkuDetails skuDetails) {
        log("oldSku is:" + oldSku + ", new sku is:" + skuDetails);
        Runnable purchaseFlowRequest = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams purchaseParams = BillingFlowParams
                        .newBuilder()
                        .setSkuDetails(skuDetails)
                        .setOldSku(oldSku)
                        .build();
                mBillingClient.launchBillingFlow(mActivity, purchaseParams);
            }
        };

        executeServiceRequest(purchaseFlowRequest);
    }

    /**
     * Clear the resources
     */
    public void destroy() {
        log("on destroy");
        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }

    public void querySkuDetailsAsync(@SkuType final String itemType, final List<String> skuList,
                                     final SkuDetailsResponseListener listener) {
        log("querySkuDetailsAsync type is " + itemType);
        // Creating a runnable from the request to use it inside our connection retry policy below
        Runnable queryRequest = new Runnable() {
            @Override
            public void run() {
                // Query the purchase async
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(itemType);
                mBillingClient.querySkuDetailsAsync(params.build(),
                        new SkuDetailsResponseListener() {
                            @Override
                            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                                listener.onSkuDetailsResponse(billingResult, skuDetailsList);
                            }
                        });
            }
        };

        executeServiceRequest(queryRequest);
    }

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * See {@link Security#verifyPurchase(String, String, String)}
     * </p>
     *
     * @param purchase Purchase to be handled
     */
    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            log("invalid purchase found");
            return;
        }

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                        if (billingResult.getResponseCode() == BillingResponseCode.OK) {
                            logInfo("purchase acknowledged");
                        } else {
                            logWarning("purchase acknowledge failed");
                        }
                    }
                };
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }

        log("new purchase added, " + purchase.getOriginalJson());
        mPurchases.add(purchase);
    }

    /**
     * Handle a result from querying of purchases and report an updated list to the listener
     */
    private void onQueryPurchasesFinished(PurchasesResult result) {
        log("onQueryPurchasesFinished, succeed? " + (result.getResponseCode() == BillingResponseCode.OK));
        // Have we been disposed of in the meantime? If so, or bad result code, then quit
        if (mBillingClient == null || result.getResponseCode() != BillingResponseCode.OK) {
            return;
        }

        // Update the UI and purchases inventory with new list of purchases
        mPurchases.clear();
        onPurchasesUpdated(result.getBillingResult(), result.getPurchasesList());
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    private boolean areSubscriptionsSupported() {
        int responseCode = mBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS).getResponseCode();
        log("areSubscriptionsSupported " + (responseCode == BillingResponseCode.OK));
        return responseCode == BillingResponseCode.OK;
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    private void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                log("queryPurchases good");
                PurchasesResult purchasesResult = mBillingClient.queryPurchases(SkuType.INAPP);

                // If there are subscriptions supported, we add subscription rows as well
                if (areSubscriptionsSupported()) {
                    PurchasesResult subscriptionResult = mBillingClient.queryPurchases(SkuType.SUBS);
                    if (subscriptionResult.getResponseCode() == BillingResponseCode.OK) {
                        purchasesResult.getPurchasesList().addAll(subscriptionResult.getPurchasesList());
                    }
                }

                //verify all available purchases
                List<Purchase> list = new ArrayList<>();
                for (Purchase purchase : purchasesResult.getPurchasesList()) {
                    if (verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
                        list.add(purchase);
                        log("purchase added, " + purchase.getOriginalJson());
                    }
                }
                PurchasesResult finalResult = new PurchasesResult(purchasesResult.getBillingResult(), list);
                log("final purchase result is " + finalResult.getBillingResult());
                onQueryPurchasesFinished(finalResult);
            }
        };

        executeServiceRequest(queryToExecute);
    }

    private void startServiceConnection(final Runnable executeOnSuccess) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                log("onBillingSetupFinished, response code is " + billingResult.getResponseCode());
                int BillingResponseCodeCode = billingResult.getResponseCode();

                if (BillingResponseCodeCode == BillingResponseCode.OK) {
                    log("onBillingSetupFinished OK");
                    mIsServiceConnected = true;
                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                log("billing service disconnected");
                mIsServiceConnected = false;
            }
        });
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            log("billing service was disconnected, retry once");
            startServiceConnection(runnable);
        }
    }

    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);
        } catch (IOException e) {
            logWarning("purchase failed to valid signature");
            return false;
        }
    }

    private void log(String message) {
        logDebug(TAG + ": " + message);
    }
}

