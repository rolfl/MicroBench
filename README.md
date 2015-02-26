# MicroBench
Enabling simpler microbenchmarks of Java8 code

Consider an (inefficient) function similar to:

```java

    private static final ToIntFunction<String> distinctCount = 
            (line) -> (int)IntStream.range(0, line.length()).map(i -> (int)line.charAt(i)).distinct().count();
```

That function finds all the distinct characters in the input string, and reports the number of distinct chars.

Now consider a function with the same purpose, but different algorithm:

```java

    private static final int countDistinctChars(String line) {
        if (line.length() <= 1) {
            return line.length();
        }
        char[] chars = line.toCharArray();
        Arrays.sort(chars);
        int count = 1;
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] != chars[i - 1]) {
                count++;
            }
        }
        return count;
        
    }
    
```

Now we want to compare their performance for counting the number of characters in the string `Hello World!`. This is the purpose of this library:

```java

        final String hello = "Hello World!";

        UBench bench = new UBench("distinct chars");
        
        bench.addIntTask("Functional hello", () -> distinctCount.applyAsInt(hello), got -> got == 9);

        bench.addIntTask("Traditional hello", () -> countDistinctChars(hello), got -> got == 9);

        bench.report(bench.press(100000));
        

```

The result of this would be the statistics on the two functions:

```

Task Functional hello:
  Iterations  :       100000
  Fastest     :      0.00039ms
  Average     :      0.00093ms
  95Pctile    :      0.00158ms
  Slowest     :      5.32727ms
  TimeBlock   : 0.00158ms 0.00176ms 0.00079ms 0.00079ms 0.00066ms 0.00060ms 0.00076ms 0.00051ms 0.00119ms 0.00061ms
  FactorHisto : 27209 63026  9433   264    24    16    20     4     0     0     2     0     1     1


Task Traditional hello:
  Iterations  :       100000
  Fastest     :      0.00000ms
  Average     :      0.00009ms
  95Pctile    :      0.00040ms
  Slowest     :      0.03118ms
  TimeBlock   : 0.00018ms 0.00018ms 0.00007ms 0.00008ms 0.00006ms 0.00007ms 0.00008ms 0.00009ms 0.00006ms 0.00006ms
  FactorHisto : 77298     0     0     0     0     0     0     0 22659    13    18     6     4     0     2

```

In this way, complicated benchmarks can be built up using a mix of traditional and lambda-based expressions in Java, and output statistics that allow you to see the effects of warmups, run time distributions, etc. The actual statistics gathered are based off the nano-second runtimes for each function, and you can set the system to run a certain number of times, for a certain amount of time, or until the performance 'plateaus' at a given level of stability (whichever comes first).
