package org.hibernate.search.develocity;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import com.gradle.maven.extension.api.cache.NormalizationProvider;


public abstract class SimpleConfiguredPlugin implements ConfiguredPlugin {

    @Override
    public void configureBuildCache(GradleEnterpriseApi gradleEnterpriseApi, MavenSession mavenSession) {
        gradleEnterpriseApi.getBuildCache().registerMojoMetadataProvider(context -> {
            context.withPlugin(getPluginName(), () -> {
                if (!isBuildCacheEnabled(context.getProject())) {
                    Log.debug(getPluginName(), "Build cache is disabled.");
                    return;
                }

                Map<String, GoalMetadataProvider> goalMetadataProviders = Collections.unmodifiableMap(getGoalMetadataProviders());

                Log.debug(getPluginName(), "Build cache is enabled. Configuring metadata providers.");
                Log.debug(getPluginName(), "Configuring metadata for goals: " + goalMetadataProviders.keySet());

                for (Entry<String, GoalMetadataProvider> goalMetadataProviderEntry : goalMetadataProviders.entrySet()) {
                    if (goalMetadataProviderEntry.getKey().equalsIgnoreCase(context.getMojoExecution().getGoal())) {
                        goalMetadataProviderEntry.getValue().configure(context);
                    }
                }
            });
        });
    }

    protected abstract String getPluginName();

    protected boolean isBuildCacheEnabled(MavenProject project) {
        return true;
    }

    protected abstract Map<String, GoalMetadataProvider> getGoalMetadataProviders();

    protected static void dependsOnGav(MojoMetadataProvider.Context.Inputs inputs, MojoMetadataProvider.Context context) {
        inputs.property("_internal_gav", context.getProject().getGroupId() + ":" + context.getProject().getArtifactId() + ":" + context.getProject().getVersion());
    }


    protected static void dependsOnOs(MojoMetadataProvider.Context.Inputs inputs) {
        inputs.property("_internal_osName", System.getProperty("os.name"))
            .property("_internal_osVersion", System.getProperty("os.version"))
            .property("_internal_osArch", System.getProperty("os.arch"));
    }

    protected static void dependsOnMavenJavaVersion(MojoMetadataProvider.Context.Inputs inputs) {
        inputs.property("_internal_javaVersion", System.getProperty("java.version"));
    }

    @FunctionalInterface
    public interface PluginNormalizationProvider {

        void configure(NormalizationProvider.Context context);
    }

    @FunctionalInterface
    public interface GoalMetadataProvider {

        void configure(MojoMetadataProvider.Context context);
    }
}
