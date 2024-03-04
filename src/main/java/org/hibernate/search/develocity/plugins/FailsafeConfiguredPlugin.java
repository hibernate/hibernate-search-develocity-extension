package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.scan.BuildScanMetadata;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

public class FailsafeConfiguredPlugin extends SurefireConfiguredPlugin {

	@Override
	protected String getPluginName() {
		return "maven-failsafe-plugin";
	}

	@Override
	protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
		return Map.of(
				"integration-test", this::configureIntegrationTest
		);
	}

	private void configureIntegrationTest(GoalMetadataProvider.Context context) {
		configureTest( context );
		context.metadata().inputs( inputs -> {
			// We try to be smart about which container is used for the build scan,
			// because an error would be of little consequence there,
			// but here it could lead to missing important regressions.
			// So we just consider all Dockerfiles are used in all integration tests,
			// and invalidate all integration test caches as soon as one Dockerfile changes.
			var containersPath = context.configuration()
					.getFailsafeSystemProperty( "org.hibernate.search.integrationtest.container.directory" );
			if ( containersPath != null ) {
				inputs.fileSet( "containers", containersPath, fileSet -> {
					fileSet.normalizationStrategy(
							MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH );
				} );
			}

			var repackagedJarPath = context.configuration()
					.getFailsafeSystemProperty( "test.repackaged-jar-path" );
			if ( repackagedJarPath != null ) {
				inputs.fileSet( "repackaged-jar", repackagedJarPath, fileSet -> {
					fileSet.normalizationStrategy(
							MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH );
				} );
			}
		} );

		if ( !isSkipped( context ) ) {
			BuildScanMetadata.addFailsafeMetadata( context );
		}
	}

	@Override
	protected boolean isSkipped(GoalMetadataProvider.Context context) {
		return context.configuration().getBoolean( "skip" )
			   || context.properties().getBoolean( "maven.test.skip" )
			   || context.configuration().getBoolean( "skipITs" )
			   || context.properties().getBoolean( "skipITs" )
			   || context.configuration().getBoolean( "skipTests" )
			   || context.properties().getBoolean( "skipTests" )
			   || context.configuration().getBoolean( "skipExec" )
			   || context.properties().getBoolean( "maven.test.skip.exec" );
	}

}
