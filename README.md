# MicroBench
Enabling simpler microbenchmarks of Java8 and more traditional code.

Often, when developing performance sensitive code, it is convenient to benchmark that code in order to evaluate and improve its performance. UBench is a tool that can help.

##Example Use Case - Largest Prime Calculator

See the running code in [the test section - net.tuis.ubench.PrimeSieve](https://github.com/rolfl/MicroBench/blob/master/src/test/java/net/tuis/ubench/PrimeSieve.java)

Consider a function which returns the largest prime number less than a given input value. This is based off a [simple "Sieve of Eratosthenes"](http://en.wikipedia.org/wiki/Sieve_of_Eratosthenes):

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

What can UBench do for us that is useful?

##Simple Performance

First up, how fast is it? What happens to the performance as the Java JIT compiler optimizes the code. What can we expect when the system is 'cold'?

Simply run the code in a UBench task:

        UBench bench = new UBench("Simple Performance");
        bench.addIntTask("Prime less than 4000", () -> getMaxPrimeBefore(4000), p -> p == 3989);
        bench.report("Simple Performance", bench.press(10000));

This will produce (on my machine) the results:

     Simple Performance
    ==================

    Task Simple Performance -> Prime less than 4000: (Unit: MICROSECONDS)
      Count    :     10000      Average  :   17.4710
      Fastest  :   13.0260      Slowest  : 1107.2170
      95Pctile :   23.6840      99Pctile :   42.6310
      TimeBlock : 25.102 16.466 16.442 16.549 16.401 18.256 15.208 17.560 16.564 16.166
      Histogram :  9717   186    61    26     8     1     1

The above results can be interpreted in multiple ways. The TimeBlock and Histogram values are the ones which will be least obvious, though.

###TimeBlock

This is a break down of the run time in to 10 zones. In the above example, with 10,000 runs, the average of the first 1000 runs is 25.102 microseconds. The next 1000 runs averages at 16.466 microseconds, and so on. You can see that the performance is about constant after the first 1000 runs.

###Histogram

This value set counts the number of runs that fall within specific time buckets. Each bucket is twice as slow as the previous bucket. The first bucket is the reference, and is based on the fastest run in the results. In the exampe above, 9717 runs were between 1X and 2X as slow as the fastest run (13.0260 microseconds). 186 runs were between 2X and 4X as slow as the fastest run. 61 runs were between 4X and 8X slower than the fastest, and so on.

The number of buckets is directly related to the number of times slower the slowest run is compared to the fastest run. In the example above, the slowest run is 85 times slower than the fastest run, so it is in bucket 64-to-128 times slower. 

##Comparative Algorithms
 
How about comparing a revised implementation? One which does not do the ```Arrays.fill(...)``` operation, but instead negates all values? The Arrays.fill is an operation that can be avoided if the logic using the boolean values in the sieve is negated. Consider code like:

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

Note that the code is the same as the initial example, but the method name is different, it does not do the Arrays.fill, and it has a negated check of the boolean value, and it sets values to true, instead of setting them to false. How much will the removal of the fill improve the performance?

        UBench bench = new UBench("Comparative Performance");
        bench.addIntTask("Primes Filled", () -> getMaxPrimeBefore(4000), p -> p == 3989);
        bench.addIntTask("Primes Negated", () -> getMaxPrimeBeforeNeg(4000), p -> p == 3989);
        bench.report("Effects of Arrays.fill()", bench.press(10000));

This produces the output:

    Effects of Arrays.fill()
    ========================
    
    Task Comparative Performance -> Primes Filled: (Unit: MICROSECONDS)
      Count    :     10000      Average  :   15.9820
      Fastest  :   14.2100      Slowest  : 1458.1320
      95Pctile :   18.5530      99Pctile :   25.2630
      TimeBlock : 15.529 17.707 15.695 15.539 15.944 17.046 15.855 15.546 15.447 15.518
      Histogram :  9931    53     9     4     1     1     1
    
    Task Comparative Performance -> Primes Negated: (Unit: MICROSECONDS)
      Count    :     10000      Average  :   15.0480
      Fastest  :   13.4200      Slowest  : 1403.2650
      95Pctile :   16.9740      99Pctile :   24.4740
      TimeBlock : 14.417 15.140 14.637 14.483 14.823 15.335 16.488 14.954 15.041 15.168
      Histogram :  9938    45    11     4     1     0     1

As you can see, the negated process is consistently about 1 microsecond faster, or, 8% in this case.
 
##Scalability testing

How does the performance change with different values for the limit? Let's check it doubling input values. As we double the input, what happens to the performance?

        UBench bench = new UBench("Sieve Scalability");
        
        int[] limits = {250, 500, 1000, 2000, 4000, 8000};
        int[] primes = {241, 499, 997, 1999, 3989, 7993};
        
        for (int i = 0; i < limits.length; i++) {
            final int limit = limits[i];
            final int check = primes[i];
            bench.addIntTask("Primes " + limit, () -> getMaxPrimeBefore(limit), p -> p == check);
        }
        
        bench.report("Prime Scalability", bench.press(UMode.SEQUENTIAL, 10000));

We run the code and get the results:

```
Prime Scalability
=================

Task Sieve Scalability -> Primes 250: (Unit: MICROSECONDS)
  Count    :     10000      Average  :    0.8840
  Fastest  :    0.3940      Slowest  : 1605.3660
  95Pctile :    0.7900      99Pctile :    0.7900
  TimeBlock : 2.409 0.753 0.699 0.701 0.703 0.844 0.702 0.699 0.690 0.639
  Histogram :  2261  7712    14     5     3     2     1     1     0     0     0     1

Task Sieve Scalability -> Primes 500: (Unit: MICROSECONDS)
  Count    :    10000      Average  :   1.3500
  Fastest  :   1.1840      Slowest  :  70.6570
  95Pctile :   1.5790      99Pctile :   1.5790
  TimeBlock : 1.341 1.373 1.416 1.335 1.335 1.332 1.334 1.332 1.371 1.333
  Histogram :  9979    18     0     0     2     1

Task Sieve Scalability -> Primes 1000: (Unit: MICROSECONDS)
  Count    :    10000      Average  :   3.1920
  Fastest  :   2.3680      Slowest  :  61.5780
  95Pctile :   5.1310      99Pctile :   5.1320
  TimeBlock : 2.888 2.834 2.871 3.207 3.396 2.969 2.857 2.875 3.085 4.946
  Histogram :  8691  1302     3     3     1

Task Sieve Scalability -> Primes 2000: (Unit: MICROSECONDS)
  Count    :     10000      Average  :    6.2780
  Fastest  :    5.5260      Slowest  : 1601.4190
  95Pctile :    6.7100      99Pctile :    8.6840
  TimeBlock : 6.067 6.086 6.372 6.035 6.445 6.038 6.041 7.616 6.038 6.048
  Histogram :  9984    14     1     0     0     0     0     0     1

Task Sieve Scalability -> Primes 4000: (Unit: MICROSECONDS)
  Count    :     10000      Average  :   13.3520
  Fastest  :   11.8410      Slowest  : 1562.7350
  95Pctile :   14.6050      99Pctile :   18.5520
  TimeBlock : 13.395 13.266 13.143 13.347 13.207 13.130 14.799 13.314 13.124 12.798
  Histogram :  9979    17     3     0     0     0     0     1

Task Sieve Scalability -> Primes 8000: (Unit: MICROSECONDS)
  Count    :      10000      Average  :    33.0100
  Fastest  :    26.0520      Slowest  : 35412.0070
  95Pctile :    41.0520      99Pctile :    42.6300
  TimeBlock : 28.020 28.138 28.887 27.991 28.095 29.253 71.221 30.652 28.668 29.184
  Histogram :  9951    35    10     0     2     1     0     0     0     0     1

```
   
The results show a slightly more than linear progression of the various times, which is what would be expected for an algorithm that's reported to be ``O( n log( log( n ) ) )``
 
##Profiling

Perhaps you just want to profile the code, and get deeper insights. You can just set large values of iterations for the tests, or simply run the code for a given length of time:

    bench.press("For Profiling", 1, TimeUnit.DAYS);
    
then kill the job when your profiling completes. Your profiles will show some overhead from the UBench itself, but the small footprint in terms of code and stack, makes UBench a convenient platform to use.