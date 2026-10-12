package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import parser.ComputationNode;
import spl.lae.LinearAlgebraEngine;
import java.nio.file.*;
import java.util.*;

/** Small end-to-end experiment, not a substitute for JMH or controlled profiling. */
public final class MatrixRace {
    public record Sample(String mode, int workers, double medianMs, double speedup, double maxError, String workerReport) {}
    public record Report(int size, int repetitions, int warmups, long seed, String javaVersion, int processors, List<Sample> samples) {}
    private static volatile double blackhole;
    private MatrixRace() {}
    public static double[][] multiplySerial(double[][] a, double[][] b) {
        double[][] c = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < b[0].length; j++) {
                double sum = 0;
                for (int k = 0; k < b.length; k++) sum += a[i][k] * b[k][j];
                c[i][j] = sum;
            }
        return c;
    }
    public static double maxError(double[][] expected, double[][] actual) {
        if (actual.length != expected.length) throw new IllegalStateException("Incorrect result shape");
        double error = 0;
        for (int i = 0; i < expected.length; i++) {
            if (actual[i].length != expected[i].length) throw new IllegalStateException("Incorrect result shape");
            for (int j = 0; j < expected[i].length; j++) {
                if (!Double.isFinite(actual[i][j])) throw new IllegalStateException("Non-finite result");
                double delta = Math.abs(expected[i][j] - actual[i][j]);
                if (delta > 1e-9 * Math.max(1, Math.abs(expected[i][j])))
                    throw new IllegalStateException("Numerical verification failed at " + i + "," + j);
                error = Math.max(error, delta);
            }
        }
        blackhole = actual[0][0];
        return error;
    }
    private static double median(List<Double> values) {
        List<Double> copy = new ArrayList<>(values); Collections.sort(copy);
        int mid = copy.size() / 2;
        return copy.size() % 2 == 0 ? (copy.get(mid - 1) + copy.get(mid)) / 2 : copy.get(mid);
    }
    public static Report run(int size, int maxWorkers, int repetitions, long seed) {
        if (size < 1 || size > 1024 || maxWorkers < 1 || maxWorkers > 32 || repetitions < 1 || repetitions > 100)
            throw new IllegalArgumentException("size: 1..1024; maxWorkers: 1..32; repetitions: 1..100");
        Random random = new Random(seed);
        double[][] a = new double[size][size], b = new double[size][size];
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) {
            a[i][j] = random.nextDouble() * 2 - 1; b[i][j] = random.nextDouble() * 2 - 1;
        }
        double[][] reference = multiplySerial(a, b);
        List<Integer> counts = new ArrayList<>();
        for (int n = 1; n <= maxWorkers; n *= 2) counts.add(n);
        if (!counts.contains(maxWorkers)) counts.add(maxWorkers);
        List<LinearAlgebraEngine> engines = new ArrayList<>();
        List<List<Double>> times = new ArrayList<>();
        for (int i = 0; i <= counts.size(); i++) times.add(new ArrayList<>());
        int warmups = 3;
        double[] errors = new double[counts.size() + 1];
        try {
            for (int count : counts) engines.add(new LinearAlgebraEngine(count));
            Random orderRandom = new Random(seed ^ 12345L);
            for (int round = -warmups; round < repetitions; round++) {
                List<Integer> order = new ArrayList<>();
                for (int i = 0; i <= counts.size(); i++) order.add(i);
                Collections.shuffle(order, orderRandom);
                for (int mode : order) {
                    // Fresh tree: the expression engine resolves nodes in-place.
                    ComputationNode tree = new ComputationNode("*", List.of(new ComputationNode(a), new ComputationNode(b)));
                    long start = System.nanoTime();
                    double[][] result = mode == 0 ? multiplySerial(a, b) : engines.get(mode - 1).run(tree).getMatrix();
                    double elapsed = (System.nanoTime() - start) / 1_000_000.0;
                    errors[mode] = Math.max(errors[mode], maxError(reference, result));
                    if (round >= 0) times.get(mode).add(elapsed);
                }
            }
            double baseline = median(times.get(0));
            List<Sample> samples = new ArrayList<>();
            samples.add(new Sample("serial", 0, baseline, 1, errors[0], "Direct triple-loop kernel"));
            for (int i = 0; i < engines.size(); i++) {
                double ms = median(times.get(i + 1));
                samples.add(new Sample("engine", counts.get(i), ms, baseline / ms, errors[i + 1], engines.get(i).getWorkerReport()));
            }
            return new Report(size, repetitions, warmups, seed, System.getProperty("java.version"), Runtime.getRuntime().availableProcessors(), samples);
        } finally { for (LinearAlgebraEngine engine : engines) engine.close(); }
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 5) throw new IllegalArgumentException("race [size] [maxWorkers] [repetitions] [outputDirectory] [seed]");
        int size = args.length > 0 ? Integer.parseInt(args[0]) : 128;
        int workers = args.length > 1 ? Integer.parseInt(args[1]) : Math.min(4, Runtime.getRuntime().availableProcessors());
        int repetitions = args.length > 2 ? Integer.parseInt(args[2]) : 5;
        Path out = Path.of(args.length > 3 ? args[3] : "results");
        long seed = args.length > 4 ? Long.parseLong(args[4]) : 42L;
        Report report = run(size, workers, repetitions, seed);
        Files.createDirectories(out);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out.resolve("matrix-race.json").toFile(), report);
        StringBuilder csv = new StringBuilder("mode,workers,median_ms,speedup_vs_serial,max_absolute_error\n");
        System.out.printf("Matrix Race | %dx%d | seed=%d | repetitions=%d | warmups=%d%n", size, size, seed, repetitions, report.warmups());
        for (Sample s : report.samples()) {
            String row = String.format(Locale.ROOT, "%s,%d,%.6f,%.4f,%.12g%n", s.mode(), s.workers(), s.medianMs(), s.speedup(), s.maxError());
            csv.append(row);
            System.out.printf(Locale.ROOT, "%-7s workers=%2d | %9.3f ms | %6.2fx | verified%n", s.mode(), s.workers(), s.medianMs(), s.speedup());
        }
        Files.writeString(out.resolve("matrix-race.csv"), csv);
        Files.writeString(out.resolve("matrix-race.html"), html(report));
        System.out.println("Report: " + out.resolve("matrix-race.html").toAbsolutePath());
        System.out.println("Engine timing includes validation, copies, scheduling and result assembly. Worker startup, verification and file I/O are excluded.");
    }
    private static String html(Report r) {
        double max = r.samples().stream().mapToDouble(Sample::medianMs).max().orElse(1);
        StringBuilder rows = new StringBuilder();
        for (Sample s : r.samples()) rows.append(String.format(Locale.ROOT,
            "<tr><td>%s</td><td>%d</td><td><div class='bar' style='width:%.2f%%'></div>%.3f ms</td><td>%.2fx</td><td>Verified</td></tr>",
            s.mode(), s.workers(), 100 * s.medianMs() / Math.max(max, 0.000001), s.medianMs(), s.speedup()));
        StringBuilder telemetry = new StringBuilder();
        for (Sample s : r.samples()) if (s.workers() > 0)
            telemetry.append("<details><summary>").append(s.workers()).append(" workers: task distribution</summary><pre>")
                .append(s.workerReport()).append("</pre></details>");
        return """
            <!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
            <title>MatrixLab | Matrix Race</title><style>
            body{background:#101827;color:#e9eff7;font:16px system-ui;margin:0;padding:6vw}main{max-width:1000px;margin:auto}
            h1{font-size:clamp(36px,6vw,64px);margin:12px 0} .eyebrow{color:#63ded2;letter-spacing:.16em;font-size:12px}
            p{color:#b4c2d4;line-height:1.7}table{width:100%;border-collapse:collapse;margin:32px 0}th,td{text-align:left;padding:18px 12px;border-bottom:1px solid #354257}
            th{color:#a5b5c9}td:nth-child(3){width:40%}.bar{height:5px;background:#63ded2;margin-bottom:9px;border-radius:3px}
            details{padding:16px;background:#1b283b;margin:10px 0;border-radius:8px}pre{overflow:auto;font-size:12px;line-height:1.7}
            .note{font-size:14px}a{color:#63ded2}</style><main><div class="eyebrow">MATRIXLAB / PERFORMANCE EXPLORER</div>
            <h1>Matrix Race</h1>
            """ + "<p>" + r.size() + " × " + r.size() + " matrix multiplication · " + r.repetitions() + " measured runs · " + r.warmups() + " warmups · seed " + r.seed() + "</p>"
            + "<p>Median duration: shorter bars are better. A speedup below 1 means the engine was slower than the direct serial kernel.</p>"
            + "<table><thead><tr><th>Mode</th><th>Workers</th><th>Median runtime</th><th>Speedup</th><th>Correctness</th></tr></thead><tbody>" + rows + "</tbody></table>"
            + telemetry + "<p class='note'>Task counts include warmups. Inputs and measurement order are seeded; scheduling and timing are not deterministic. All measured outputs were checked against the serial reference."
            + " Engine timing includes validation, matrix copies, synchronization, scheduling and output assembly. Worker startup, numerical verification and file writes are excluded."
            + " Results describe this machine and workload; this is an educational experiment, not a JMH benchmark or a claim of universal speedup.</p></main></html>";
    }
}
