package org.hibernate.search.develocity;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.hibernate.search.develocity.normalization.Normalization;
import org.hibernate.search.develocity.plugins.EnforcerConfiguredPlugin;
import org.hibernate.search.develocity.plugins.ForbiddenApisConfiguredPlugin;
import org.hibernate.search.develocity.plugins.FormatterConfiguredPlugin;
import org.hibernate.search.develocity.plugins.ImpsortConfiguredPlugin;
import org.hibernate.search.develocity.plugins.SourceConfiguredPlugin;
import org.hibernate.search.develocity.scan.BuildScanMetadata;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.GradleEnterpriseListener;
import com.gradle.maven.scan.extension.internal.api.BuildScanApiInternal;

@SuppressWarnings("deprecation")
@Component(role = GradleEnterpriseListener.class, hint = "hibernate-search-build-cache", description = "Configures the build cache of the Hibernate Search project")
public class HibernateSearchProjectBuildCacheGradleEnterpriseListener implements GradleEnterpriseListener {


    @Override
    public void configure(GradleEnterpriseApi gradleEnterpriseApi, MavenSession mavenSession) throws Exception {
        gradleEnterpriseApi.getBuildScan().publishAlways();
        ((BuildScanApiInternal) gradleEnterpriseApi.getBuildScan()).publishIfAuthenticated();
        BuildScanMetadata.addMetadataToBuildScan(gradleEnterpriseApi.getBuildScan());

        Normalization.configureNormalization(gradleEnterpriseApi.getBuildCache());

        List<ConfiguredPlugin> configuredGoals = List.of(
                new EnforcerConfiguredPlugin(),
                new SourceConfiguredPlugin(),
                new FormatterConfiguredPlugin(),
                new ImpsortConfiguredPlugin(),
                new ForbiddenApisConfiguredPlugin()
        );

        for (ConfiguredPlugin configuredGoal : configuredGoals) {
            configuredGoal.configureBuildCache(gradleEnterpriseApi, mavenSession);
        }
    }
}
