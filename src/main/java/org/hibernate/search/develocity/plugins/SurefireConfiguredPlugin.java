package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.SimpleConfiguredPlugin;
import org.hibernate.search.develocity.util.JavaVersions;

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
