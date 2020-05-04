/*
 *
 * https://github.com/xiaofeng-han/AndroidLibs/tree/master/flowlayoutmanager
 *
 * Copyright 2016 Xiaofeng Han
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package mega.privacy.android.app.components.flowlayoutmanager;


import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import mega.privacy.android.app.components.flowlayoutmanager.cache.CacheHelper;
import mega.privacy.android.app.components.flowlayoutmanager.cache.Line;


import java.util.LinkedList;
import java.util.List;

public class FlowLayoutManager extends RecyclerView.LayoutManager {

    private static final String LOG_TAG = "FlowLayoutManager";
    RecyclerView recyclerView;
    int firstChildAdapterPosition = 0;
    RecyclerView.Recycler recyclerRef;
    FlowLayoutOptions flowLayoutOptions;
    FlowLayoutOptions newFlowLayoutOptions;
    LayoutHelper layoutHelper;
    CacheHelper cacheHelper;
    public FlowLayoutManager() {
        flowLayoutOptions = new FlowLayoutOptions();
        newFlowLayoutOptions = FlowLayoutOptions.clone(flowLayoutOptions);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (!cacheHelper.valid() && getChildCount() != 0) {
            return;
        }

        if (cacheHelper.contentAreaWidth() != layoutHelper.visibleAreaWidth()) {
            cacheHelper.contentAreaWidth(layoutHelper.visibleAreaWidth());
        }

        recyclerRef = recycler;
        if (state.isPreLayout()) {
            onPreLayoutChildren(recycler, state);
        } else {
            cacheHelper.startBatchSetting();
            onRealLayoutChildren(recycler);
            cacheHelper.endBatchSetting();
        }

    }

    private void onPreLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {

        // start from first view child
        int firstItemAdapterPosition = getChildAdapterPosition(0);
        if (firstItemAdapterPosition == RecyclerView.NO_POSITION) {
            detachAndScrapAttachedViews(recycler);
            return;
        }
        int currentItemPosition = firstItemAdapterPosition < 0 ? 0 : firstItemAdapterPosition;
        Point point = layoutHelper.layoutStartPoint(LayoutContext.fromLayoutOptions(flowLayoutOptions));
        int x = point.x, y = point.y, height = 0;
        boolean newline;
        int real_x = point.x, real_y = point.y, real_height = 0;
        boolean real_newline;
        Rect rect = new Rect();
        Rect real_rect = new Rect();
        // detach all first.
        detachAndScrapAttachedViews(recycler);

        LayoutContext beforeContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);

        // this option use old options alignment & new options line limit to calc items for animation.
        LayoutContext afterContext = LayoutContext.clone(beforeContext);
        afterContext.layoutOptions.itemsPerLine = newFlowLayoutOptions.itemsPerLine;

        // track before removed and after removed layout in same time, to make sure only add items at
        // bottom that visible after item removed.
        while (currentItemPosition < state.getItemCount()) {
            View child = recycler.getViewForPosition(currentItemPosition);
            boolean childRemoved = isChildRemoved(child);
            // act as removed view still there, to calc new items location.
            newline = calcChildLayoutRect(child, x, y, height, beforeContext, rect);
            if (newline) {
                point = startNewline(rect, beforeContext);
                x = point.x;
                y = point.y;
                height = rect.height();
                beforeContext.currentLineItemCount = 1;
            } else {
                x = advanceInSameLine(x, rect, beforeContext);
                height = Math.max(height, rect.height());
                beforeContext.currentLineItemCount ++;
            }

            if (!childRemoved) {
                real_newline = calcChildLayoutRect(child, real_x, real_y, real_height, afterContext, real_rect);
                if (real_newline) {
                    point = startNewline(real_rect, afterContext);
                    real_x = point.x;
                    real_y = point.y;
                    real_height = real_rect.height();
                    afterContext.currentLineItemCount = 1;
                } else {
                    real_x = advanceInSameLine(real_x, real_rect, afterContext);
                    real_height = Math.max(real_height, real_rect.height());
                    afterContext.currentLineItemCount ++;
                }
            }

            // stop add new view if after removal, new items are not visible.
            if (!childVisible(true, real_x, real_y, real_x + rect.width(), real_y + rect.height())) {
                recycler.recycleView(child);
                break;
            } else {
                if (childRemoved) {
                    addDisappearingView(child);
                } else {
                    addView(child);
                }
                layoutDecorated(child, rect.left, rect.top, rect.right, rect.bottom);
            }
            currentItemPosition ++;
        }
        flowLayoutOptions = FlowLayoutOptions.clone(newFlowLayoutOptions);
    }

    private void onRealLayoutChildren(RecyclerView.Recycler recycler) {
        detachAndScrapAttachedViews(recycler);
        Point startPoint = layoutStartPoint();
        int x = startPoint.x, y = startPoint.y;
        int itemCount = getItemCount();
        int height = 0;
        boolean newLine;
        Rect rect = new Rect();
        LayoutContext layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);
        for (int i = firstChildAdapterPosition; i < itemCount; i ++) {
            View child = recycler.getViewForPosition(i);
            newLine = calcChildLayoutRect(child, x, y, height, layoutContext, rect);
            if (!childVisible(false, rect)) {
                recycler.recycleView(child);
                return;
            } else {
                addView(child);
                layoutDecorated(child, rect.left, rect.top, rect.right, rect.bottom);
                cacheHelper.setItem(i, new Point(rect.width(), rect.height()));
            }

            if (newLine) {
                Point lineInfo = startNewline(rect);
                x = lineInfo.x;
                y = lineInfo.y;
                height = rect.height();
                layoutContext.currentLineItemCount = 1;

            } else {
                x = advanceInSameLine(x, rect, layoutContext);
                height = Math.max(height, rect.height());
                layoutContext.currentLineItemCount ++;
            }
        }
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }


//    @Override
//    public boolean canScrollVertically() {
//        if (getChildCount() == 0) {
//            return false;
//        }
//
//        View firstChild = getChildAt(0);
//        View lastChild = getChildAt(getChildCount() - 1);
//        View topChild = getChildAt(getMaxHeightLayoutPositionInLine(0));
//        View bottomChild = getChildAt(getMaxHeightLayoutPositionInLine(getChildCount() - 1));
//        boolean topReached = false, bottomReached = false;
//        if (getChildAdapterPosition(firstChild) == 0) {
//            if (getDecoratedTop(topChild) >= topVisibleEdge()) {
//                topReached = true;
//            }
//        }
//
//        if (getChildAdapterPosition(lastChild) == recyclerView.getAdapter().getItemCount() - 1) {
//            if (getDecoratedBottom(bottomChild) <= bottomVisibleEdge()) {
//                bottomReached = true;
//            }
//        }
//        return !(topReached && bottomReached);
//    }

//    @Override
//    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
//        if (dy == 0) {
//            return 0;
//        }
//        if (getItemCount() == 0) {
//            return 0;
//        }
//
//        View firstChild = getChildAt(0);
//        View lastChild = getChildAt(getChildCount() - 1);
//        View topChild = getChildAt(getMaxHeightLayoutPositionInLine(0));
//        View bottomChild = getChildAt(getMaxHeightLayoutPositionInLine(getChildCount() - 1));
//        boolean topReached = false, bottomReached = false;
//        if (getChildAdapterPosition(firstChild) == 0) {
//            if (getDecoratedTop(topChild) >= topVisibleEdge()) {
//                topReached = true;
//            }
//        }
//
//        if (getChildAdapterPosition(lastChild) == recyclerView.getAdapter().getItemCount() - 1) {
//            if (getDecoratedBottom(bottomChild) <= bottomVisibleEdge()) {
//                bottomReached = true;
//            }
//        }
//
//        if (dy > 0 && bottomReached) {
//            return 0;
//        }
//
//        if (dy < 0 && topReached) {
//            return 0;
//        }
//
//        return dy > 0? contentMoveUp(dy, recycler) : contentMoveDown(dy, recycler);
//    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        this.flowLayoutOptions = FlowLayoutOptions.clone(newFlowLayoutOptions);
        if (cacheHelper != null) {
            cacheHelper.clear();
        }
        cacheHelper = new CacheHelper(flowLayoutOptions.itemsPerLine, layoutHelper.visibleAreaWidth());
        super.onItemsChanged(recyclerView);

    }

    @Override
    public void onItemsAdded(RecyclerView recyclerView, final int positionStart, final int itemCount) {
        cacheHelper.add(positionStart, itemCount);
        super.onItemsAdded(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsRemoved(RecyclerView recyclerView, final int positionStart, final int itemCount) {
        cacheHelper.remove(positionStart, itemCount);
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        cacheHelper.invalidSizes(positionStart, itemCount);
        super.onItemsUpdated(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        cacheHelper.invalidSizes(positionStart, itemCount);
        super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
    }

    @Override
    public void onItemsMoved(RecyclerView recyclerView, final int from, final int to, int itemCount) {
        cacheHelper.move(from, to, itemCount);
        super.onItemsMoved(recyclerView, from, to, itemCount);
    }

    /**
     * Contents moving up to top
     */
    private int contentMoveUp(int dy, RecyclerView.Recycler recycler) {
        int actualDy = dy;
        int maxHeightIndex = getMaxHeightLayoutPositionInLine(getChildCount() - 1);
        View maxHeightItem = getChildAt(maxHeightIndex);
        int offscreenBottom = getDecoratedBottom(maxHeightItem) - bottomVisibleEdge();
        if (offscreenBottom >= dy) {
            offsetChildrenVertical(-dy);
            return dy;
        }
        while (getChildAdapterPosition(getChildCount() - 1) < getItemCount() - 1) {
            addNewLineAtBottom(recycler);
            maxHeightIndex = getMaxHeightLayoutPositionInLine(getChildCount() - 1);
            maxHeightItem = getChildAt(maxHeightIndex);
            offscreenBottom += getDecoratedMeasuredHeight(maxHeightItem);
            if (offscreenBottom >= dy) {
                break;
            }
        }

        if (offscreenBottom < dy) {
            actualDy = offscreenBottom;
        }
        offsetChildrenVertical(-actualDy);
        while (!lineVisible(0)) {
            recycleLine(0, recycler);
        }
        firstChildAdapterPosition = getChildAdapterPosition(0);
        return actualDy;
    }

    /**
     * Contents move down to bottom
     */
    private int contentMoveDown(int dy, RecyclerView.Recycler recycler) {
        int actualDy = dy;
        int maxHeightItemIndex = getMaxHeightLayoutPositionInLine(0);
        View maxHeightItem = getChildAt(maxHeightItemIndex);

        // scrolling
        int offScreenTop = topVisibleEdge() - getDecoratedTop(maxHeightItem);
        if (offScreenTop > Math.abs(actualDy)) {
            offsetChildrenVertical(-dy);
            return dy;
        }
        while (getChildAdapterPosition(0) > 0) {
            addNewLineAtTop(recycler);
            maxHeightItemIndex = getMaxHeightLayoutPositionInLine(0);
            maxHeightItem = getChildAt(maxHeightItemIndex);
            offScreenTop += getDecoratedMeasuredHeight(maxHeightItem);
            if (offScreenTop >= Math.abs(dy)) {
                break;
            }
        }

        if (offScreenTop < Math.abs(dy)) {
            actualDy = -offScreenTop;
        }

        offsetChildrenVertical(-actualDy);
        while (!lineVisible(getChildCount() - 1)) {
            recycleLine(getChildCount() - 1, recycler);
        }
        firstChildAdapterPosition = getChildAdapterPosition(0);
        return actualDy;
    }

    /**
     * Add new line of elements at top, to keep layout, have to virtually layout from beginning.
     * Here is an example to explain why: Say total width is 10, current line is [5, 2, 1], 3 numbers
     * before current line is 7, 2, 6. If you just look back, you could say previous line [2, 6], but there
     * is another possibility: [7, 2], [6]. You don't know which one is correct without knowing how the line
     * before previous one, same thing could happen to that line too, so you have to sort everything from beginning
     * or cache the result.
     */
    private void addNewLineAtTop(RecyclerView.Recycler recycler) {
        int x = layoutStartPoint().x, bottom = getDecoratedTop(getChildAt(getMaxHeightLayoutPositionInLine(0))), y;
        int height = 0;
        List<View> lineChildren = new LinkedList<>();
        int currentAdapterPosition = 0;
        int endAdapterPosition = getChildAdapterPosition(0) - 1;
        Rect rect = new Rect();
        boolean newline;
        boolean firstItem = true;
        LayoutContext layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);

        int firstItemAdapterPosition = getChildAdapterPosition(0);
        if (cacheHelper.hasPreviousLineCached(firstItemAdapterPosition)) {
            int previousLineIndex = cacheHelper.itemLineIndex(firstItemAdapterPosition) - 1;
            Line previousLine = cacheHelper.getLine(previousLineIndex);
            int firstNewItemAdapterPosition = cacheHelper.firstItemIndex(previousLineIndex);
            for (int i = 0; i < previousLine.itemCount; i ++) {
                View newView = recycler.getViewForPosition(firstNewItemAdapterPosition + i);
                addView(newView, i);
                lineChildren.add(newView);
            }
            height = previousLine.maxHeight;
        } else {

            while (currentAdapterPosition <= endAdapterPosition) {
                View newChild = recycler.getViewForPosition(currentAdapterPosition);

                newline = calcChildLayoutRect(newChild, x, 0, height, layoutContext, rect);
                cacheHelper.setItem(currentAdapterPosition, new Point(rect.width(), rect.height()));
                // add view to make sure not be recycled.
                addView(newChild, lineChildren.size());
                if (newline && !firstItem) {
                    // end of one line, but not reach the top line yet. recycle the line and
                    // move on to next.
                    for (View viewToRecycle : lineChildren) {
                        removeAndRecycleView(viewToRecycle, recycler);
                    }
                    lineChildren.clear();
                    x = advanceInSameLine(layoutStartPoint().x, rect, layoutContext);
                    height = rect.height();
                    layoutContext.currentLineItemCount = 1;
                } else {
                    x = advanceInSameLine(x, rect, layoutContext);
                    height = Math.max(height, rect.height());
                    firstItem = false;
                    layoutContext.currentLineItemCount++;
                }
                lineChildren.add(newChild);

                currentAdapterPosition++;

            }
        }
        x = layoutStartPoint().x;
        y = bottom - height;
        firstItem = true;
        layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);
        for (int i = 0; i < lineChildren.size(); i ++) {
            View childView = lineChildren.get(i);
            newline = calcChildLayoutRect(childView, x, y, height, layoutContext, rect);
            if (newline && firstItem) {
                int rectHeight = rect.height();
                rect.top -= rectHeight;
                rect.bottom -= rectHeight;
                firstItem = false;
            }
            layoutDecorated(childView, rect.left, rect.top, rect.right, rect.bottom);
            x = advanceInSameLine(x, rect, layoutContext);
        }
    }

    /**
     * Add new line at bottom of views.
     */
    private void addNewLineAtBottom(RecyclerView.Recycler recycler) {
        int x = layoutStartPoint().x, y = getDecoratedBottom(getChildAt(getMaxHeightLayoutPositionInLine(getChildCount() - 1)));
        int childAdapterPosition = getChildAdapterPosition(getChildCount() - 1) + 1;
        // no item to add
        if (childAdapterPosition == getItemCount()) {
            return;
        }
        Rect rect = new Rect();
        boolean newline;
        boolean firstItem = true;
        LayoutContext layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);
        while (childAdapterPosition < getItemCount()) {
            View newChild = recycler.getViewForPosition(childAdapterPosition);
            newline = calcChildLayoutRect(newChild, x, y, 0, layoutContext, rect);
            cacheHelper.setItem(childAdapterPosition, new Point(rect.width(), rect.height()));
            if (newline && !firstItem) {
                recycler.recycleView(newChild);
                layoutContext.currentLineItemCount = 1;
                return;
            } else {
                addView(newChild);
                layoutDecorated(newChild, rect.left, rect.top, rect.right, rect.bottom);
                x = advanceInSameLine(x, rect, layoutContext);
                childAdapterPosition ++;
                firstItem = false;
                layoutContext.currentLineItemCount ++;
            }
        }
    }

    @Override
    public void onAttachedToWindow(final RecyclerView view) {
        super.onAttachedToWindow(view);
        this.recyclerView = view;
        layoutHelper = new LayoutHelper(this, recyclerView);
        cacheHelper = new CacheHelper(flowLayoutOptions.itemsPerLine, layoutHelper.visibleAreaWidth());
        if (layoutHelper.visibleAreaWidth() == 0) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    cacheHelper.contentAreaWidth(layoutHelper.visibleAreaWidth());
                }
            });
        }

    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return true;
    }

    @Override
    public void scrollToPosition(int position) {
        firstChildAdapterPosition = position;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(final RecyclerView recyclerView, final RecyclerView.State state, int position) {
        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return new PointF(0, getOffsetOfItemToFirstChild(targetPosition, recyclerRef));
            }
        };
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    @Override
    public void setAutoMeasureEnabled(boolean enabled) {
        super.setAutoMeasureEnabled(enabled);
    }

    private int leftVisibleEdge() {
        return getPaddingLeft();
    }

    private int rightVisibleEdge() {
        return getWidth() - getPaddingRight();
    }

    private int topVisibleEdge() {
        return getPaddingTop();
    }

    private int bottomVisibleEdge() {
        return getHeight() - getPaddingBottom();
    }

    private boolean childVisible(boolean preLayout, int left, int top, int right, int bottom) {
        if (recyclerView.getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            return true;
        }
        return Rect.intersects(new Rect(leftVisibleEdge(), topVisibleEdge(), rightVisibleEdge(), bottomVisibleEdge()),
                new Rect(left, top, right, bottom));
    }

    private boolean childVisible(boolean preLayout, Rect childRect) {
        if (!preLayout && recyclerView.getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            return true;
        }
        return Rect.intersects(new Rect(leftVisibleEdge(), topVisibleEdge(), rightVisibleEdge(), bottomVisibleEdge()), childRect);
    }

    private int getMaxHeightLayoutPositionInLine(int layoutPosition) {
        final View child = getChildAt(layoutPosition);
        int maxIndexBefore = layoutPosition, maxIndexAfter = layoutPosition, maxHeightBefore = getDecoratedMeasuredHeight(child), maxHeightAfter = getDecoratedMeasuredHeight(child);
        int currentIndex = layoutPosition;
        LayoutContext layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);
        while (currentIndex >= 0 && !isStartOfLine(currentIndex, layoutContext)) {
            final View beforeChild = getChildAt(currentIndex);
            if (getDecoratedMeasuredHeight(beforeChild) > maxHeightBefore) {
                maxIndexBefore = currentIndex;
                maxHeightBefore = getDecoratedMeasuredHeight(beforeChild);
            }
            currentIndex --;
        }
        // count in first one in line
        if (maxHeightBefore < getDecoratedMeasuredHeight(getChildAt(currentIndex))) {
            maxIndexBefore = currentIndex;
            maxHeightBefore = getDecoratedMeasuredHeight(getChildAt(currentIndex));
        }

        currentIndex = layoutPosition;
        while (currentIndex < getChildCount() && !isEndOfLine(currentIndex, layoutContext)) {
            final View afterChild = getChildAt(currentIndex);
            if (getDecoratedMeasuredHeight(afterChild) > maxHeightAfter) {
                maxIndexAfter = currentIndex;
                maxHeightAfter = getDecoratedMeasuredHeight(afterChild);
            }
            currentIndex ++;
        }
        // count in last one in line
        if (maxHeightAfter < getDecoratedMeasuredHeight(getChildAt(currentIndex))) {
            maxIndexAfter = currentIndex;
            maxHeightAfter = getDecoratedMeasuredHeight(getChildAt(currentIndex));
        }
        if (maxHeightBefore >= maxHeightAfter) {
            return maxIndexBefore;
        }
        return maxIndexAfter;
    }

    private List<View> getAllViewsInLine(int index) {
        int firstItemIndex = index;
        while(!isStartOfLine(firstItemIndex)) {
            firstItemIndex --;
        }

        List<View> viewList = new LinkedList<>();
        viewList.add(getChildAt(firstItemIndex));
        int nextItemIndex = firstItemIndex + 1;
        LayoutContext layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);
        while (nextItemIndex < getChildCount() && !isStartOfLine(nextItemIndex, layoutContext)) {
            viewList.add(getChildAt(nextItemIndex));
            nextItemIndex ++;
        }
        return viewList;
    }

    private int getChildAdapterPosition(int layoutPosition) {
        return getChildAdapterPosition(getChildAt(layoutPosition));
    }

    private int getChildAdapterPosition(View child) {
        if (child == null) {
            return RecyclerView.NO_POSITION;
        }
        return ((RecyclerView.LayoutParams)child.getLayoutParams()).getViewAdapterPosition();
    }

    public int getChildLayoutPosition(View child) {
        return ((RecyclerView.LayoutParams)child.getLayoutParams()).getViewLayoutPosition();
    }

    private boolean lineVisible(int index) {
        int maxHeightItemIndex = getMaxHeightLayoutPositionInLine(index);
        View maxHeightItem = getChildAt(maxHeightItemIndex);
        return Rect.intersects(new Rect(leftVisibleEdge(), topVisibleEdge(), rightVisibleEdge(), bottomVisibleEdge()),
                new Rect(leftVisibleEdge(), getDecoratedTop(maxHeightItem), rightVisibleEdge(), getDecoratedBottom(maxHeightItem)));
    }

    private void recycleLine(int index, RecyclerView.Recycler recycler) {
        List<View> viewList = getAllViewsInLine(index);
        for (View viewToRecycle : viewList) {
            removeAndRecycleView(viewToRecycle, recycler);
        }
    }

    private int getOffsetOfItemToFirstChild(int adapterPosition, RecyclerView.Recycler recycler) {
        int firstChildPosition = getChildAdapterPosition(0);
        if (firstChildPosition == adapterPosition) {
            // first child is target, just make sure it is fully visible.
            return topVisibleEdge() - getDecoratedTop(getChildAt(0));
        }

        if (adapterPosition > firstChildPosition) {
            int lastChildAdapterPosition = getChildAdapterPosition(getChildCount() - 1);
            // target child in screen, no need to calc.
            if (lastChildAdapterPosition >= adapterPosition) {
                int targetChildIndex = getChildCount() - 1 - (lastChildAdapterPosition - adapterPosition);
                return getDecoratedTop(getChildAt(targetChildIndex)) - topVisibleEdge();
            } else {
                // target child is below screen edge
                int y = getDecoratedBottom(getChildAt(getMaxHeightLayoutPositionInLine(getChildCount() - 1))) - topVisibleEdge();
                int targetAdapterPosition = lastChildAdapterPosition + 1;
                int x = layoutStartPoint().x;
                int height = 0;
                Rect rect = new Rect();
                boolean newline;
                LayoutContext layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);
                while (targetAdapterPosition != adapterPosition) {
                    View nextChild = recycler.getViewForPosition(targetAdapterPosition);
                    newline = calcChildLayoutRect(nextChild, x, y, height, layoutContext, rect);
                    if (newline) {
                        x = advanceInSameLine(layoutStartPoint().x, rect, layoutContext);
                        y = rect.top;
                        height = rect.height();
                        layoutContext.currentLineItemCount = 1;
                    } else {
                        x = advanceInSameLine(x, rect, layoutContext);
                        height = Math.max(height, getDecoratedMeasuredHeight(nextChild));
                        layoutContext.currentLineItemCount ++;
                    }
                    recycler.recycleView(nextChild);
                    targetAdapterPosition ++;
                }
                return y;
            }
        } else {
            // target is off screen top, Need to start from beginning in data set
            int targetAdapterPosition = 0, x = layoutStartPoint().x, height = 0;
            int y = topVisibleEdge() - getDecoratedTop(getChildAt(0));
            Rect rect = new Rect();
            boolean newline;
            LayoutContext layoutContext = LayoutContext.fromLayoutOptions(flowLayoutOptions);
            while (targetAdapterPosition <= firstChildPosition) {
                View child = recycler.getViewForPosition(targetAdapterPosition);
                newline = calcChildLayoutRect(child, x, y, height, rect);
                if (newline) {
                    x = advanceInSameLine(layoutStartPoint().x, rect);
                    height = rect.height();
                    if (targetAdapterPosition >= adapterPosition) {
                        y += height;
                    }
                    layoutContext.currentLineItemCount = 1;
                } else {
                    x = advanceInSameLine(x, rect);
                    height = Math.max(height, getDecoratedMeasuredHeight(child));
                    layoutContext.currentLineItemCount ++;
                }
                targetAdapterPosition ++;
            }
            return -y;
        }
    }

    /**
     * Is child has been marked as removed.
     */
    private boolean isChildRemoved(View child) {
        return ((RecyclerView.LayoutParams)child.getLayoutParams()).isItemRemoved();
    }

    public FlowLayoutManager setAlignment(Alignment alignment) {
        newFlowLayoutOptions.alignment = alignment;
        return this;
    }

    public FlowLayoutManager singleItemPerLine() {
        if (layoutHelper == null) {
            flowLayoutOptions.itemsPerLine = 1;
        } else {
            newFlowLayoutOptions.itemsPerLine = 1;
        }
        if (cacheHelper != null) {
            cacheHelper.clear();
        }
        if (layoutHelper != null) {
            cacheHelper = new CacheHelper(1, layoutHelper.visibleAreaWidth());
        }
        return this;
    }

    public FlowLayoutManager maxItemsPerLine(int itemsPerLine) {
        if (layoutHelper == null) {
            flowLayoutOptions.itemsPerLine = itemsPerLine;
        } else {
            newFlowLayoutOptions.itemsPerLine = itemsPerLine;
        }
        if (cacheHelper != null) {
            cacheHelper.clear();
        }
        if (layoutHelper != null) {
            cacheHelper = new CacheHelper(itemsPerLine, layoutHelper.visibleAreaWidth());
        }
        return this;
    }

    public FlowLayoutManager removeItemPerLineLimit() {
        if (layoutHelper == null) {
            flowLayoutOptions.itemsPerLine = FlowLayoutOptions.ITEM_PER_LINE_NO_LIMIT;
        } else {
            newFlowLayoutOptions.itemsPerLine = FlowLayoutOptions.ITEM_PER_LINE_NO_LIMIT;
        }
        if (cacheHelper != null) {
            cacheHelper.clear();
        }
        if (layoutHelper != null) {
            cacheHelper = new CacheHelper(FlowLayoutOptions.ITEM_PER_LINE_NO_LIMIT, layoutHelper.visibleAreaWidth());
        }
        return this;
    }

    /*****************alignment related functions*****************/
    private boolean calcChildLayoutRect(View child, int x, int y, int lineHeight, Rect rect) {
        return calcChildLayoutRect(child, x, y, lineHeight, LayoutContext.fromLayoutOptions(flowLayoutOptions), rect);
    }
    private boolean calcChildLayoutRect(View child, int x, int y, int lineHeight, LayoutContext layoutContext, Rect rect) {

        boolean newLine;
        measureChildWithMargins(child, 0, 0);
        int childWidth = getDecoratedMeasuredWidth(child);
        int childHeight = getDecoratedMeasuredHeight(child);
        switch (layoutContext.layoutOptions.alignment) {
            case RIGHT:
                if (LayoutHelper.shouldStartNewline(x, childWidth, leftVisibleEdge(), rightVisibleEdge(), layoutContext)) {
                    newLine = true;
                    rect.left = rightVisibleEdge() - childWidth;
                    rect.top = y + lineHeight;
                    rect.right = rightVisibleEdge();
                    rect.bottom = rect.top + childHeight;
                } else {
                    newLine = false;
                    rect.left = x - childWidth;
                    rect.top = y;
                    rect.right = x;
                    rect.bottom = rect.top + childHeight;
                }
                break;
            case LEFT:
            default:
                if (LayoutHelper.shouldStartNewline(x, childWidth, leftVisibleEdge(), rightVisibleEdge(), layoutContext)) {
                    newLine = true;
                    rect.left = leftVisibleEdge();
                    rect.top = y + lineHeight;
                    rect.right = rect.left + childWidth;
                    rect.bottom = rect.top + childHeight;
                } else {
                    newLine = false;
                    rect.left = x;
                    rect.top = y;
                    rect.right = rect.left + childWidth;
                    rect.bottom = rect.top + childHeight;
                }
                break;
        }

        return newLine;
    }

    private Point startNewline(Rect rect) {
        return startNewline(rect, LayoutContext.fromLayoutOptions(flowLayoutOptions));

    }
    private Point startNewline(Rect rect, LayoutContext layoutContext) {
        switch (layoutContext.layoutOptions.alignment) {
            case RIGHT:
                return new Point(rightVisibleEdge() - rect.width(), rect.top);
            case LEFT:
            default:
                return new Point(leftVisibleEdge() + rect.width(), rect.top);
        }

    }

    private int advanceInSameLine(int x, Rect rect) {
        return advanceInSameLine(x, rect, LayoutContext.fromLayoutOptions(flowLayoutOptions));
    }
    private int advanceInSameLine(int x, Rect rect, LayoutContext layoutContext) {
        switch (layoutContext.layoutOptions.alignment) {
            case RIGHT:
                return x - rect.width();
            case LEFT:
            default:
                return x + rect.width();
        }
    }

    private Point layoutStartPoint() {
        return layoutHelper.layoutStartPoint(LayoutContext.fromLayoutOptions(flowLayoutOptions));
    }

    private boolean isStartOfLine(int index) {
        return isStartOfLine(index, LayoutContext.fromLayoutOptions(flowLayoutOptions));
    }
    private boolean isStartOfLine(int index, LayoutContext layoutContext) {
        if (index == 0) {
            return true;
        } else {
            switch (layoutContext.layoutOptions.alignment) {
                case RIGHT:
                    return getDecoratedRight(getChildAt(index)) >= rightVisibleEdge();
                case LEFT:
                default:
                    return getDecoratedLeft(getChildAt(index)) <= leftVisibleEdge();
            }
        }
    }

    private boolean isEndOfLine(int index) {
        return isEndOfLine(index, LayoutContext.fromLayoutOptions(flowLayoutOptions));
    }

    private boolean isEndOfLine(int index, LayoutContext layoutContext) {
        if (LayoutHelper.hasItemsPerLineLimit(layoutContext.layoutOptions) && layoutContext.currentLineItemCount == layoutContext.layoutOptions.itemsPerLine) {
            return true;
        }
        if (getChildCount() == 0 || index == getChildCount() - 1) {
            return true;
        }
        return isStartOfLine(index + 1, layoutContext);
    }

}