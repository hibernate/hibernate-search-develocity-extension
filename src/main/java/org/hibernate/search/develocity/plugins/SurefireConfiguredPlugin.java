package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.SimpleConfiguredPlugin;
import org.hibernate.search.develocity.util.JavaVersions;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

public class SurefireConfiguredPlugin extends SimpleConfiguredPlugin {

	@Override
	protected String getPluginName() {
		return "maven-surefire-plugin";
	}

	@Override
	protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
		return Map.of(
				"test", SurefireConfiguredPlugin::configureTest
		);
	}

	protected static void configureTest(MojoMetadataProvider.Context context) {
		context.inputs( inputs -> {
			dependsOnConfigurableJavaExecutable( inputs, context, "jvm", JavaVersions::forJavaExecutable );
		} );
	}
}
