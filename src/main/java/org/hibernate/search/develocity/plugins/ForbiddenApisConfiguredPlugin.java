package org.hibernate.search.develocity.plugins;

import java.io.File;
import java.util.Map;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.SimpleConfiguredPlugin;

import com.gradle.develocity.agent.maven.api.cache.MojoMetadataProvider.Context.FileSet.NormalizationStrategy;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class ForbiddenApisConfiguredPlugin extends SimpleConfiguredPlugin {

    @Override
    protected String getPluginName() {
        return "forbiddenapis";
    }

    @Override
    protected Map<String, GoalMetadataProvider> getGoalMetadataProviders() {
        return Map.of(
                "check", ForbiddenApisConfiguredPlugin::configureCheck,
                "testCheck", ForbiddenApisConfiguredPlugin::configureTestCheck);
    }

    private static void configureCheck(GoalMetadataProvider.Context context) {
        var metadata = context.metadata();

        metadata.inputs(inputs -> {
            dependsOnGav(inputs, context);
            inputs.properties("signatures", "bundledSignatures", "failOnUnsupportedJava", "failOnMissingClasses",
                    "failOnUnresolvableSignatures", "ignoreSignaturesOfMissingClasses",
                    "failOnViolation", "disableClassloadingCache", "targetVersion", "releaseVersion", "includes", "excludes",
                    "suppressAnnotations", "skip", "packaging");

            inputs.fileSet("signaturesFiles", fs -> fs.normalizationStrategy(NormalizationStrategy.RELATIVE_PATH));
            inputs.fileSet("classpathElements", fs -> fs.normalizationStrategy(NormalizationStrategy.COMPILE_CLASSPATH));
            inputs.fileSet("classesDirectory", fs -> fs.normalizationStrategy(NormalizationStrategy.COMPILE_CLASSPATH));

            // for now, we push only one signature artifacts but we could explore the config if it was strictly necessary
            File hibernateSearchBuildConfigArtifactFile = resolveHibernateSearchBuildConfigArtifact(context);

            inputs.fileSet("hibernateSearchBuildConfig", hibernateSearchBuildConfigArtifactFile,
                    fs -> fs.normalizationStrategy(NormalizationStrategy.CLASSPATH));

            inputs.ignore("signaturesArtifacts", "projectRepos", "repoSession");
        });

        metadata.outputs(outputs -> {
            outputs.cacheable("If the inputs and signatures are identical, we should have the same output");
        });
    }

    private static void configureTestCheck(GoalMetadataProvider.Context context) {
        var metadata = context.metadata();

        configureCheck(context);

        metadata.inputs(inputs -> {
            inputs.properties("testTargetVersion", "testReleaseVersion");
        });
    }

    private static File resolveHibernateSearchBuildConfigArtifact(GoalMetadataProvider.Context context) {
        Artifact hibernateSearchBuildConfigArtifact = new DefaultArtifact("org.hibernate.search",
                "hibernate-search-build-config", "jar",
                context.metadata().getProject().getVersion());
        File hibernateSearchBuildConfigArtifactFile = context.metadata().getSession().getRepositorySession().getWorkspaceReader()
                .findArtifact(hibernateSearchBuildConfigArtifact);
        if (hibernateSearchBuildConfigArtifactFile == null) {
            hibernateSearchBuildConfigArtifactFile = new File(context.metadata().getSession().getRepositorySession()
                    .getLocalRepositoryManager().getPathForLocalArtifact(hibernateSearchBuildConfigArtifact));
        }
        return hibernateSearchBuildConfigArtifactFile;
    }
}
