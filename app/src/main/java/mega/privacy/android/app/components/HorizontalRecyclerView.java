package mega.privacy.android.app.components;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class HorizontalRecyclerView extends RecyclerView {

    private LinearLayoutManager layoutManager;


    public HorizontalRecyclerView(Context context) {
        super(context);
        init();
    }

    public HorizontalRecyclerView(Context context,AttributeSet attrs) {
        super(context,attrs);
        init();
    }

    public HorizontalRecyclerView(Context context,AttributeSet attrs,int defStyle) {
        super(context,attrs,defStyle);
        init();
    }

    private void init() {
        layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false);
        setLayoutManager(layoutManager);
    }

    @Override
    protected void onMeasure(int widthSpec,int heightSpec) {
        super.onMeasure(widthSpec,heightSpec);
        //hide the partially visible item.
        if (layoutManager.findLastVisibleItemPosition() > layoutManager.findLastCompletelyVisibleItemPosition()) {
            getChildAt(getChildCount() - 1).setVisibility(GONE);
        }

    }
}
