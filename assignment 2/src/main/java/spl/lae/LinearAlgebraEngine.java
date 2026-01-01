package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads must be positive");
        }
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        if (computationRoot == null) {
            throw new IllegalArgumentException("computationRoot cannot be null");
        }

// apply associative nesting if required by the spec
        computationRoot.associativeNesting();

// keep resolving until the root becomes a matrix node
        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
            ComputationNode nextNode = computationRoot.findResolvable();
            if (nextNode == null) {
                throw new IllegalStateException("No resolvable node found");
            }

            loadAndCompute(nextNode);

        }

        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        if (node.getNodeType() == ComputationNodeType.MATRIX) {
            return;
        }
        for (ComputationNode child : node.getChildren()) {
            if (child.getNodeType() != ComputationNodeType.MATRIX) {
                throw new IllegalStateException("Cannot compute node: not all children are resolved.");
            }
        }

        leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());

        if (node.getChildren().size() > 1) {
            rightMatrix.loadRowMajor(node.getChildren().get(1).getMatrix());
        }

        List<Runnable> tasks;

        switch (node.getNodeType()) {
            case ADD:
                tasks = createAddTasks();
                break;
            case MULTIPLY:
                tasks = createMultiplyTasks();
                break;
            case NEGATE:
                tasks = createNegateTasks();
                break;
            case TRANSPOSE:
                tasks = createTransposeTasks();
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation: " + node.getNodeType());
        }

        if (tasks != null && !tasks.isEmpty()) {
            executor.submitAll(tasks);
            node.resolve(leftMatrix.readRowMajor());
        }

    }

    public List<Runnable> createAddTasks() {
        List<Runnable> tasks = new java.util.ArrayList<>();
        int rows = rightMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int r = i;

            tasks.add(() -> {
                try {
                    leftMatrix.get(r).add(rightMatrix.get(r));
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            });
        }

        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        List<Runnable> tasks = new java.util.ArrayList<>();
        int rows = leftMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int r = i;

            tasks.add(() -> {
                try {
                    leftMatrix.get(r).vecMatMul(rightMatrix);
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            });
        }

        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        List<Runnable> tasks = new java.util.ArrayList<>();
        int rows = leftMatrix.length();

        for (int i = 0; i < rows; i++) {
            final int r = i;

            tasks.add(() -> {
                try {
                    leftMatrix.get(r).negate();
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            });
        }

        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        List<Runnable> tasks = new java.util.LinkedList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            SharedVector vLeft = leftMatrix.get(i);
            tasks.add(() -> vLeft.transpose());
        }
        return tasks;
    }

    public String getWorkerReport() {
        return executor.getWorkerReport();
    }
    public void shutdown() throws InterruptedException {
        if (executor != null) {
            executor.shutdown();
        }
    }


}
