package net.tuis.ubench;

import net.tuis.ubench.scale.MathEquation;
import net.tuis.ubench.scale.Models;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Simon Forsberg
 */
@SuppressWarnings("javadoc")
public class AnalyzeTest {

    @Test
    public void nSquared() {
        MathEquation eq = ScaleDetect.detect(new double[]{42, 107, 73, 120}, new double[]{511, 312, 400, 242}, Models.N_SQUARED);
        assertArrayEquals(new double[]{ -0.0021060, -2.947499, 635.60559 }, eq.getParameters(), 0.001);
    }

    @Test
    public void linear() {
        MathEquation eq = ScaleDetect.detect(new double[]{1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0, 2048.0, 4096.0, 8192.0, 16384.0, 32768.0, 65536.0, 131072.0, 262144.0, 524288.0},
                new double[]{905.0, 901.0, 939.0, 927.0, 920.0, 898.0, 884.0, 861.0, 852.0, 864.0, 869.0, 867.0, 866.0, 867.0, 857.0, 857.0, 854.0, 855.0, 872.0, 865.0},
                Models.LINEAR);
        assertArrayEquals(new double[]{ -0.000048924907, 881.5650 }, eq.getParameters(), 0.001);
    }

    @Test
    public void nLogN() {
        MathEquation eq = ScaleDetect.detect(new double[]{1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0, 2048.0, 4096.0, 8192.0, 16384.0, 32768.0, 65536.0, 131072.0, 262144.0, 524288.0},
                new double[]{857.0, 860.0, 898.0, 975.0, 993.0, 1601.0, 1530.0, 2947.0, 6106.0, 16111.0, 35937.0, 80497.0, 184819.0, 390424.0, 847658.0, 1820366.0, 4095873.0, 8463674.0, 17483933, 39126742},
                Models.N_LOG_N);
        assertArrayEquals(new double[]{ 12.9000747 }, eq.getParameters(), 0.001);
    }

    @Test
    public void exponentialNaN() {
        double[] expX = { 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0, 2048.0, 4096.0, 8192.0, 16384.0, 14336.0, 12288.0, 10240.0, 6144.0 };
        double[] expY = { 488.0, 488.0, 488.0, 488.0, 977.0, 3422.0, 11733.0, 44000.0, 170134.0, 672711.0, 2614089.0, 1.0346356E7, 4.1465605E7, 1.68587977E8, 6.8453302E8, 5.13354909E8, 3.99514184E8, 2.639047E8, 9.4308146E7 };
        MathEquation eq = ScaleDetect.detect(expX, expY, Models.EXPONENTIAL);
        assertArrayEquals(new double[]{ Double.NaN }, eq.getParameters(), 0.001);
    }

    @Test
    public void exponential() {
        double[] expX = new double[]{1.0, 2.0, 4.0, 8.0, 16.0};
        double[] expY = new double[]{2.3, 5.6, 32, 955, 913480};
        MathEquation eq = ScaleDetect.detect(expX, expY, Models.EXPONENTIAL);
        assertArrayEquals(new double[]{ 2.3579999 }, eq.getParameters(), 0.001);
    }

}
