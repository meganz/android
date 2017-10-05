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

public class Line {
    public int itemCount;
    public int totalWidth;
    public int maxHeight;
    public int maxHeightIndex;

    public static final Line EMPTY_LINE = new Line();
    public Line() {
        itemCount = 0;
        totalWidth = 0;
        maxHeight = 0;
        maxHeightIndex = -1;

    }

    public Line clone() {
        Line clone = new Line();
        clone.itemCount = itemCount;
        clone.totalWidth = totalWidth;
        clone.maxHeight = maxHeight;
        clone.maxHeightIndex = maxHeightIndex;
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Line line = (Line) o;

        if (itemCount != line.itemCount) return false;
        if (totalWidth != line.totalWidth) return false;
        if (maxHeight != line.maxHeight) return false;
        return maxHeightIndex == line.maxHeightIndex;

    }

    @Override
    public int hashCode() {
        int result = itemCount;
        result = 31 * result + totalWidth;
        result = 31 * result + maxHeight;
        result = 31 * result + maxHeightIndex;
        return result;
    }

    @Override
    public String toString() {
        return "Line{" +
                "itemCount=" + itemCount +
                ", totalWidth=" + totalWidth +
                ", maxHeight=" + maxHeight +
                ", maxHeightIndex=" + maxHeightIndex +
                '}';
    }
}