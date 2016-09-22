package mega.privacy.android.app.components.tokenautocomplete;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.view.ViewGroup;

/**
 * https://github.com/splitwise/TokenAutoComplete
 *
 * Copyright (c) 2013, 2014 splitwise, Wouter Dullaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");  you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 * Span that holds a view it draws when rendering
 *
 * Created on 2/3/15.
 * @author mgod
 */
public class ViewSpan extends ReplacementSpan {
    protected View view;
    private int maxWidth;

    public ViewSpan(View v, int maxWidth) {
        super();
        this.maxWidth = maxWidth;
        view = v;
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void prepView() {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        prepView();

        canvas.save();
        //Centering the token looks like a better strategy that aligning the bottom
        int padding = (bottom - top - view.getBottom()) / 2;
        canvas.translate(x, bottom - view.getBottom() - padding);
        view.draw(canvas);
        canvas.restore();
    }

    public int getSize(Paint paint, CharSequence charSequence, int i, int i2, Paint.FontMetricsInt fm) {
        prepView();

        if (fm != null) {
            //We need to make sure the layout allots enough space for the view
            int height = view.getMeasuredHeight();
            int need = height - (fm.descent - fm.ascent);
            if (need > 0) {
                int ascent = need / 2;
                //This makes sure the text drawing area will be tall enough for the view
                fm.descent += need - ascent;
                fm.ascent -= ascent;
                fm.bottom += need - ascent;
                fm.top -= need / 2;
            }
        }

        return view.getRight();
    }
}