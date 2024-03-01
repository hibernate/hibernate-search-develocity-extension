package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.SimpleConfiguredPlugin;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider.Context.FileSet.EmptyDirectoryHandling;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider.Context.FileSet.NormalizationStrategy;
import com.gradle.maven.extension.api.scan.BuildScanApi;

public class FormatterConfiguredPlugin extends SimpleConfiguredPlugin {

    @Override
    protected String getPluginName() {
        return "formatter-maven-plugin";
    }

    @Override
    protected Map<String, GoalMetadataProvider> getGoalMetadataProviders(BuildScanApi buildScanApi) {
        return Map.of(
                "format", FormatterConfiguredPlugin::configureFormat,
                "validate", FormatterConfiguredPlugin::configureValidate);
    }

    // This is for FormatterMojo, extended by ValidateMojo
    // See https://github.com/revelc/formatter-maven-plugin/tree/main/src/main/java/net/revelc/code/formatter
    private static void configureCommon(MojoMetadataProvider.Context context) {
        context.inputs(inputs -> {
            dependsOnGav(inputs, context);

            inputs.properties("includes", "excludes", "compilerSource", "compilerCompliance", "compilerTargetPlatform", "lineEnding", "configFile",
                    "configJsFile", "configHtmlFile", "configXmlFile", "configJsonFile", "configCssFile", "skipFormattingCache",
                    "skipJavaFormatting", "skipJsFormatting", "skipHtmlFormatting", "skipXmlFormatting", "skipJsonFormatting",
                    "skipCssFormatting", "skipFormatting", "useEclipseDefaults", "javaExclusionPattern", "removeTrailingWhitespace", "includeResources");

            inputs.fileSet("sourceDirectory", fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH)
                    .emptyDirectoryHandling(EmptyDirectoryHandling.IGNORE));
            inputs.fileSet("testSourceDirectory", fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH)
                    .emptyDirectoryHandling(EmptyDirectoryHandling.IGNORE));
            inputs.fileSet("directories", fileSet -> fileSet.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH)
                    .emptyDirectoryHandling(EmptyDirectoryHandling.IGNORE));

            inputs.ignore("project", "targetDirectory", "basedir", "cachedir");
        });

        context.nested("encoding", c -> c.inputs(inputs -> inputs.properties("displayName")));
    }

    private static void configureFormat(MojoMetadataProvider.Context context) {
        configureCommon(context);

        context.outputs(outputs -> {
            outputs.cacheable("If the inputs and dependencies are identical, we should have the same output");
        });
    }

    private static void configureValidate(MojoMetadataProvider.Context context) {
        configureCommon(context);

		context.inputs( inputs -> {
			inputs.properties( "aggregator", "executionRoot");
			// We already depend on the GAV.
			inputs.ignore( "mojoGroupId", "mojoArtifactId", "mojoVersion" );
			// This is only used to log the format command on failure
			inputs.ignore( "mavenProject", "mavenSession" );
		} );

        context.outputs(outputs -> {
            outputs.cacheable("If the inputs and dependencies are identical, validation should give the same result (success/failure)");
        });
    }
}
