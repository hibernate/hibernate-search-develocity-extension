package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.SimpleConfiguredPlugin;
import org.hibernate.search.develocity.util.JavaVersions;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import com.gradle.maven.extension.api.scan.BuildScanApi;

public class CompilerConfiguredPlugin extends SimpleConfiguredPlugin {

	@Override
	protected String getPluginName() {
		return "maven-compiler-plugin";
	}

	@Override
	protected Map<String, GoalMetadataProvider> getGoalMetadataProviders(BuildScanApi buildScanApi) {
		return Map.of(
				"compile", CompilerConfiguredPlugin::configureCompile,
				"testCompile", CompilerConfiguredPlugin::configureCompile
		);
	}

	private static void configureCompile(MojoMetadataProvider.Context context) {
		context.inputs( inputs -> {
			dependsOnConfigurableJavaExecutable( inputs, context, "executable", JavaVersions::forJavacExecutable );
		} );
	}
}
