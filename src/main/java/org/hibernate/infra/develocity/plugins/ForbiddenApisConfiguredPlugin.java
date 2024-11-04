package org.hibernate.infra.develocity.plugins;

import java.io.File;
import java.util.Map;

import org.hibernate.infra.develocity.GoalMetadataProvider;
import org.hibernate.infra.develocity.SimpleConfiguredPlugin;

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

            // for now, we push only one signature artifacts, but we could explore the config if it was strictly necessary
            File buildConfigArtifactFile = resolveBuildConfigArtifact(context);

            inputs.fileSet("buildConfigArtifact", buildConfigArtifactFile,
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

    private static File resolveBuildConfigArtifact(GoalMetadataProvider.Context context) {
        String groupId = context.metadata().getProject().getModel().getGroupId();
        if ("org.hibernate.search".equals(groupId)) {
            return resolveBuildConfigArtifact("org.hibernate.search", "hibernate-search-build-config", context);
        } else if ("org.hibernate.validator".equals(groupId)) {
            return resolveBuildConfigArtifact("org.hibernate.validator", "hibernate-validator-build-config", context);
        } else {
            throw new IllegalArgumentException("This project is not supported by the extension: %s:%s".formatted(groupId, context.metadata().getProject().getArtifactId()));
        }

    }

    private static File resolveBuildConfigArtifact(String groupId, String artifactId, GoalMetadataProvider.Context context) {
        Artifact hibernateSearchBuildConfigArtifact = new DefaultArtifact(groupId, artifactId, "jar",
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
