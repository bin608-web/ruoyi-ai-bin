package org.ruoyi.skills.service.core;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Skill executor - executes skill code safely
 * This is a simplified executor. In production, you would use a sandboxed environment.
 */
@Component
public class SkillExecutor {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final long EXECUTION_TIMEOUT_MS = 30000; // 30 seconds

    /**
     * Execute skill code with input and parameters
     * @param code The skill code to execute
     * @param input The input data
     * @param parameters JSON string of parameters
     * @return The output from the skill
     */
    public String executeSkill(String code, String input, String parameters) throws Exception {
        // Determine language and execute accordingly
        if (code.contains("public class") && code.contains("public static void main")) {
            return executeJava(code, input, parameters);
        } else if (code.startsWith("def ") || code.startsWith("import ")) {
            return executePython(code, input, parameters);
        } else if (code.startsWith("function ") || code.startsWith("const ") || code.startsWith("let ")) {
            return executeJavaScript(code, input, parameters);
        } else {
            // Default to Java
            return executeJava(code, input, parameters);
        }
    }

    private String executeJava(String code, String input, String parameters) throws Exception {
        // Extract class name
        String className = "Skill";
        if (code.contains("public class ")) {
            int start = code.indexOf("public class ") + 13;
            int end = code.indexOf(" ", start);
            if (end == -1) end = code.indexOf("{", start);
            className = code.substring(start, end).trim();
        }

        // Create temporary directory
        Path tempDir = Files.createTempDirectory("skill_" + className + "_");
        Path sourceFile = tempDir.resolve(className + ".java");

        // Write source file
        Files.write(sourceFile, code.getBytes());

        // Compile
        ProcessBuilder compileBuilder = new ProcessBuilder("javac", sourceFile.toString());
        compileBuilder.directory(tempDir.toFile());
        Process compileProcess = compileBuilder.start();

        int compileResult = compileProcess.waitFor();
        if (compileResult != 0) {
            String error = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()))
                .lines().reduce("", (a, b) -> a + b + "\n");
            throw new RuntimeException("Compilation failed: " + error);
        }

        // Prepare input data
        Map<String, String> env = new HashMap<>();
        env.put("SKILL_INPUT", input != null ? input : "");
        env.put("SKILL_PARAMS", parameters != null ? parameters : "");

        // Execute
        ProcessBuilder execBuilder = new ProcessBuilder("java", "-cp", tempDir.toString(), className);
        execBuilder.environment().putAll(env);
        execBuilder.directory(tempDir.toFile());

        Process execProcess = execBuilder.start();

        // Wait with timeout
        boolean completed = execProcess.waitFor(EXECUTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!completed) {
            execProcess.destroyForcibly();
            throw new RuntimeException("Execution timed out");
        }

        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(execProcess.getInputStream()));
        String output = reader.lines().reduce("", (a, b) -> a + b + "\n");

        // Clean up
        deleteDirectory(tempDir.toFile());

        return output.trim();
    }

    private String executePython(String code, String input, String parameters) throws Exception {
        Path tempFile = Files.createTempFile("skill_", ".py");
        Files.write(tempFile, code.getBytes());

        ProcessBuilder builder = new ProcessBuilder("python", tempFile.toString());
        builder.environment().put("SKILL_INPUT", input != null ? input : "");
        builder.environment().put("SKILL_PARAMS", parameters != null ? parameters : "");

        Process process = builder.start();

        boolean completed = process.waitFor(EXECUTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Execution timed out");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output = reader.lines().reduce("", (a, b) -> a + b + "\n");

        Files.delete(tempFile);

        return output.trim();
    }

    private String executeJavaScript(String code, String input, String parameters) throws Exception {
        Path tempFile = Files.createTempFile("skill_", ".js");
        Files.write(tempFile, code.getBytes());

        ProcessBuilder builder = new ProcessBuilder("node", tempFile.toString());
        builder.environment().put("SKILL_INPUT", input != null ? input : "");
        builder.environment().put("SKILL_PARAMS", parameters != null ? parameters : "");

        Process process = builder.start();

        boolean completed = process.waitFor(EXECUTION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Execution timed out");
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String output = reader.lines().reduce("", (a, b) -> a + b + "\n");

        Files.delete(tempFile);

        return output.trim();
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
