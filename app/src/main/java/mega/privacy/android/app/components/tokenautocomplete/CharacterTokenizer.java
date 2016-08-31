package mega.privacy.android.app.components.tokenautocomplete;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView;

import java.util.ArrayList;

/**
 * https://github.com/splitwise/TokenAutoComplete
 *
 * Copyright (c) 2013, 2014 splitwise, Wouter Dullaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");  you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 * Tokenizer with configurable array of characters to tokenize on.
 *
 * Created on 2/3/15.
 * @author mgod
 */
public class CharacterTokenizer implements MultiAutoCompleteTextView.Tokenizer {
    ArrayList<Character> splitChar;

    CharacterTokenizer(char[] splitChar){
        super();
        this.splitChar = new ArrayList<>(splitChar.length);
        for(char c : splitChar) this.splitChar.add(c);
    }

    public int findTokenStart(CharSequence text, int cursor) {
        int i = cursor;

        while (i > 0 && !splitChar.contains(text.charAt(i - 1))) {
            i--;
        }
        while (i < cursor && text.charAt(i) == ' ') {
            i++;
        }

        return i;
    }

    public int findTokenEnd(CharSequence text, int cursor) {
        int i = cursor;
        int len = text.length();

        while (i < len) {
            if (splitChar.contains(text.charAt(i))) {
                return i;
            } else {
                i++;
            }
        }

        return len;
    }

    public CharSequence terminateToken(CharSequence text) {
        int i = text.length();

        while (i > 0 && text.charAt(i - 1) == ' ') {
            i--;
        }

        if (i > 0 && splitChar.contains(text.charAt(i - 1))) {
            return text;
        } else {
            // Try not to use a space as a token character
            String token = (splitChar.size()>1 && splitChar.get(0)==' ' ? splitChar.get(1) : splitChar.get(0))+" ";
            if (text instanceof Spanned) {
                SpannableString sp = new SpannableString(text + token);
                TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                        Object.class, sp, 0);
                return sp;
            } else {
                return text + token;
            }
        }
    }
}