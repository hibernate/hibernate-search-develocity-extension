package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.SimpleConfiguredPlugin;
import org.hibernate.search.develocity.util.JavaVersions;
import org.hibernate.search.develocity.util.MavenMojoExecutionConfig;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

public class SurefireConfiguredPlugin extends SimpleConfiguredPlugin {

	@Override
	protected String getPluginName() {
		return "maven-surefire-plugin";
	}

	@Override
	protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
		return Map.of(
				"test", this::configureTest
		);
	}

	protected void configureTest(GoalMetadataProvider.Context context) {
		var metadata = context.metadata();
		metadata.inputs( inputs -> {
			dependsOnConfigurableJavaExecutable( inputs, context, "jvm", isSkipped( context ),
					JavaVersions::forJavaExecutable );
		} );

		// Develocity handles environment variables as a big bulk by default,
		// which won't work if some variables point to absolute paths.
		// Let's try to mimic system properties handling instead.
		// NOTE: ignoring with inputs.ignore( "environmentVariables" ) doesn't work for some reason...
		context.nested( MavenMojoExecutionConfig.SUREFIRE_ENVIRONMENT_VARIABLES,
				this::configureEnvironmentVariables );
	}

	private void configureEnvironmentVariables(GoalMetadataProvider.Context context) {
		context.metadata().inputs( inputs -> {
			for ( Map.Entry<String, String> envVariable : context.configuration()
					.getSurefireEnvironmentVariables().entrySet() ) {
				var key = envVariable.getKey();
				var value = envVariable.getValue();
				if ( value == null ) {
					value = "";
				}
				if ( value.startsWith( context.metadata().getSession().getExecutionRootDirectory() ) ) {
					inputs.fileSet( key, value, fileSet -> {
						fileSet.normalizationStrategy(
								MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH );
					} );
				}
				else {
					inputs.property( key, value );
				}
			}
		} );
	}

	protected boolean isSkipped(GoalMetadataProvider.Context context) {
		return context.configuration().getBoolean( "skip" )
				|| context.properties().getBoolean( "maven.test.skip" )
				|| context.configuration().getBoolean( "skipTests" )
				|| context.properties().getBoolean( "skipTests" )
				|| context.configuration().getBoolean( "skipExec" )
				|| context.properties().getBoolean( "maven.test.skip.exec" );
	}
}
