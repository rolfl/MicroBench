package net.tuis.ubench;

import java.util.Arrays;

@SuppressWarnings("javadoc")
public class PrimeSieve {
    
    /**
     * Basic "Sieve of Eratosthenes" implementation, no optimizations 
     * @param limit only primes less than this limit will be calculated.
     * @return the largest prime less than the supplied limit.
     */
    public static final int getMaxPrimeBefore(int limit) {
        boolean[] sieve = new boolean[limit];
        Arrays.fill(sieve, true);
        sieve[0] = false;
        sieve[1] = false;
        int largest = 0;
        for (int p = 2; p < limit; p++) {
            if (sieve[p]) {
                largest = p;
                for (int np = p * 2; np < limit; np += p) {
                    sieve[np] = false;
                }
            }
        }
        return largest;
    }

    public static final int getMaxPrimeBeforeNeg(int limit) {
        boolean[] sieve = new boolean[limit];
        // Arrays.fill(sieve, true);
        sieve[0] = true;
        sieve[1] = true;
        int largest = 0;
        for (int p = 2; p < limit; p++) {
            if (!sieve[p]) {
                largest = p;
                for (int np = p * 2; np < limit; np += p) {
                    sieve[np] = true;
                }
            }
        }
        return largest;
    }
    
    private static void benchSimple() {
        UBench bench = new UBench("Simple Performance");
        bench.addIntTask("Prime less than 4000", () -> getMaxPrimeBefore(4000), p -> p == 3989);
        bench.press(10000).report("Simple Performance");
    }

    private static void benchSimpleNeg() {
        UBench bench = new UBench("Negate Performance");
        bench.addIntTask("Prime less than 4000", () -> getMaxPrimeBeforeNeg(4000), p -> p == 3989);
        bench.press(10000).report("Negated Performance");
    }

    private static void benchCompare() {
        UBench bench = new UBench("Comparative Performance");
        
        bench.addIntTask("Primes Filled", () -> getMaxPrimeBefore(4000), p -> p == 3989);
        bench.addIntTask("Primes Negated", () -> getMaxPrimeBeforeNeg(4000), p -> p == 3989);
        
        bench.press(10000).report("Effects of Arrays.fill()");
    }

    private static void benchScale() {
        UBench bench = new UBench("Sieve Scalability");
        
        int[] limits = {250, 500, 1000, 2000, 4000, 8000};
        int[] primes = {241, 499, 997, 1999, 3989, 7993};
        
        for (int i = 0; i < limits.length; i++) {
            final int limit = limits[i];
            final int check = primes[i];
            bench.addIntTask("Primes " + limit, () -> getMaxPrimeBefore(limit), p -> p == check);
        }
        
        bench.press(UMode.SEQUENTIAL, 10000).report("Prime Scalability");
    }

    public static void main(String[] args) {
        
        benchSimple();
        benchSimpleNeg(); // to warm up equally.
        benchCompare();
        benchScale();

    }
}
