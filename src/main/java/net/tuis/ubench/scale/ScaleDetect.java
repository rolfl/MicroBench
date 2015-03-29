package net.tuis.ubench.scale;

import net.tuis.ubench.UScale;
import org.apache.commons.math3.linear.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

/**
 * Created by Simon on 3/28/2015.
 */
public class ScaleDetect {

    public static double norm(RealVector data) {
        double norm = data.getNorm();
        return norm;
    }

    public static double[] newtonSolve(Function<double[], double[]> function, double[] initial, double tolerance) {
        RealVector dx = new ArrayRealVector(initial.length);
        dx.set(tolerance + 1);
        int iterations = 0;
        int d = initial.length;
        double h = 1e-4;
        double[] values = Arrays.copyOf(initial, initial.length);

        while (norm(dx) > tolerance) {
            double[] fx = function.apply(values);
            Array2DRowRealMatrix df = new Array2DRowRealMatrix(fx.length, d);
            ArrayRealVector fxVector = new ArrayRealVector(fx);
            for (int i = 0; i < d; i++) {
                double originalValue = values[i];
                values[i] += h;
                double[] fxi = feval(function, values);
                values[i] = originalValue;
                ArrayRealVector fxiVector = new ArrayRealVector(fxi);
                RealVector result = fxiVector.subtract(fxVector);
                result = result.mapDivide(h);
                df.setColumn(i, result.toArray());
            }
            dx = new RRQRDecomposition(df).getSolver().solve(fxVector.mapMultiply(-1));
            // df has size = initial, and fx has size equal to whatever that function produces.
            for (int i = 0; i < values.length; i++) {
                values[i] += dx.getEntry(i);
            }
            iterations++;
        }
        System.out.println("solved in " + iterations + " iterations");



        return values;
    }

    private static double[] feval(Function<double[], double[]> function, double[] values) {
        return function.apply(values);
    }


    public static MathEquation detect(UScale scale) {
        List<UScale.ScaleResult> stats = scale.getStats();

        double[] x = new double[stats.size()];
        double[] y = new double[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            UScale.ScaleResult stat = stats.get(i);
            x[i] = stat.getScale();
            y[i] = stat.getStats().getAverageRawNanos();
        }

        MathModel[] models = new MathModel[]{ Models.LINEAR, Models.LOG_N, Models.N_LOG_N, Models.N_SQUARED };
        Optional<MathEquation> bestModel = Arrays.stream(models).map(m -> detect(x, y, m)).max(Comparator.comparingDouble(eq -> eq.getRSquared()));
        return bestModel.get();
    }

    private static MathEquation detect(double[] x, double[] y, MathModel model) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y size must match");
        }

        Function<double[], double[]> function = new Function<double[], double[]>() {
            @Override
            public double[] apply(double[] doubles) {
                double[] result = new double[x.length];
                DoubleUnaryOperator func = model.getFunction().apply(doubles);
                for (int i = 0; i < x.length; i++) {
                    result[i] = y[i] - func.applyAsDouble(x[i]);
                }
                return result;
            }
        };
        double[] results = newtonSolve(function, model.getInitialValues(), 0.00001);
        DoubleUnaryOperator finalFunction = model.getFunction().apply(results);

        double[] funcValues = function.apply(results);
        double yAverage = Arrays.stream(y).average().getAsDouble();
        double variance = 0;
        double residualSumOfSquares = 0;
        double explainedSumOfSquares = 0;

        for (int i = 0; i < y.length; i++) {
            double yi = y[i];
            double fi = finalFunction.applyAsDouble(x[i]);
            variance += (yi - yAverage) * (yi - yAverage);
            residualSumOfSquares += (yi - fi) * (yi - fi);
            explainedSumOfSquares += (fi - yAverage) * (fi - yAverage);
        }

        double rSquared = 1 - residualSumOfSquares / variance;
        System.out.println(Arrays.toString(x));
        System.out.println(Arrays.toString(y));
        System.out.println(Arrays.toString(results));
        MathEquation eq = new MathEquation(finalFunction, results, model.getFormat(), rSquared);
        System.out.println(String.format("%f variance, %f residual sum, %f avg, %f rsquared, %f explained sum of squares",
                variance, residualSumOfSquares, yAverage, rSquared, explainedSumOfSquares));
        System.out.println(eq);
        System.out.println();
        return new MathEquation(finalFunction, results, model.getFormat(), rSquared);
    }

    public static void main(String[] args) {
        detect(new double[]{ 42, 107, 73, 120 }, new double[]{ 511, 312, 400, 242 }, Models.N_SQUARED);

        detect(new double[]{ 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0, 2048.0, 4096.0, 8192.0, 16384.0, 32768.0, 65536.0, 131072.0, 262144.0, 524288.0 },
                new double[]{ 905.0, 901.0, 939.0, 927.0, 920.0, 898.0, 884.0, 861.0, 852.0, 864.0, 869.0, 867.0, 866.0, 867.0, 857.0, 857.0, 854.0, 855.0, 872.0, 865.0 },
                Models.LINEAR);

        detect(new double[]{ 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0, 2048.0, 4096.0, 8192.0, 16384.0, 32768.0, 65536.0, 131072.0, 262144.0, 524288.0 },
               new double[]{ 857.0, 860.0, 898.0, 975.0, 993.0, 1601.0, 1530.0, 2947.0, 6106.0, 16111.0, 35937.0, 80497.0, 184819.0, 390424.0, 847658.0, 1820366.0, 4095873.0, 8463674.0, 17483933, 39126742 },
                Models.N_LOG_N);

        detect(new double[]{ 4, 10, 15 },
                new double[]{ 10.1146, 42, 74.0937 },
                Models.N_LOG_N);
    }

}
