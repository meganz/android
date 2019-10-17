package mega.privacy.android.app.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

import static mega.privacy.android.app.utils.LogUtil.*;

public class CustomizedGridCallRecyclerView extends RecyclerView {

    private CustomizedGridLayoutManager manager;
    public int columnWidth = -1;
    private boolean isWrapContent = false;
    private int widthTotal = 0;

    public CustomizedGridCallRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public CustomizedGridCallRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomizedGridCallRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = {
                    android.R.attr.columnWidth,
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }

        manager = new CustomizedGridLayoutManager(getContext(), 1);
        setLayoutManager(manager);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        logDebug("onMeasure-> widthSpec: " + widthSpec + ", heightSpec: " + heightSpec);
        super.onMeasure(widthSpec, heightSpec);
        if(!isWrapContent){
            logDebug("columnWidth :" + columnWidth);
            if (columnWidth > 0) {
                int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
                logDebug("spanCount: " + spanCount);
                manager.setSpanCount(spanCount);
            }
        }else{

            ViewGroup.LayoutParams params = getLayoutParams();
            logDebug("columnWidth :" + columnWidth);
            if (columnWidth > 0) {
                int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
                logDebug("spanCount: " + spanCount);
                manager.setSpanCount(spanCount);
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                setLayoutParams(params);
            }
        }
    }

    public void setWrapContent(){
        isWrapContent = true;
//		widthTotal = getMeasuredWidth();
//		ViewGroup.LayoutParams params = getLayoutParams();
//		params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//		setLayoutParams(params);
        this.invalidate();
    }

    public int findFirstCompletelyVisibleItemPosition() {
        return getLayoutManager().findFirstCompletelyVisibleItemPosition();
    }

    public int findFirstVisibleItemPosition() {
        return getLayoutManager().findFirstVisibleItemPosition();
    }

    public int getColumnWidth() {
        return columnWidth;
    }
    public void setColumnWidth(int columnWidth) {
        this.columnWidth = columnWidth;
    }

    @Override
    public CustomizedGridLayoutManager getLayoutManager() {
        return manager;
    }
}
