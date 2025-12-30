package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: <numThreads> <input.json> <output.json>");
            return;
        }

        int threads;
        try {
            threads = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            OutputWriter.write("Invalid number of threads", args[2]);
            return;
        }

        String inFile = args[1];
        String outFile = args[2];

        LinearAlgebraEngine eng = null;
        long t0 = System.nanoTime();

        try {
            InputParser p = new InputParser();
            ComputationNode root = p.parse(inFile);

            eng = new LinearAlgebraEngine(threads);
            ComputationNode ans = eng.run(root);

            OutputWriter.write(ans.getMatrix(), outFile);

        } catch (IllegalArgumentException ex) {
            OutputWriter.write(ex.getMessage(), outFile);

        } catch (Exception ex) {
            OutputWriter.write(ex.getMessage(), outFile);

        } finally {
            long t1 = System.nanoTime();
            double elapsedMs = (t1 - t0) / 1_000_000.0;

            System.out.println("=== Total Runtime ===");
            System.out.println(elapsedMs + " ms");

            if (eng != null) {
                System.out.println("=== Worker Report ===");
                System.out.println(eng.getWorkerReport());
            }
        }
    }
}