package net.tuis.ubench;

import static org.junit.Assert.*;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class TestUBench {
    
    @Test
    public void testBenchName() {
        UBench bench = new UBench("foo");
        assertEquals("foo", bench.getSuiteName());
    }
    
    @Test(expected=UBenchRuntimeException.class)
    public void testIntTaskException() {
        UBench bench = new UBench("test");
        bench.addIntTask("test", () -> 1, i -> i == 2);
        bench.press(1);
    }

    @Test(expected=UBenchRuntimeException.class)
    public void testDoubleTaskException() {
        UBench bench = new UBench("test");
        bench.addDoubleTask("test", () -> 1.0, i -> i == 2.0);
        bench.press(1);
    }

    @Test(expected=UBenchRuntimeException.class)
    public void testLongTaskException() {
        UBench bench = new UBench("test");
        bench.addLongTask("test", () -> 1L, i -> i == 2L);
        bench.press(1);
    }

}
