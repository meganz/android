package mega.privacy.android.app.components.tokenautocomplete;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * https://github.com/splitwise/TokenAutoComplete
 *
 * Copyright (c) 2013, 2014 splitwise, Wouter Dullaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");  you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 * Span that displays +[x]
 *
 * Created on 2/3/15.
 * @author mgod
 */

public class CountSpan extends ViewSpan {
    public String text = "";

    public CountSpan(int count, Context ctx, int textColor, int textSize, int maxWidth) {
        super(new TextView(ctx), maxWidth);
        TextView v = (TextView)view;
        v.setTextColor(textColor);
        v.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        setCount(count);
    }

    public void setCount(int c) {
        text = "+" + c;
        ((TextView)view).setText(text);
    }
}