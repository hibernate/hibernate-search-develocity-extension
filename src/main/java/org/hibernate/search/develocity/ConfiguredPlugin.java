package org.hibernate.search.develocity;

import org.apache.maven.execution.MavenSession;

import com.gradle.maven.extension.api.GradleEnterpriseApi;

public interface ConfiguredPlugin {

    void configureBuildCache(GradleEnterpriseApi gradleEnterpriseApi, MavenSession mavenSession);

}
