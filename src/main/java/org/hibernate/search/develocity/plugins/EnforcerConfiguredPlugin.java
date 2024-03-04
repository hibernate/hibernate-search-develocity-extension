package org.hibernate.search.develocity.plugins;

import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.SimpleConfiguredPlugin;

public class EnforcerConfiguredPlugin extends SimpleConfiguredPlugin {

    @Override
    protected String getPluginName() {
        return "maven-enforcer-plugin";
    }

    @Override
    protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
        return Map.of(
                "enforce", EnforcerConfiguredPlugin::configureEnforce);
    }

    private static void configureEnforce(GoalMetadataProvider.Context context) {
        var metadata = context.metadata();
        metadata.inputs(inputs -> {
            dependsOnGav(inputs, context);
            inputs.properties("skip", "fail", "failFast", "failIfNoRules", "rules", "rulesToExecute", "rulesToSkip",
                    "ignoreCache");
            dependsOnOs(inputs);
            dependsOnMavenJavaVersion(inputs);

            String dependencies = metadata.getProject().getArtifacts().stream()
                    .map(a -> a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion() + ":" + a.getClassifier())
                    .sorted()
                    .collect(Collectors.joining("\n"));

            inputs.property("dependencies", dependencies);

            inputs.ignore("project", "mojoExecution", "session");
        });

        metadata.outputs(outputs -> {
            outputs.cacheable("If the inputs and dependencies are identical, we should have the same output");
        });
    }
}
