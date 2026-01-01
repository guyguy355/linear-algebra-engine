package spl.lae;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import parser.ComputationNode;
import parser.InputParser;
import spl.lae.LinearAlgebraEngine;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class LinearAlgebraEngineTest {
    final private int THREAD_COUNT = 3;

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4,5,6})
    public void testExamplesFolder(int index) throws IOException, ParseException, InterruptedException {
        testExample(index);
    }

    public static double[][] parseResultMatrix(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(filePath));

        JsonNode resultNode = root.get("result");
        if (resultNode == null || !resultNode.isArray()) {
            throw new IllegalArgumentException("Invalid format: 'result' key not found or not an array");
        }

        int rows = resultNode.size();
        double[][] matrix = new double[rows][];

        for (int i = 0; i < rows; i++) {
            JsonNode rowNode = resultNode.get(i);
            int cols = rowNode.size();
            matrix[i] = new double[cols];
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = rowNode.get(j).asDouble();
            }
        }
        return matrix;
    }

    private void testExample(int index) throws IOException, ParseException, InterruptedException {
        String inputPath = "Examples/example" + index + ".json";
        String expectedPath = "Examples/out" + index + ".json";

        InputParser parser = new InputParser();

        ComputationNode inputNode = parser.parse(inputPath);

        double[][] expectedMatrix = parseResultMatrix(expectedPath);

        LinearAlgebraEngine engine = new LinearAlgebraEngine(THREAD_COUNT);
        ComputationNode resultNode = engine.run(inputNode);
        double[][] actualMatrix = resultNode.getMatrix();

        engine.shutdown();

        assertMatricesEqual(expectedMatrix, actualMatrix, "Mismatch in example " + index);
    }

    private void assertMatricesEqual(double[][] expected, double[][] actual, String message) {
        Assertions.assertNotNull(actual, message + ": Result matrix is null");
        Assertions.assertEquals(expected.length, actual.length, message + ": Row count mismatch");
        Assertions.assertEquals(expected[0].length, actual[0].length, message + ": Column count mismatch");

        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[i].length; j++) {
                Assertions.assertEquals(expected[i][j], actual[i][j],
                        String.format("%s: Value mismatch at [%d][%d]", message, i, j));
            }
        }
    }
}