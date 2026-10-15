package com.kltyton.darwin_soldier.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedPairStackTest {
    private static final Object SOURCE_A = new Object();
    private static final Object SOURCE_B = new Object();

    @Test
    void consumeMatchesSourceIdentityAndCapsFinalDamage() {
        BoundedPairStack<Object> stack = new BoundedPairStack<>(8);
        stack.push(SOURCE_A, 100.0F, 1L);

        assertEquals(90.0F, stack.consume(SOURCE_A, 95.0F, 10.0F), 0.0001F);
        assertTrue(stack.isEmpty());
    }

    @Test
    void unrelatedSourceDoesNotConsumeCapture() {
        BoundedPairStack<Object> stack = new BoundedPairStack<>(8);
        stack.push(SOURCE_A, 100.0F, 1L);

        assertEquals(95.0F, stack.consume(SOURCE_B, 95.0F, 10.0F), 0.0001F);
        assertEquals(1, stack.size());
        assertEquals(90.0F, stack.consume(SOURCE_A, 95.0F, 10.0F), 0.0001F);
        assertTrue(stack.isEmpty());
    }

    @Test
    void nestedPairsAreConsumedByMostRecentMatchingSource() {
        BoundedPairStack<Object> stack = new BoundedPairStack<>(8);
        stack.push(SOURCE_A, 100.0F, 1L);
        stack.push(SOURCE_B, 50.0F, 1L);

        assertEquals(40.0F, stack.consume(SOURCE_B, 40.0F, 10.0F), 0.0001F);
        assertEquals(90.0F, stack.consume(SOURCE_A, 95.0F, 10.0F), 0.0001F);
        assertTrue(stack.isEmpty());
    }

    @Test
    void sameIdentitySequentialCapturesConsumeNewestFirst() {
        BoundedPairStack<Object> stack = new BoundedPairStack<>(8);
        stack.push(SOURCE_A, 100.0F, 1L);
        stack.push(SOURCE_A, 50.0F, 2L);

        assertEquals(40.0F, stack.consume(SOURCE_A, 40.0F, 10.0F), 0.0001F);
        assertEquals(1, stack.size());
        assertEquals(90.0F, stack.consume(SOURCE_A, 95.0F, 10.0F), 0.0001F);
        assertTrue(stack.isEmpty());
    }

    @Test
    void skippedOrOldEntriesCannotDefeatNewestSameIdentityMatch() {
        BoundedPairStack<Object> stack = new BoundedPairStack<>(8);
        stack.push(SOURCE_A, 100.0F, 1L);
        stack.push(SOURCE_A, 50.0F, 2L);
        stack.push(SOURCE_B, 30.0F, 3L);

        assertEquals(40.0F, stack.consume(SOURCE_A, 95.0F, 10.0F), 0.0001F);
        assertEquals(2, stack.size());
        assertEquals(90.0F, stack.consume(SOURCE_A, 95.0F, 10.0F), 0.0001F);
        assertEquals(1, stack.size());
        assertEquals(20.0F, stack.consume(SOURCE_B, 95.0F, 10.0F), 0.0001F);
        assertTrue(stack.isEmpty());
    }

    @Test
    void sizeIsBoundedAndOldestEntriesAreEvicted() {
        BoundedPairStack<Object> stack = new BoundedPairStack<>(2);
        stack.push(SOURCE_A, 1.0F, 1L);
        stack.push(SOURCE_B, 1.0F, 2L);
        Object sourceC = new Object();
        stack.push(sourceC, 1.0F, 3L);

        assertEquals(2, stack.size());
        assertEquals(1.0F, stack.consume(SOURCE_A, 1.0F, 1.0F), 0.0001F);
        assertEquals(2, stack.size());
        assertFalse(stack.isEmpty());
    }

    @Test
    void staleCapturesArePruned() {
        BoundedPairStack<Object> stack = new BoundedPairStack<>(8);
        stack.push(SOURCE_A, 100.0F, 10L);

        stack.prune(200L, 100L);

        assertTrue(stack.isEmpty());
    }
}
