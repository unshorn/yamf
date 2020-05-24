package nz.ac.wgtn.yamf.checks.junit;

import com.google.common.base.Preconditions;
import nz.ac.wgtn.yamf.commons.OS;
import nz.ac.wgtn.yamf.commons.XML;
import org.zeroturnaround.exec.ProcessResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Actions to run tests (usually acceptance tests).
 * Details from test runs are extracted from xml reports generated by the junit runner, and stored in JUNIT_REPORT_FOLDER.
 * The console output is also captured. This could be parsed (instead of parsing the xml files), see outcommented code at the end.
 * Parsing XML files seems to be more reliable.
 * @author jens dietrich
 */
public class JUnitActions {

    public static final String JUNIT_REPORT_FOLDER = ".junit-reports";
    public static final String JUPITER_REPORT_NAME = "TEST-junit-jupiter.xml";
    public static final String VINTAGE_REPORT_NAME = "TEST-junit-vintage.xml";

    public static final DateFormat FOLDERNAME_FROM_TIMESTAMP_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd--HH-mm-ss--SSS");

    /**
     * Run unit tests, and return results.
     * @param junitRunner the junit runner library, for instance junit-platform-console-standalone-1.6.2.jar
     * @param testClass the name of the class with tests
     * @param classpath the classpath to be used
     * @param includeJUnitReports whether to include the content of the generated junit report(s) in the test results
     * @return test results
     * @throws Exception
     */
    public static TestResults test (File junitRunner,String testClass,String classpath,boolean includeJUnitReports) throws Exception {

        // see https://junit.org/junit5/docs/current/user-guide/  , section 4.3
        Preconditions.checkArgument(junitRunner!=null,"JUnit runner library must be provided (junit-platform-console-standalone-1.6.2.jar or similar)");
        Preconditions.checkArgument(junitRunner.exists(),"JUnit runner not found (junit-platform-console-standalone-1.6.2.jar or similar): " + junitRunner.getAbsolutePath());

        File junitReportFolder = new File(new File(JUNIT_REPORT_FOLDER),""+FOLDERNAME_FROM_TIMESTAMP_FORMAT.format(new Date())); // unique folder names
        junitReportFolder.mkdirs();

        ProcessResult result = null;
        if (classpath==null) {
            result = OS.exe(new File("."), "java","-jar", junitRunner.getAbsolutePath(), "-reports-dir",junitReportFolder.getAbsolutePath(),"-c",testClass);
        }
        else {
            result = OS.exe(new File("."), "java","-jar", junitRunner.getAbsolutePath(), "-reports-dir",junitReportFolder.getAbsolutePath(),"-cp",classpath,"-c",testClass);
        }

        // parse results
        String output = result.outputString();

        // DEBUGGING ONLY
        System.out.println(output);

        TestResults testResults = new TestResults();
        testResults.setConsoleOutput(output);

        if (junitReportFolder.exists()) {
            String details = "";
            if (includeJUnitReports) {
                for (File junitReport : junitReportFolder.listFiles(fl -> !fl.isHidden())) {
                    try (Stream<String> stream = Files.lines(junitReport.toPath())) {
                        details = details + "======= " + junitReport.getName() + " =======\n";
                        details = details + stream.collect(Collectors.joining());
                    } catch (IOException e) {
                        throw new Exception(e);
                    }
                }
            }
            else {
                details = "junit reports were generated in " + junitReportFolder.getAbsolutePath();
            }
            testResults.setDetails(details);
        }

        // parse XML report(s)
        extractStatsFromReport(new File(junitReportFolder,JUPITER_REPORT_NAME),testResults);
        extractStatsFromReport(new File(junitReportFolder,VINTAGE_REPORT_NAME),testResults);

        return testResults;
    }

    private static void extractStatsFromReport(File junitReport, TestResults testResults) throws Exception {
        assert junitReport.exists() : "generated junit report does not exist and cannot be parsed for test outcome: " + junitReport.getAbsolutePath();
        int testCount = XML.evalXPathSingleNodeAsInt(junitReport, "/testsuite/@tests");
        testResults.addToTests(testCount);
        int testsFailed = XML.evalXPathSingleNodeAsInt(junitReport, "/testsuite/@failures");
        testResults.addToTestsFailed(testsFailed);
        int testSkippedCount = XML.evalXPathSingleNodeAsInt(junitReport, "/testsuite/@skipped");
        testResults.addToTestsSkipped(testSkippedCount);
        int testResultingInErrorCounts = XML.evalXPathSingleNodeAsInt(junitReport, "/testsuite/@errors");
        testResults.addToTestsWithErrors(testResultingInErrorCounts);
    }


}
