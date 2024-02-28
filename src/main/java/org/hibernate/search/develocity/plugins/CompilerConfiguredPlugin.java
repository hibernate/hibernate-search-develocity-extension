package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.SimpleConfiguredPlugin;
import org.hibernate.search.develocity.util.JavaVersions;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

public class CompilerConfiguredPlugin extends SimpleConfiguredPlugin {

	@Override
	protected String getPluginName() {
		return "maven-compiler-plugin";
	}

	@Override
	protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
		return Map.of(
				"compile", CompilerConfiguredPlugin::configureCompile,
				"testCompile", CompilerConfiguredPlugin::configureCompile
		);
	}

	private static void configureCompile(MojoMetadataProvider.Context context) {
		context.inputs( inputs -> {
			String executable = context.getMojoExecution().getConfiguration().getAttribute( "executable" );
			if ( executable == null ) {
				dependsOnMavenJavaVersion( inputs );
			}
			else {
				inputs.properties(
						"_internal_executable_version",
						JavaVersions.forExecutable( executable )
				);
			}
		} );
	}
}
