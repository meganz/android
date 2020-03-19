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
package mega.privacy.android.app.service.iab;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import mega.privacy.android.app.middlelayer.iab.BillingManager;
import mega.privacy.android.app.middlelayer.iab.BillingUpdatesListener;
import mega.privacy.android.app.middlelayer.iab.MegaPurchase;
import mega.privacy.android.app.middlelayer.iab.MegaSku;
import mega.privacy.android.app.middlelayer.iab.QuerySkuListCallback;
import nz.mega.sdk.MegaUser;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManagerImpl implements BillingManager {

    /**
     * Handles all the interactions with Play Store (via Billing library), maintains connection to
     * it through BillingClient and caches temporary states/data if needed.
     *
     * @param activity        The Context, here's {@link mega.privacy.android.app.lollipop.ManagerActivityLollipop}
     * @param updatesListener The callback, when billing status update. {@link BillingUpdatesListener}
     * @param pl              Payload, using MegaUser's hanlde as payload. {@link MegaUser#getHandle()}
     */
    public BillingManagerImpl(Activity activity, BillingUpdatesListener updatesListener, String pl) {}

    @Override
    public boolean isPurchased(MegaPurchase purchase) {
        return false;
    }

    @Override
    public void initiatePurchaseFlow(@Nullable String oldSku, @Nullable String purchaseToken, @NonNull MegaSku skuDetails) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void getInventory(QuerySkuListCallback callback) {

    }

    @Override
    public boolean isPayloadValid(String pl) {
        return false;
    }

    @Override
    public String getPayload() {
        return null;
    }
}

