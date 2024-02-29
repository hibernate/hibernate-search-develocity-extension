package org.hibernate.search.develocity.plugins;

import java.util.Map;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

public class FailsafeConfiguredPlugin extends SurefireConfiguredPlugin {

	@Override
	protected String getPluginName() {
		return "maven-failsafe-plugin";
	}

	@Override
	protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
		return Map.of(
				"integration-test", FailsafeConfiguredPlugin::configureIntegrationTest
		);
	}

	private static void configureIntegrationTest(MojoMetadataProvider.Context context) {
		configureTest( context );
	}
}
