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
