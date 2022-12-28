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
package mega.privacy.android.app.middlelayer.iab;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;

/**
 * Define all billing constants
 */
public class BillingConstant {
    public static final int PAY_METHOD_RES_ID = R.string.payment_method_google_wallet;
    public static final int PAY_METHOD_ICON_RES_ID = R.drawable.ic_google_wallet;
    public static final int PAYMENT_GATEWAY = MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET;

    public static final String SUBSCRIPTION_PLATFORM_PACKAGE_NAME = "com.android.vending";
    public static final String SUBSCRIPTION_LINK_FOR_APP_STORE = "http://play.google.com/store/account/subscriptions";
    public static final String SUBSCRIPTION_LINK_FOR_BROWSER = "http://play.google.com/store/account/subscriptions";
}

