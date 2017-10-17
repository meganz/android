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
package mega.privacy.android.app.components.flowlayoutmanager.cache;

import android.graphics.Point;
import android.util.SparseArray;

/**
 * A Helper class that will save Line information (items count, total width etc.) and will be used
 * for layout, that will avoid low efficiency layout
 */
public class CacheHelper {
    public static final int NOT_FOUND = -1;
    /**
     * Item per line limit, set to 0 if no limit
     */
    final int itemPerLine;
    /**
     * The area width for content layout, must be greater than zero
     */
    int contentAreaWidth;
    SparseArray<Point> sizeMap;
    SparseArray<Line> lineMap;
    boolean batchSetting = false;

    public CacheHelper(int itemsPerLine, int contentAreaWidth) {
        this.itemPerLine = itemsPerLine;
        this.contentAreaWidth = contentAreaWidth;
        sizeMap = new SparseArray<>();
        lineMap = new SparseArray<>();
    }

    /**
     * Add measured items into cache
     */
    public void add(int startIndex, Point... sizes) {
        if (!valid()) {
            return;
        }
        invalidateLineMapAfter(startIndex);
        makeSpace(startIndex, sizes.length);
        int index = startIndex;
        for (Point size : sizes) {
            sizeMap.put(index ++, size);
        }
        refreshLineMap();
    }

    /**
     * Make space for {@param count} items
     */
    public void add(int startIndex, int count) {
        if (!valid()) {
            return;
        }
        invalidateLineMapAfter(startIndex);
        makeSpace(startIndex, count);
        refreshLineMap();
    }

    /**
     * Sizes has been changed and new sizes not available yet, just remove them from cache
     * The line map will also be invalidate after the invalidated items
     * @param index
     * @param count
     */
    public void invalidSizes(int index, int count) {
        if (!valid()) {
            return;
        }
        invalidateLineMapAfter(index);
        int actualCount = actualCount(index, count);
        for (int i = 0; i < actualCount; i ++) {
            sizeMap.remove(index + i);
        }
        refreshLineMap();
    }

    public void remove(int index, int count) {
        if (!valid()) {
            return;
        }
        invalidateLineMapAfter(index);
        int actualCount = actualCount(index, count);
        for (int i = 0; i < actualCount; i ++) {
            sizeMap.remove(index + i);
        }

        // move everything behind to fill the hole.
        for (int i = index + actualCount; i < sizeMap.size() + actualCount; i ++) {
            Point tmp = sizeMap.get(i);
            sizeMap.remove(i);
            sizeMap.put(i - actualCount, tmp);
        }
        refreshLineMap();
    }

    public void setItem(int index, Point newSize) {
        if (!valid()) {
            return;
        }
        if (sizeMap.get(index, null) != null) {
            Point cachedPoint = sizeMap.get(index);
            if (!cachedPoint.equals(newSize)) {
                invalidateLineMapAfter(index);
                sizeMap.put(index, newSize);
                refreshLineMap();
            }
        } else {
            invalidateLineMapAfter(index);
            sizeMap.put(index, newSize);
            refreshLineMap();
        }
    }

    /**
     * Move items from one place to another. no check on parameter as invoker will make sure it is correct
     */
    public void move(int from, int to, int count) {
        if (!valid()) {
            return;
        }
        invalidateLineMapAfter(Math.min(from, to));
        Point[] itemsToMove = new Point[count];
        for (int i = from; i < from + count; i ++) {
            itemsToMove[i - from] = sizeMap.get(i);
        }
        boolean movingForward = from - to > 0;
        int itemsToShift = Math.abs(from - to);

        if (!movingForward) {
            itemsToShift -= count;
        }
        int shiftIndex = movingForward ? from - 1 : from + count;
        int shiftIndexStep = movingForward ? -1 : 1;

        int shifted = 0;
        while (shifted < itemsToShift) {
            sizeMap.put(shiftIndex - (shiftIndexStep) * count, sizeMap.get(shiftIndex));
            shiftIndex += shiftIndexStep;
            shifted ++;
        }

        int setIndex = to;
        if (!movingForward) {
            setIndex = from + itemsToShift;
        }
        for (Point item : itemsToMove) {
            sizeMap.put(setIndex++, item);
        }
        refreshLineMap();
    }

    public int[] getLineMap() {
        if (!valid()) {
            return new int[0];
        }
        int[] lineCounts = new int[this.lineMap.size()];
        for (int i = 0; i < this.lineMap.size(); i ++) {
            lineCounts[i] = this.lineMap.get(i).itemCount;
        }
        return lineCounts;
    }

