package org.hibernate.search.develocity.normalization;

import com.gradle.develocity.agent.maven.api.cache.BuildCacheApi;

public final class Normalization {

    private Normalization() {
    }

    public static void configureNormalization(BuildCacheApi buildCacheApi) {
        // System properties
        buildCacheApi.registerNormalizationProvider(
                context -> context.configureSystemPropertiesNormalization(s -> {
                    s.addIgnoredKeys("maven.repo.local", "maven.settings");
                }));

        // Application.properties
        buildCacheApi.registerNormalizationProvider(
                context -> context.configureRuntimeClasspathNormalization(c -> {
                }));
    }
}
