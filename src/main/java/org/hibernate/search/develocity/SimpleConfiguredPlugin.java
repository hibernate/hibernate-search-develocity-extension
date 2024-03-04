package org.hibernate.search.develocity;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.hibernate.search.develocity.scan.BuildScanMetadata;
import org.hibernate.search.develocity.util.JavaVersions;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;


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

				for ( Entry<String, GoalMetadataProvider> goalMetadataProviderEntry : goalMetadataProviders.entrySet() ) {
					if ( goalMetadataProviderEntry.getKey().equalsIgnoreCase( context.getMojoExecution().getGoal() ) ) {
						goalMetadataProviderEntry.getValue()
								.configure( new GoalMetadataProvider.Context( gradleEnterpriseApi.getBuildScan(), context ) );
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

    protected static void dependsOnGav(MojoMetadataProvider.Context.Inputs inputs, GoalMetadataProvider.Context context) {
		var project = context.metadata().getProject();
        inputs.property("_internal_gav", project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
    }


    protected static void dependsOnOs(MojoMetadataProvider.Context.Inputs inputs) {
        inputs.property("_internal_osName", System.getProperty("os.name"))
            .property("_internal_osVersion", System.getProperty("os.version"))
            .property("_internal_osArch", System.getProperty("os.arch"));
    }

    protected static void dependsOnMavenJavaVersion(MojoMetadataProvider.Context.Inputs inputs) {
        inputs.property("_internal_javaVersion", System.getProperty("java.version"));
    }

    protected static void dependsOnConfigurableJavaExecutable(MojoMetadataProvider.Context.Inputs inputs,
			GoalMetadataProvider.Context context, String configChildName, Boolean skipped,
            Function<String, String> executableToVersion) {
		String javaExecutable = context.configuration().getString( configChildName );
		String javaVersion = executableToVersion.apply( javaExecutable );
		boolean canCacheExactVersion = context.properties().cacheExactJavaVersion();
		inputs.property( "_internal_" + configChildName + "_java_version",
				canCacheExactVersion
						? javaVersion
						: JavaVersions.toJdkMajor( javaVersion, javaVersion ) );

		if ( skipped == null || !skipped ) {
			BuildScanMetadata.addJavaExecutableVersion( context, javaExecutable, javaVersion, canCacheExactVersion );
			Log.info(
					context.metadata().getMojoExecution().getPlugin().getArtifactId(),
					"Using %s at path '%s'; resolved version: %s"
							.formatted( configChildName, javaExecutable, javaVersion.replace( '\n', ' ' ).trim() )
			);
		}
    }

}
