package com.voicestock.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class DatabasePersistenceIT {
    @TempDir Path directory;

    @Test
    void committedDataSurvivesCompleteApplicationProcessRestart() throws Exception {
        TestDatabase.required("TEST_DB_URL");
        TestDatabase.required("TEST_DB_USERNAME");
        TestDatabase.required("TEST_DB_PASSWORD");
        Path manifest = directory.resolve("record-ids.properties");
        try {
            assertThat(runProbe("write", manifest)).contains("PERSISTENCE_WRITE_COMMITTED");
            // The write process has exited: no application context or Hibernate cache remains.
            assertThat(runProbe("read", manifest)).contains("PERSISTENCE_RESTART_VERIFIED");
            assertThat(runProbe("complete", manifest)).contains("ACTION_COMPLETED");
            assertThat(runProbe("readCompleted", manifest)).contains("COMPLETED_ACTION_RESTART_VERIFIED");
        } finally {
            if (Files.exists(manifest)) runProbe("cleanup", manifest);
        }
    }

    private String runProbe(String action, Path manifest) throws Exception {
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Path args = directory.resolve(action + ".args");
        // An argument file avoids Windows command-line length limits. It holds no credentials.
        Files.writeString(args, "-cp\n\"" + classpath.replace('\\', '/') + "\"\n"
                + PersistenceProbe.class.getName() + "\n" + action + "\n\"" + manifest.toString().replace('\\', '/') + "\"\n");
        Path log = directory.resolve(action + ".log");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "@" + args).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Persistence probe timed out: " + action);
        }
        String output = Files.readString(log);
        // Redact even if an unexpected driver error happens to contain a credential.
        output = output.replace(TestDatabase.required("TEST_DB_PASSWORD"), "[REDACTED]");
        assertThat(process.exitValue()).withFailMessage("Probe %s failed:%n%s", action, output).isZero();
        return output;
    }
}

