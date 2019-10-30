/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mega.privacy.android.app.utils.billing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app product's listing details.
 */
public class SkuDetails {

    private static final String KEY_PRODUCTION_ID = "productId";
    private static final String KEY_TYPE = "type";
    private static final String KEY_PRICE = "price";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";

    private String mItemType;
    private String mSku;
    private String mType;
    private String mPrice;
    private String mTitle;
    private String mDescription;
    private String mJson;

    SkuDetails(String itemType, String jsonSkuDetails) throws JSONException {
        mItemType = itemType;
        mJson = jsonSkuDetails;
        JSONObject o = new JSONObject(mJson);
        mSku = o.optString(KEY_PRODUCTION_ID);
        mType = o.optString(KEY_TYPE);
        mPrice = o.optString(KEY_PRICE);
        mTitle = o.optString(KEY_TITLE);
        mDescription = o.optString(KEY_DESCRIPTION);
    }

    public String getSku() { return mSku; }
    public String getType() { return mType; }
    public String getPrice() { return mPrice; }
    public String getTitle() { return mTitle; }
    public String getDescription() { return mDescription; }
    public String getmItemType() { return mItemType; }

    @Override
    public String toString() {
        return "SkuDetails:" + mJson;
    }
}
