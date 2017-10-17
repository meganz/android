/*
 *
 * https://github.com/rockerhieu/emojicon
 *
 * Copyright 2014 Hieu Rocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mega.privacy.android.app.components.emojicon;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.emojicon.EmojiconTextView;
import mega.privacy.android.app.components.emojicon.emoji.Emojicon;

import java.util.List;


class EmojiconAdapter extends ArrayAdapter<Emojicon> {
    private boolean mUseSystemDefault = false;

    public EmojiconAdapter(Context context, List<Emojicon> data) {
        super(context,R.layout.emojicon_item, data);
        mUseSystemDefault = false;
    }

    public EmojiconAdapter(Context context, List<Emojicon> data, boolean useSystemDefault) {
        super(context,R.layout.emojicon_item, data);
        mUseSystemDefault = useSystemDefault;
    }

    public EmojiconAdapter(Context context, Emojicon[] data) {
        super(context, R.layout.emojicon_item, data);
        mUseSystemDefault = false;
    }

    public EmojiconAdapter(Context context, Emojicon[] data, boolean useSystemDefault) {
        super(context,R.layout.emojicon_item, data);
        mUseSystemDefault = useSystemDefault;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = View.inflate(getContext(), R.layout.emojicon_item, null);
            ViewHolder holder = new ViewHolder();
            holder.icon = (EmojiconTextView) v.findViewById(R.id.emojicon_icon);
            holder.icon.setUseSystemDefault(mUseSystemDefault);
            v.setTag(holder);
        }
        Emojicon emoji = getItem(position);
        ViewHolder holder = (ViewHolder) v.getTag();
        holder.icon.setText(emoji.getEmoji());
        return v;
    }

    static class ViewHolder {
        EmojiconTextView icon;
    }
}