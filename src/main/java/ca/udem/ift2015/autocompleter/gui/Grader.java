package ca.udem.ift2015.autocompleter.gui;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Consumer;

/**
 * Compile les classes de test (mvn test-compile), puis exécute chaque suite
 * JUnit in-process via le Platform Launcher et retourne les résultats.
 */
public class Grader {

    // ── Data records ──────────────────────────────────────────────────────────

    public record TestResult(String methodName, boolean passed, String errorMsg) {}

    public record ComponentScore(String label, String testClass, double maxPts,
                                 List<TestResult> tests) {

        public int    passed() { return (int) tests.stream().filter(TestResult::passed).count(); }
        public int    total()  { return tests.size(); }
        public double earned() { return total() == 0 ? 0 : (double) passed() / total() * maxPts; }
    }

    // ── Component definitions ─────────────────────────────────────────────────

    private record ComponentDef(String label, String testClass, double maxPts) {}

    static final List<ComponentDef> COMPONENTS = List.of(
            new ComponentDef("HashFrequencyTable",
                    "ca.udem.ift2015.autocompleter.student.HashFrequencyTableTest",  8.0),
            new ComponentDef("HeapTopKStrategy",
                    "ca.udem.ift2015.autocompleter.student.HeapTopKStrategyTest",   10.0),
            new ComponentDef("KatzBackoffModel",
                    "ca.udem.ift2015.autocompleter.student.KatzBackoffModelTest",   18.0),
            new ComponentDef("PrefixTrie",
                    "ca.udem.ift2015.autocompleter.student.PrefixTrieTest",         14.0)
    );

    // ── Main grading method ───────────────────────────────────────────────────

    /**
     * Compile les tests puis les exécute in-process.
     *
     * @param projectRoot répertoire contenant {@code pom.xml}
     * @param onStatus    callback pour les messages de statut
     */
    public static List<ComponentScore> grade(File projectRoot, Consumer<String> onStatus)
            throws IOException, InterruptedException {

        // 1. Compile test classes (fast — no test execution)
        onStatus.accept("Compilation des tests…");
        compileTests(projectRoot, onStatus);

        // 2. URLClassLoader pointing only to target/test-classes/
        //    Student code in target/classes/ is already on the parent classloader.
        File testClassesDir = new File(projectRoot, "target/test-classes");

        // 3. Run each component's tests in-process via JUnit Platform Launcher
        List<ComponentScore> scores = new ArrayList<>();
        try (URLClassLoader testLoader = new URLClassLoader(
                new URL[]{ testClassesDir.toURI().toURL() },
                Grader.class.getClassLoader())) {
            for (ComponentDef def : COMPONENTS) {
                onStatus.accept("Tests : " + def.label() + "…");
                scores.add(runComponent(def, testLoader));
            }
        }
        return scores;
    }

    // ── In-process JUnit execution ────────────────────────────────────────────

    private static ComponentScore runComponent(ComponentDef def, URLClassLoader loader) {
        Class<?> testClass;
        try {
            testClass = loader.loadClass(def.testClass());
        } catch (ClassNotFoundException e) {
            return new ComponentScore(def.label(), def.testClass(), def.maxPts(), List.of());
        }

        List<TestResult> results = new ArrayList<>();

        LauncherDiscoveryRequest req = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testClass))
                .build();

        try {
            Launcher launcher = LauncherFactory.create();
            launcher.execute(req, new TestExecutionListener() {
                @Override
                public void executionFinished(TestIdentifier id,
                                              TestExecutionResult result) {
                    if (!id.isTest()) return;
                    String name = id.getDisplayName();
                    if (name.endsWith("()")) name = name.substring(0, name.length() - 2);
                    boolean passed = result.getStatus()
                            == TestExecutionResult.Status.SUCCESSFUL;
                    String msg = result.getThrowable()
                            .map(t -> t.getMessage() != null ? t.getMessage()
                                                             : t.toString())
                            .orElse("");
                    results.add(new TestResult(name, passed, msg));
                }
            });
        } catch (Exception e) {
            results.add(new TestResult("(erreur launcher)",
                    false, e.getMessage() != null ? e.getMessage() : e.toString()));
        }

        return new ComponentScore(def.label(), def.testClass(), def.maxPts(), results);
    }

    // ── mvn test-compile subprocess ───────────────────────────────────────────

    private static void compileTests(File projectRoot, Consumer<String> onLine)
            throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String mvn = windows ? "mvn.cmd" : "mvn";
        ProcessBuilder pb = new ProcessBuilder(mvn, "test-compile", "--no-transfer-progress");
        pb.directory(projectRoot);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) onLine.accept(line);
        }
        int code = p.waitFor();
        if (code != 0) throw new IOException("Compilation échouée (code " + code + ")");
    }
}
