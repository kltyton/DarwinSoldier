package com.kltyton.darwin_soldier.combat;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Bounded, pruned per-player stack pairing an identity (DamageSource instance)
 * with a captured pre-armor amount. Nested/reentrant hurt calls are handled by
 * pushing in order and consuming the most recent matching identity.
 */
final class BoundedPairStack<T> {
    private final int maxSize;
    private final ArrayDeque<Entry<T>> entries = new ArrayDeque<>();

    BoundedPairStack(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
    }

    void push(T identity, float amount, long captureTime) {
        entries.addLast(new Entry<>(identity, amount, captureTime));
        while (entries.size() > maxSize) {
            entries.removeFirst();
        }
    }

    float consume(T identity, float finalAmount, float minimumReduction) {
        Iterator<Entry<T>> iterator = entries.descendingIterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (entry.identity == identity) {
                iterator.remove();
                return DefenseReductionMath.cappedFinal(finalAmount, entry.amount, minimumReduction);
            }
        }
        return finalAmount;
    }

    void prune(long now, long maxAgeTicks) {
        long cutoff = now - maxAgeTicks;
        while (!entries.isEmpty() && entries.peekFirst().captureTime < cutoff) {
            entries.removeFirst();
        }
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    int size() {
        return entries.size();
    }

    private record Entry<T>(T identity, float amount, long captureTime) {
    }
}
