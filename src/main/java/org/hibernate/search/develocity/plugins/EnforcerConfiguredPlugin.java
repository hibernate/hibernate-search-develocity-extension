package org.hibernate.search.develocity.plugins;

import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.develocity.SimpleConfiguredPlugin;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

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

    private static void configureEnforce(MojoMetadataProvider.Context context) {
        context.inputs(inputs -> {
            dependsOnGav(inputs, context);
            inputs.properties("skip", "fail", "failFast", "failIfNoRules", "rules", "rulesToExecute", "rulesToSkip",
                    "ignoreCache");
            dependsOnOs(inputs);
            dependsOnJavaVersion(inputs);

            // TODO I'm not entirely sure Develocity considers the dependencies of the plugin in the inputs
            // typically in the case of the enforcer plugin, we add a dependency containing the rules
            // and this dependency should be added as an input if not
            // see context.getMojoExecution().getPlugin().getDependencies()

            String dependencies = context.getProject().getArtifacts().stream()
                    .map(a -> a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion() + ":" + a.getClassifier())
                    .sorted()
                    .collect(Collectors.joining("\n"));

            inputs.property("dependencies", dependencies);

            inputs.ignore("project", "mojoExecution", "session");
        });

        context.outputs(outputs -> {
            outputs.cacheable("If the inputs and dependencies are identical, we should have the same output");
        });
    }
}
