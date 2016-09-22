package mega.privacy.android.app.components.tokenautocomplete;

import android.content.res.ColorStateList;
import android.text.style.TextAppearanceSpan;

/**
 * https://github.com/splitwise/TokenAutoComplete
 *
 * Copyright (c) 2013, 2014 splitwise, Wouter Dullaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");  you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 * Subclass of TextAppearanceSpan just to work with how Spans get detected
 *
 * Created on 2/3/15.
 * @author mgod
 */
public class HintSpan extends TextAppearanceSpan {
    public HintSpan(String family, int style, int size, ColorStateList color, ColorStateList linkColor) {
        super(family, style, size, color, linkColor);
    }
}