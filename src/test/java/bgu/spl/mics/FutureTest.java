package bgu.spl.mics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Future class.
 */
public class FutureTest {

    private Future<String> future;

    @BeforeEach
    public void setUp() {
        future = new Future<>();
    }

    @Test
    public void basicTest() {
        assertTrue(true);
    }

    @Test
    public void testResolveAndGet() {
        // Ensure the future is initially unresolved
        assertFalse(future.isDone(), "Future should not be resolved initially.");

        // Resolve the future
        String expectedValue = "Hello, World!";
        future.resolve(expectedValue);

        // Ensure the future is resolved
        assertTrue(future.isDone(), "Future should be resolved after calling resolve().");

        // Check that the resolved value matches
        assertEquals(expectedValue, future.get(), "Future did not return the correct resolved value.");
    }

    @Test
    public void testGetWithTimeoutWhenResolved() {
        // Resolve the future
        String expectedValue = "Resolved Value";
        future.resolve(expectedValue);

        // Check get with timeout when already resolved
        String actualValue = future.get(1, TimeUnit.SECONDS);
        assertEquals(expectedValue, actualValue, "Future did not return the correct value with timeout when resolved.");
    }

    @Test
    public void testGetWithTimeoutWhenUnresolved() {
        // Check get with timeout when unresolved
        String result = future.get(1, TimeUnit.SECONDS);
    assertNull(result, "Future should return null when unresolved and timeout expires.");
}

}

