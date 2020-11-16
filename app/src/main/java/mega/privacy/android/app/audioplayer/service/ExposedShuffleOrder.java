package mega.privacy.android.app.audioplayer.service;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.source.ShuffleOrder;

import java.util.Arrays;
import java.util.Random;

/**
 * This class is barely a copy of {@link ShuffleOrder.DefaultShuffleOrder}, only expose
 * the shuffled index.
 */
public class ExposedShuffleOrder implements ShuffleOrder {
    private final Random random;
    private final int[] shuffled;
    private final int[] indexInShuffled;
    private final ShuffleChangeListener listener;

    /**
     * Creates an instance with a specified length.
     *
     * @param length The length of the shuffle order.
     */
    public ExposedShuffleOrder(int length, ShuffleChangeListener listener) {
        this(length, new Random(), listener);
    }

    /**
     * Creates an instance with a specified length and the specified random seed. Shuffle orders of
     * the same length initialized with the same random seed are guaranteed to be equal.
     *
     * @param length The length of the shuffle order.
     * @param randomSeed A random seed.
     */
    public ExposedShuffleOrder(int length, long randomSeed, ShuffleChangeListener listener) {
        this(length, new Random(randomSeed), listener);
    }

    /**
     * Creates an instance with a specified shuffle order and the specified random seed. The random
     * seed is used for {@link #cloneAndInsert(int, int)} invocations.
     *
     * @param shuffledIndices The shuffled indices to use as order.
     * @param randomSeed A random seed.
     */
    public ExposedShuffleOrder(int[] shuffledIndices, long randomSeed, ShuffleChangeListener listener) {
        this(Arrays.copyOf(shuffledIndices, shuffledIndices.length), new Random(randomSeed), listener);
    }

    private ExposedShuffleOrder(int length, Random random, ShuffleChangeListener listener) {
        this(createShuffledList(length, random), random, listener);
    }

    private ExposedShuffleOrder(int[] shuffled, Random random, ShuffleChangeListener listener) {
        this.shuffled = shuffled;
        this.random = random;
        this.indexInShuffled = new int[shuffled.length];
        for (int i = 0; i < shuffled.length; i++) {
            indexInShuffled[shuffled[i]] = i;
        }
        this.listener = listener;

        listener.onShuffleChanged(this);
    }

    @Override
    public int getLength() {
        return shuffled.length;
    }

    @Override
    public int getNextIndex(int index) {
        int shuffledIndex = indexInShuffled[index];
        return ++shuffledIndex < shuffled.length ? shuffled[shuffledIndex] : C.INDEX_UNSET;
    }

    @Override
    public int getPreviousIndex(int index) {
        int shuffledIndex = indexInShuffled[index];
        return --shuffledIndex >= 0 ? shuffled[shuffledIndex] : C.INDEX_UNSET;
    }

    @Override
    public int getLastIndex() {
        return shuffled.length > 0 ? shuffled[shuffled.length - 1] : C.INDEX_UNSET;
    }

    @Override
    public int getFirstIndex() {
        return shuffled.length > 0 ? shuffled[0] : C.INDEX_UNSET;
    }

    @Override
    public ShuffleOrder cloneAndInsert(int insertionIndex, int insertionCount) {
        int[] insertionPoints = new int[insertionCount];
        int[] insertionValues = new int[insertionCount];
        for (int i = 0; i < insertionCount; i++) {
            insertionPoints[i] = random.nextInt(shuffled.length + 1);
            int swapIndex = random.nextInt(i + 1);
            insertionValues[i] = insertionValues[swapIndex];
            insertionValues[swapIndex] = i + insertionIndex;
        }
        Arrays.sort(insertionPoints);
        int[] newShuffled = new int[shuffled.length + insertionCount];
        int indexInOldShuffled = 0;
        int indexInInsertionList = 0;
        for (int i = 0; i < shuffled.length + insertionCount; i++) {
            if (indexInInsertionList < insertionCount
                    && indexInOldShuffled == insertionPoints[indexInInsertionList]) {
                newShuffled[i] = insertionValues[indexInInsertionList++];
            } else {
                newShuffled[i] = shuffled[indexInOldShuffled++];
                if (newShuffled[i] >= insertionIndex) {
                    newShuffled[i] += insertionCount;
                }
            }
        }
        return new ExposedShuffleOrder(newShuffled, new Random(random.nextLong()), listener);
    }

    @Override
    public ShuffleOrder cloneAndRemove(int indexFrom, int indexToExclusive) {
        int numberOfElementsToRemove = indexToExclusive - indexFrom;
        int[] newShuffled = new int[shuffled.length - numberOfElementsToRemove];
        int foundElementsCount = 0;
        for (int i = 0; i < shuffled.length; i++) {
            if (shuffled[i] >= indexFrom && shuffled[i] < indexToExclusive) {
                foundElementsCount++;
            } else {
                newShuffled[i - foundElementsCount] =
                        shuffled[i] >= indexFrom ? shuffled[i] - numberOfElementsToRemove : shuffled[i];
            }
        }
        return new ExposedShuffleOrder(newShuffled, new Random(random.nextLong()), listener);
    }

    @Override
    public ShuffleOrder cloneAndClear() {
        return new ExposedShuffleOrder(/* length= */ 0, new Random(random.nextLong()), listener);
    }

    private static int[] createShuffledList(int length, Random random) {
        int[] shuffled = new int[length];
        for (int i = 0; i < length; i++) {
            int swapIndex = random.nextInt(i + 1);
            shuffled[i] = shuffled[swapIndex];
            shuffled[swapIndex] = i;
        }
        return shuffled;
    }

    public interface ShuffleChangeListener {
        void onShuffleChanged(ShuffleOrder newShuffle);
    }
}
