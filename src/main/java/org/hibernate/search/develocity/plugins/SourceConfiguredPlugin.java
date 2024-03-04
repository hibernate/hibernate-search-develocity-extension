package org.hibernate.search.develocity.plugins;

import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.SimpleConfiguredPlugin;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider.Context.FileSet.NormalizationStrategy;

/**
 * TODO discuss this more in depth with Alexey, especially to make sure the output directory is not shared with other plugins.
 */
public class SourceConfiguredPlugin extends SimpleConfiguredPlugin {

    @Override
    protected String getPluginName() {
        return "maven-source-plugin";
    }

    @Override
    protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
        return Map.of(
                "jar-no-fork", SourceConfiguredPlugin::jarNoFork);
    }

    private static void jarNoFork(GoalMetadataProvider.Context context) {
        var metadata = context.metadata();
        var project = metadata.getProject();

        metadata.inputs(inputs -> {
            dependsOnGav(inputs, context);

            inputs.properties("classifier", "includes", "excludes", "useDefaultExcludes",
                    "useDefaultManifestFile", "attach", "excludeResources", "includePom", "finalName", "forceCreation",
                    "skipSource", "outputTimestamp");
            inputs.fileSet("defaultManifestFile", fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH));
            inputs.fileSet("resources", project.getResources().stream().map(r -> r.getDirectory())
                    .collect(Collectors.toList()),
                    fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH));
            inputs.fileSet("sources", project.getCompileSourceRoots(),
                    fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH));

            inputs.ignore("project", "jarArchiver", "archive", "outputDirectory", "reactorProjects", "session");
        });

        metadata.outputs(outputs -> {
            outputs.cacheable("If the inputs are identical, we should have the same output");
            outputs.file("source-jar", project.getBuild().getDirectory() + "/"
                    + project.getBuild().getFinalName() + "-sources.jar");
        });
    }
}
