package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.scan.BuildScanMetadata;
import org.hibernate.search.develocity.util.MavenConfigs;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import com.gradle.maven.extension.api.scan.BuildScanApi;

import org.apache.maven.plugin.MojoExecution;

public class FailsafeConfiguredPlugin extends SurefireConfiguredPlugin {

	@Override
	protected String getPluginName() {
		return "maven-failsafe-plugin";
	}

	@Override
	protected Map<String, GoalMetadataProvider> getGoalMetadataProviders(BuildScanApi buildScanApi) {
		return Map.of(
				"integration-test", context -> configureIntegrationTest( context, buildScanApi )
		);
	}

	private static void configureIntegrationTest(MojoMetadataProvider.Context context, BuildScanApi buildScanApi) {
		configureTest( context );
		context.inputs( inputs -> {
			// We try to be smart about which container is used for the build scan,
			// because an error would be of little consequence there,
			// but here it could lead to missing important regressions.
			// So we just consider all Dockerfiles are used in all integration tests,
			// and invalidate all integration test caches as soon as one Dockerfile changes.
			var containersPath = MavenConfigs.getFailsafeSystemProperty( context.getMojoExecution(),
					"org.hibernate.search.integrationtest.container.directory" );
			if ( containersPath != null ) {
				inputs.fileSet( "containers", containersPath, fileSet -> {
					fileSet.normalizationStrategy(
							MojoMetadataProvider.Context.FileSet.NormalizationStrategy.RELATIVE_PATH );
				} );
			}
		} );
		if ( !isSkipped( context.getMojoExecution() ) ) {
			BuildScanMetadata.addFailsafeMetadataToBuildScan( buildScanApi, context.getSession(), context.getMojoExecution() );
		}
	}

	private static boolean isSkipped(MojoExecution mojoExecution) {
		return MavenConfigs.getBooleanConfig( mojoExecution, "skip" )
				|| MavenConfigs.getBooleanConfig( mojoExecution, "skipITs" )
				|| MavenConfigs.getBooleanConfig( mojoExecution, "skipExec" );
	}

}
