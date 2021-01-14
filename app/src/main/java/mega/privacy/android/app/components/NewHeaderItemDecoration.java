/*
 * https://github.com/husuxing/newiyi
 *
 * husuxing
 */
package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;

public class NewHeaderItemDecoration extends RecyclerView.ItemDecoration {

    private final static int TEXT_SIZE = 14;
    private static int LEFT_OFFSET = 73;
    private final static int TITLE_HEIGHT = 40;

    private Map<Integer, String> keys = new HashMap<>();

    private Drawable mDivider;

    private int mTitleHeight;

    private Paint mTextPaint;

    private Paint mBackgroundPaint;

    private float mTextHeight;

    private float mTextBaselineOffset;

    private Context mContext;

    private int type;

    public NewHeaderItemDecoration(Context context) {
        init(context);
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    private void init(Context mContext) {
        this.mContext = mContext;
        mDivider = ContextCompat.getDrawable(mContext,R.drawable.line_divider);
        setmTitleHeight((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,TITLE_HEIGHT,mContext.getResources().getDisplayMetrics()));
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,TEXT_SIZE,mContext.getResources().getDisplayMetrics()));
        mTextPaint.setColor(mContext.getResources().getColor(R.color.primary_text));
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        mTextHeight = fm.bottom - fm.top;
        mTextBaselineOffset = fm.bottom;
        
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setColor(mContext.getResources().getColor(R.color.white));
    }
    
    @Override
    public void onDraw(Canvas c,RecyclerView parent,RecyclerView.State state) {
        super.onDraw(c,parent,state);
        drawVertical(c,parent);
    }
    
    @Override
    public void onDrawOver(Canvas c,RecyclerView parent,RecyclerView.State state) {
        super.onDrawOver(c,parent,state);
    }
    
    @Override
    public void getItemOffsets(Rect outRect,View view,RecyclerView parent,RecyclerView.State state) {
        super.getItemOffsets(outRect,view,parent,state);
        int pos = parent.getChildViewHolder(view).getAdapterPosition();
        if (keys.containsKey(pos)) {
            outRect.set(0,mTitleHeight,0,0);
        }
        else {
            outRect.set(0,mDivider.getIntrinsicHeight(),0,0);
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
        for (int i=0; i<parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)child.getLayoutParams();
            if(type == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
                LEFT_OFFSET = 73;
            }else{
                LEFT_OFFSET = 17;
            }
            if (keys.containsKey(params.getViewLayoutPosition())) {
                top = child.getTop() - params.topMargin - mTitleHeight;
                bottom = top + mTitleHeight;
                c.drawRect(left,top,right,bottom,mBackgroundPaint);
                float x = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,17,mContext.getResources().getDisplayMetrics());
                float y = bottom - (mTitleHeight - mTextHeight) / 2 - mTextBaselineOffset;
                c.drawText(keys.get(params.getViewLayoutPosition()),x,y,mTextPaint);

                if (i != 0 && type == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
                    bottom = top + mDivider.getIntrinsicHeight();
                    float leftOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,LEFT_OFFSET,mContext.getResources().getDisplayMetrics());
                    mDivider.setBounds((int)(left + leftOffset),top,right,bottom);
                    mDivider.draw(c);
                }
            }
            else {
                if (type == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
                    top = child.getTop() - params.topMargin - mDivider.getIntrinsicHeight();
                    bottom = top + mDivider.getIntrinsicHeight();
                    float leftOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,LEFT_OFFSET,mContext.getResources().getDisplayMetrics());
                    mDivider.setBounds((int)(left + leftOffset),top,right,bottom);
                    mDivider.draw(c);
                }
            }
            //Draw the separator for the last item.
            if(i == parent.getChildCount() -1) {
				if (type == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
					float leftOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,LEFT_OFFSET,mContext.getResources().getDisplayMetrics());
					mDivider.setBounds((int)(left + leftOffset),child.getBottom(),right,child.getBottom() + mDivider.getIntrinsicHeight());
					mDivider.draw(c);
				}
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
