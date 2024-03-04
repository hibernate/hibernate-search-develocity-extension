package org.hibernate.search.develocity.plugins;

import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.SimpleConfiguredPlugin;
import org.hibernate.search.develocity.util.JavaVersions;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

public class SurefireConfiguredPlugin extends SimpleConfiguredPlugin {

	private static final String SUREFIRE_ENVIRONMENT_VARIABLES = "environmentVariables";

	private static final Pattern TEST_INDEXES_PATTERN = Pattern.compile( "(^|/)test-indexes($|/)" );

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

		configureEnvironmentVariables(context);
	}

	// Develocity handles environment variables as a big blob by default,
	// which won't work if some variables point to absolute paths.
	private void configureEnvironmentVariables(GoalMetadataProvider.Context context) {
		context.metadata().inputs( inputs -> {
			// First, override the property to disable handling of environment variables as a blob.
			// NOTE: ignoring with inputs.ignore( "environmentVariables" ) doesn't work for some reason:
			// we end up with the goal being marked as "not cacheable"
			// because "properties were declared both as input and ignored: [environmentVariables]"
			// NOTE: we get the same result with context.nested( "environmentVariables" ),
			// which is why we don't use that.
			inputs.property( SUREFIRE_ENVIRONMENT_VARIABLES, "IGNORED" );

			// Then, try to mimic system properties handling.
			for ( Map.Entry<String, String> envVariable : context.configuration()
					.getStringMap( SUREFIRE_ENVIRONMENT_VARIABLES ).entrySet() ) {
				var key = envVariable.getKey();
				var keyForDevelocity = SUREFIRE_ENVIRONMENT_VARIABLES + "." + key;
				var value = envVariable.getValue();
				if ( value == null ) {
					value = "";
				}
				if ( value.startsWith( context.metadata().getSession().getExecutionRootDirectory() ) ) {
					if ( TEST_INDEXES_PATTERN.matcher( "test-indexes" ).find() ) {
						// Lucene indexes used in tests -- we don't care about these.
						inputs.ignore( keyForDevelocity );
					}
					else {
						inputs.fileSet( keyForDevelocity, value, fileSet -> {
							fileSet.normalizationStrategy(
									MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH );
						} );
					}
				}
				else {
					inputs.property( keyForDevelocity, value );
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