    public int itemLineIndex(int itemIndex) {
        if (!valid()) {
            return NOT_FOUND;
        }
        int itemCount = 0;
        for (int i = 0; i < lineMap.size(); i ++) {
            itemCount += lineMap.get(i).itemCount;
            if (itemCount >= itemIndex + 1) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    public Line containingLine(int itemIndex) {
        if (!valid()) {
            return null;
        }
        return getLine(itemLineIndex(itemIndex));
    }

    public int firstItemIndex(int lineIndex) {
        if (!valid()) {
            return NOT_FOUND;
        }
        int itemCount = 0;
        for (int i = 0; i < lineIndex; i ++) {
            itemCount += lineMap.get(i).itemCount;
        }
        return itemCount;
    }

    public Line getLine(int lineIndex) {
        if (!valid()) {
            return null;
        }
        return lineMap.get(lineIndex, null);
    }

    public boolean hasPreviousLineCached(int itemIndex) {
        if (!valid()) {
            return false;
        }
        int lineIndex = itemLineIndex(itemIndex);
        if (lineIndex == NOT_FOUND) {
            return false;
        }

        if (lineIndex > 0) {
            return true;
        }
        return false;
    }

    public boolean hasNextLineCached(int itemIndex) {
        if (!valid()) {
            return false;
        }
        int lineIndex = itemLineIndex(itemIndex);
        if (lineIndex == NOT_FOUND) {
            return false;
        }
        return !lineMap.get(lineIndex + 1, Line.EMPTY_LINE).equals(Line.EMPTY_LINE);
    }

    public void clear() {
        sizeMap.clear();
        lineMap.clear();
    }

    public void contentAreaWidth(int width) {
        contentAreaWidth = width;
        lineMap.clear();
        refreshLineMap();
    }

    public int contentAreaWidth() {
        return contentAreaWidth;
    }

    public boolean valid() {
        return contentAreaWidth > 0;
    }

    public void startBatchSetting() {
        batchSetting = true;
    }

    public void endBatchSetting() {
        batchSetting = false;
        lineMap.clear();
        refreshLineMap();
    }

    //===================== Helper methods ========================

    /**
     * Move item after startIndex to make {count} space(s)
     */
    private void makeSpace(int startIndex, int count) {
        for (int i = sizeMap.size() - 1; i >= startIndex; i --) {
            sizeMap.put(i + count, sizeMap.get(i));
        }
        for (int i = startIndex; i < startIndex + count; i ++) {
            sizeMap.remove(i);
        }
    }

    /**
     * Rebuild line map. and should stop if there is a hole (like item changed or item inserted but not measured)
     */
    private void refreshLineMap() {
        if (!valid() || batchSetting) {
            return;
        }
        int index = refreshLineMapStartIndex();
        Point cachedSize = sizeMap.get(index, null);
        int lineIndex = lineMap.size();
        int lineItemCount = 0;
        Line currentLine = containingLine(index);

        if (currentLine == null) {
            currentLine = new Line();
        } else {
            lineIndex = itemLineIndex(index);
        }

        int lineWidth = currentLine.totalWidth;
        while (cachedSize != null) {
            lineWidth += cachedSize.x;
            lineItemCount ++;
            if (lineWidth <= contentAreaWidth) {
                if (itemPerLine > 0) { // have item per line limit
                    if (lineItemCount > itemPerLine) { // exceed item per line limit
                        lineMap.put(lineIndex, currentLine);

                        // put this item to next line
                        currentLine = new Line();
                        addToLine(currentLine, cachedSize, index);
                        lineIndex ++;
                        lineWidth = cachedSize.x;
                        lineItemCount = 1;
                    } else {
                        addToLine(currentLine, cachedSize, index);
                    }
                } else {
                    addToLine(currentLine, cachedSize, index);
                }
            } else { // too wide to add this item, put line item count to index and put this one to new line
                lineMap.put(lineIndex, currentLine);
                currentLine = new Line();
                addToLine(currentLine, cachedSize, index);
                lineIndex ++;
                lineWidth = cachedSize.x;
                lineItemCount = 1;

            }
            index ++;
            cachedSize = sizeMap.get(index, null);
        }

        if (currentLine.itemCount > 0) {
            lineMap.append(lineIndex, currentLine);
        }
    }

    /**
     * Add view info to line
     */
    private void addToLine(Line line, Point item, int index) {
        line.itemCount ++;
        line.totalWidth += item.x;
        line.maxHeight = item.y > line.maxHeight ? item.y : line.maxHeight;
        if (item.y == line.maxHeight) {
            line.maxHeightIndex = index;
        }
    }

    /**
     * return actual count from index to expected count or end of sizeMap
     */
    private int actualCount(int index, int count) {
        return index + count > sizeMap.size() ? sizeMap.size() - index : count;
    }

    /**
     * Invalidate line map that contains item and all lines after
     * @param itemIndex
     */
    private void invalidateLineMapAfter(int itemIndex) {
        if (batchSetting) {
            return;
        }
        int itemLineIndex = itemLineIndex(itemIndex);
        Line line = lineMap.get(itemLineIndex, null);
        if (line == null && lineMap.size() > 0) {
            lineMap.remove(lineMap.size() - 1);
        }
        while (line != null) {
            lineMap.remove(itemLineIndex);
            itemLineIndex ++;
            line = lineMap.get(itemLineIndex, null);
        }
    }

    private int refreshLineMapStartIndex() {
        int itemCount = 0;
        for (int i = 0; i < lineMap.size(); i ++) {
            itemCount += lineMap.get(i).itemCount;
        }
        if (itemCount >= sizeMap.size()) {
            return NOT_FOUND;
        }
        return itemCount;
    }




}