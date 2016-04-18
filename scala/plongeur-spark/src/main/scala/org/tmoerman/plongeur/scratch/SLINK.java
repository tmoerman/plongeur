package org.tmoerman.plongeur.scratch;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class SLINK {

    interface Function2<A, B, C> {
        C apply(A a, B b);
    }

    public static final class SLINKClusteringResult {

        public final double[] height;
        public final int[] parent;

        public SLINKClusteringResult(double[] height, int[] parent) {
            this.height = height;
            this.parent = parent;
        }

    }

    public static <A> SLINKClusteringResult slink(List<A> data, Function2<A, A, Double> distance) {
        final int size = data.size();

        // WTF: distances as integers???

        final double[] height    = new double[size];
        final int[] parent       = new int[size];
        final double[] distanceN = new double[size];

        for (int n = 0; n < size; n++) { // conflation of adding a new point (index) and data structures -> maps could be possible

            // Step 1
            parent[n] = n;
            height[n] = Integer.MAX_VALUE;

            // Step 2
            for (int i = 0; i < n; i++) {
                distanceN[i] = distance.apply(data.get(i), data.get(n));
            }

            // Step 3
            for (int i = 0; i < n; i++) {
                if (height[i] >= distanceN[i]) {
                    distanceN[parent[i]] = Math.min(distanceN[parent[i]], height[i]);
                    height[i] = distanceN[i];
                    parent[i] = n;
                } else {
                    distanceN[parent[i]] = Math.min(distanceN[parent[i]], distanceN[i]);
                }
            }

            // Step 4
            for (int i = 0; i < n; i++) {
                if (height[i] >= height[parent[i]]) {
                    parent[i] = n;
                }
            }

        }
        return new SLINKClusteringResult(height, parent);
    }

    static class Point {
        double x, y;
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    static Function2<Point, Point, Double> euclidean = new Function2<Point, Point, Double>() {
        public Double apply(Point p1, Point p2) {
            return sqrt(pow(p1.x - p2.x, 2) + pow(p1.y - p2.y, 2));
        }
    };

    public static void main(String... args) {

        final double[][] data = {
                {0, 0}, {0, 1}, {1, 0}, {1, 1},
                {3, 3}, {3, 4}, {4, 3}, {4, 4}};

        final List<Point> all = new ArrayList<Point>() {{
            for (double[] point : data) {
                add(new Point(point[0], point[1]));
            }
        }};

        SLINKClusteringResult result = slink(all, euclidean);


    }


}
