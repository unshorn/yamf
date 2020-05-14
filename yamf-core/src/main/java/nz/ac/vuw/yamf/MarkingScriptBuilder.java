package nz.ac.vuw.yamf;

import com.google.common.base.Preconditions;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import nz.ac.vuw.yamf.reporting.Reporter;
import java.util.function.Function;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Marking script builder.
 * @author jens dietrich
 */
public class MarkingScriptBuilder {


    private Consumer<File> beforeMarkingEachProject = (projectFolder) -> {};
    private Consumer<File> afterMarkingEachProject = (projectFolder) -> {};
    private Runnable beforeMarkingAllProjects = () -> {};
    private Runnable afterMarkingAllProjects = () -> {};
    private List<Function<File, Reporter>> reporterFactories = new ArrayList<>();
    private File[] submissions = null;
    private Class markingScheme = null;

    public MarkingScriptBuilder beforeMarkingEachProjectDo(Consumer<File> action) {
        this.beforeMarkingEachProject = action;
        return this;
    }

    public MarkingScriptBuilder afterMarkingEachActionDo(Consumer<File> action) {
        this.afterMarkingEachProject = action;
        return this;
    }

    public MarkingScriptBuilder beforeMarkingAllProjectsDo(Runnable action) {
        this.beforeMarkingAllProjects = action;
        return this;
    }

    public MarkingScriptBuilder afterMarkingAllProjectsDo(Runnable action) {
        this.afterMarkingAllProjects = action;
        return this;
    }

    // the function maps submissions to reporters
    public MarkingScriptBuilder reportTo(Function<File,Reporter> reporter) {
        this.reporterFactories.add(reporter);
        return this;
    }

    public MarkingScriptBuilder submissions(File[] submissions) {
        this.submissions = submissions;
        return this;
    }

    public MarkingScriptBuilder markingScheme(Class markingScheme) {
        this.markingScheme = markingScheme;
        return this;
    }

    public void run() throws Exception {
        Preconditions.checkState(this.submissions!=null,"submissions to mark must be set");
        Preconditions.checkState(this.markingScheme!=null,"marking scheme must be set");
        Preconditions.checkState(!this.reporterFactories.isEmpty(),"at least one reporter must be set");

        beforeMarkingAllProjects.run();
        for (File projectFolder:submissions)   {
            if (projectFolder.isDirectory()) {
                beforeMarkingEachProject.accept(projectFolder);
                // junit boilerplate code
                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                    .request()
                    .selectors(selectClass(markingScheme)).build();
                Launcher launcher = LauncherFactory.create();
                MarkingTestExecutionListener listener = new MarkingTestExecutionListener() ;
                launcher.registerTestExecutionListeners(listener);
                launcher.execute(request);
                List<MarkingResultRecord> results = listener.getResults();
                for (Function<File,Reporter> reporterFactory:reporterFactories) {
                    Reporter reporter = reporterFactory.apply(projectFolder);
                    reporter.generateReport(results);
                }
                afterMarkingEachProject.accept(projectFolder);
            }
        }
        afterMarkingAllProjects.run();
    }

}
