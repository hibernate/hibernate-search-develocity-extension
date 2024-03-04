package org.hibernate.search.develocity.normalization;

import com.gradle.maven.extension.api.cache.BuildCacheApi;

public final class Normalization {

    private Normalization() {
    }

    public static void configureNormalization(BuildCacheApi buildCacheApi) {
		buildCacheApi.registerNormalizationProvider(
				context -> context.configureSystemPropertiesNormalization( s -> {
					s.addIgnoredKeys( "maven.repo.local", "maven.settings" );
				} ) );

		buildCacheApi.registerNormalizationProvider(
				context -> context.configureRuntimeClasspathNormalization( c -> {
					// Lucene indexes
					c.addIgnoredFiles( "**/target/**/test-indexes" );
				} ) );
    }
}
