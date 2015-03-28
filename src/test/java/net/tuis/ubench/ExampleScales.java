package net.tuis.ubench;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;


@SuppressWarnings("javadoc")
public class ExampleScales {

    private static final int[] randomData(int size) {
        //System.out.println("Randomizing " + size);
        Random rand = new Random(size);
        return IntStream.generate(rand::nextInt).limit(size).toArray();
    }
    
    public static void main(String[] args) {
        UScale.scale(div -> div / 3, scale -> scale).report();
        UScale.scale(Arrays::sort, scale -> randomData(scale), false).report();
    }

}
