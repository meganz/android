/*
 * android2
 * mega.privacy.android.app.components
 *
 * Created by Ash Wu on 31/07/18 11:42 AM.
 * Copyright (c) 2018 mega.co.nz
 */
package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.R;

public class FloatingItemDecoration extends RecyclerView.ItemDecoration {
    
    private final static int TEXT_SIZE = 14;
    private final static int LEFT_OFFSET = 73;
    private final static int TITLE_HEIGHT = 40;
    
    private Map<Integer, String> keys = new HashMap<>();
    
    private Drawable mDivider;
    
    private int mTitleHeight;
    
    private Paint mTextPaint;
    
    private Paint mBackgroundPaint;
    
    private float mTextHeight;
    
    private float mTextBaselineOffset;
    
    private Context mContext;
    
    private boolean showFloatingHeaderOnScrolling = true;
    
    public FloatingItemDecoration(Context context) {
        init(context);
    }
    
    private void init(Context mContext) {
        this.mContext = mContext;
        mDivider = ContextCompat.getDrawable(mContext,R.drawable.line_divider);
        setmTitleHeight((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,TITLE_HEIGHT,mContext.getResources().getDisplayMetrics()));
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,TEXT_SIZE,mContext.getResources().getDisplayMetrics()));
        mTextPaint.setColor(mContext.getResources().getColor(R.color.mail_my_account));
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        mTextHeight = fm.bottom - fm.top;
        mTextBaselineOffset = fm.bottom;
        
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setColor(mContext.getResources().getColor(R.color.new_background_fragment));
    }
    
    @Override
    public void onDraw(Canvas c,RecyclerView parent,RecyclerView.State state) {
        super.onDraw(c,parent,state);
        drawVertical(c,parent);
    }
    
    @Override
    public void onDrawOver(Canvas c,RecyclerView parent,RecyclerView.State state) {
        super.onDrawOver(c,parent,state);
        if (!showFloatingHeaderOnScrolling) {
            return;
        }
        int firstVisiblePos = ((LinearLayoutManager)parent.getLayoutManager()).findFirstVisibleItemPosition();
        if (firstVisiblePos == RecyclerView.NO_POSITION) {
            return;
        }
        String title = getTitle(firstVisiblePos);
        if (TextUtils.isEmpty(title)) {
            return;
        }
        boolean flag = false;
        if (getTitle(firstVisiblePos + 1) != null && !title.equals(getTitle(firstVisiblePos + 1))) {
            View child = parent.findViewHolderForAdapterPosition(firstVisiblePos).itemView;
            if (child.getTop() + child.getMeasuredHeight() < mTitleHeight) {
                c.save();
                flag = true;
                c.translate(0,child.getTop() + child.getMeasuredHeight() - mTitleHeight);
            }
        }
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        int top = parent.getPaddingTop();
        int bottom = top + mTitleHeight;
        c.drawRect(left,top,right,bottom,mBackgroundPaint);
        float x = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,LEFT_OFFSET,mContext.getResources().getDisplayMetrics());
        float y = bottom - (mTitleHeight - mTextHeight) / 2 - mTextBaselineOffset;
        c.drawText(title,x,y,mTextPaint);
        if (flag) {
            c.restore();
        }
    }
    
    @Override
    public void getItemOffsets(Rect outRect,View view,RecyclerView parent,RecyclerView.State state) {
        super.getItemOffsets(outRect,view,parent,state);
        int pos = parent.getChildViewHolder(view).getAdapterPosition();
        if (keys.containsKey(pos)) {
            outRect.set(0,mTitleHeight,0,0);
        } else {
            outRect.set(0,1,0,0);
        }
    }
    
    private String getTitle(int position) {
        while (position >= 0) {
            if (keys.containsKey(position)) {
                return keys.get(position);
            }
            position--;
        }
        return null;
    }
    
    private void drawVertical(Canvas c,RecyclerView parent) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        int top = 0;
        int bottom = 0;
        for (int i = 0;i < parent.getChildCount();i++) {
            View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)child.getLayoutParams();
            if (keys.containsKey(params.getViewLayoutPosition())) {
                top = child.getTop() - params.topMargin - mTitleHeight;
                bottom = top + mTitleHeight;
                c.drawRect(left,top,right,bottom,mBackgroundPaint);
                float x = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,LEFT_OFFSET,mContext.getResources().getDisplayMetrics());
                float y = bottom - (mTitleHeight - mTextHeight) / 2 - mTextBaselineOffset;//
                c.drawText(keys.get(params.getViewLayoutPosition()),x,y,mTextPaint);
            } else {
                top = child.getTop() - params.topMargin - 1;
                bottom = top + 1;
                float leftOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,LEFT_OFFSET,mContext.getResources().getDisplayMetrics());
                mDivider.setBounds((int)(left + leftOffset),top,right,bottom);
                mDivider.draw(c);
            }
        }
    }
    
    public void setKeys(Map<Integer, String> keys) {
        this.keys.clear();
        this.keys.putAll(keys);
    }
    
    public void setmTitleHeight(int titleHeight) {
        this.mTitleHeight = titleHeight;
    }
}
