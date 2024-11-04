package org.hibernate.infra.develocity.util;

import java.nio.file.Path;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import com.gradle.develocity.agent.maven.api.cache.MojoMetadataProvider;
import com.gradle.develocity.agent.maven.api.cache.NormalizationProvider;

public final class Matchers {

    private Matchers() {
    }

    public static boolean directory(MojoMetadataProvider.Context context, Path directory) {
        return directory(context.getSession(), context.getProject(), directory);
    }

    public static boolean module(MojoMetadataProvider.Context context, String artifactId) {
        return module(context.getProject(), artifactId);
    }

    public static boolean module(MojoMetadataProvider.Context context, String groupId, String artifactId) {
        return module(context.getProject(), groupId, artifactId);
    }

    public static boolean directory(NormalizationProvider.Context context, Path directory) {
        return directory(context.getSession(), context.getProject(), directory);
    }

    public static boolean module(NormalizationProvider.Context context, String artifactId) {
        return module(context.getProject(), artifactId);
    }

    public static boolean module(NormalizationProvider.Context context, String groupId, String artifactId) {
        return module(context.getProject(), groupId, artifactId);
    }

    private static boolean directory(MavenSession session, MavenProject project, Path directory) {
        if (project == null || project.getBasedir() == null) {
            return false;
        }

        Path rootProject = session.getRequest().getMultiModuleProjectDirectory().toPath();
        Path currentProject = project.getBasedir().toPath();

        Path relativePath = rootProject.relativize(currentProject);

        return relativePath.startsWith(directory);
    }

    private static boolean module(MavenProject project, String artifactId) {
        return module(project, "org.hibernate.search", artifactId);
    }

    private static boolean module(MavenProject project, String groupId, String artifactId) {
        return project.getGroupId().equals(groupId) &&
                project.getArtifactId().equals(artifactId);
    }
}
