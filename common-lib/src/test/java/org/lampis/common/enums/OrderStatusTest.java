package org.lampis.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void testGetNextStatus() {
        assertEquals(OrderStatus.PROCESSING, OrderStatus.UNPROCESSED.getNextStatus());
        assertEquals(OrderStatus.PROCESSED, OrderStatus.PROCESSING.getNextStatus());
        assertEquals(OrderStatus.SHIPPED, OrderStatus.PROCESSED.getNextStatus());
        assertEquals(OrderStatus.SHIPPED, OrderStatus.SHIPPED.getNextStatus()); // Terminal state
        assertEquals(OrderStatus.CANCELLED, OrderStatus.CANCELLED.getNextStatus()); // Terminal state
    }

    @Test
    void testIsTerminal() {
        assertFalse(OrderStatus.UNPROCESSED.isTerminal());
        assertFalse(OrderStatus.PROCESSING.isTerminal());
        assertFalse(OrderStatus.PROCESSED.isTerminal());
        assertTrue(OrderStatus.SHIPPED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.isTerminal());
    }

    @Test
    void testCanTransitionTo_ValidTransitions() {
        // Normal progression
        assertTrue(OrderStatus.UNPROCESSED.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.PROCESSED));
        assertTrue(OrderStatus.PROCESSED.canTransitionTo(OrderStatus.SHIPPED));

        // Same status
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.PROCESSING));

        // Can cancel from non-terminal states
        assertTrue(OrderStatus.UNPROCESSED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PROCESSED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void testCanTransitionTo_InvalidTransitions() {
        // Cannot transition from terminal states
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PROCESSING));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PROCESSING));

        // Cannot skip states
        assertFalse(OrderStatus.UNPROCESSED.canTransitionTo(OrderStatus.PROCESSED));
        assertFalse(OrderStatus.UNPROCESSED.canTransitionTo(OrderStatus.SHIPPED));

        // Cannot go backwards
        assertFalse(OrderStatus.PROCESSED.canTransitionTo(OrderStatus.PROCESSING));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PROCESSED));

        // Cannot cancel if already shipped
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }
}
