package org.hibernate.search.develocity.plugins;

import java.util.Map;

import org.hibernate.search.develocity.SimpleConfiguredPlugin;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider.Context.FileSet.EmptyDirectoryHandling;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider.Context.FileSet.NormalizationStrategy;

public class FormatterConfiguredPlugin extends SimpleConfiguredPlugin {

    @Override
    protected String getPluginName() {
        return "formatter-maven-plugin";
    }

    @Override
    protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
        return Map.of(
                "format", FormatterConfiguredPlugin::configureFormat);
    }

    private static void configureFormat(MojoMetadataProvider.Context context) {
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

        context.outputs(outputs -> {
            outputs.cacheable("If the inputs and dependencies are identical, we should have the same output");
        });
    }
}
