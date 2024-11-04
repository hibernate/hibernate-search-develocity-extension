package org.hibernate.infra.develocity.plugins;

import java.util.Map;

import org.hibernate.infra.develocity.GoalMetadataProvider;
import org.hibernate.infra.develocity.SimpleConfiguredPlugin;
import org.hibernate.infra.develocity.scan.BuildScanMetadata;
import org.hibernate.infra.develocity.util.JavaVersions;

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

	private static void configureCompile(GoalMetadataProvider.Context context) {
		var metadata = context.metadata();
		metadata.inputs( inputs -> {
			dependsOnConfigurableJavaExecutable( inputs, context, "executable",
					context.configuration().getBoolean( "skip" ),
					JavaVersions::forJavacExecutable );
		} );
		BuildScanMetadata.addCompilerMetadata( context );
	}
}
