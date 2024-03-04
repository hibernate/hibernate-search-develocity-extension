package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.SimpleConfiguredPlugin;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider.Context.FileSet.EmptyDirectoryHandling;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider.Context.FileSet.NormalizationStrategy;

public class ImpsortConfiguredPlugin extends SimpleConfiguredPlugin {

    @Override
    protected String getPluginName() {
        return "impsort-maven-plugin";
    }

    @Override
    protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
        return Map.of(
                "sort", ImpsortConfiguredPlugin::configureSort,
                "check", ImpsortConfiguredPlugin::configureValidate);
    }

    // This is for AbstractImpSortMojo, extended by CheckMojo and SortMojo
    // See https://github.com/revelc/impsort-maven-plugin/tree/main/src/main/java/net/revelc/code/impsort/maven/plugin
    private static void configureCommon(GoalMetadataProvider.Context context) {
        var metadata = context.metadata();

        metadata.inputs(inputs -> {
            dependsOnGav(inputs, context);

            inputs.properties("sourceEncoding", "skip", "staticGroups", "groups", "staticAfter", "joinStaticWithNonStatic",
                    "includes", "excludes", "removeUnused", "treatSamePackageAsUnused", "breadthFirstComparator",
                    "lineEnding", "compliance");

            inputs.fileSet("sourceDirectory", fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH)
                    .emptyDirectoryHandling(EmptyDirectoryHandling.IGNORE));
            inputs.fileSet("testSourceDirectory", fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH)
                    .emptyDirectoryHandling(EmptyDirectoryHandling.IGNORE));
            inputs.fileSet("directories", fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH)
                    .emptyDirectoryHandling(EmptyDirectoryHandling.IGNORE));

            inputs.ignore("project", "plugin",
                    // For now, we need to ignore the cachedir until we can declare it as an output. See below.
                    "cachedir");
        });

        metadata.outputs(outputs -> {
            // For now we don't want to output the cachedir as it contains absolute paths
            // See https://github.com/revelc/impsort-maven-plugin/pull/87
            //outputs.directory("cachedir");
        });
    }

	private static void configureSort(GoalMetadataProvider.Context context) {
		var metadata = context.metadata();

		configureCommon( context );

		metadata.outputs( outputs -> {
			outputs.cacheable( "If the inputs and dependencies are identical, we should have the same output" );
		} );
	}

	private static void configureValidate(GoalMetadataProvider.Context context) {
		var metadata = context.metadata();

		configureCommon( context );

		metadata.outputs( outputs -> {
			outputs.cacheable( "If the inputs and dependencies are identical, we should have the same output" );
		} );
	}
}
