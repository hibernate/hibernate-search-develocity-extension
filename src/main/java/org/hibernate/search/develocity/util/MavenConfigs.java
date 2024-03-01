/*
 *
 *  * Hibernate Search, full-text search for your domain model
 *  *
 *  * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 *  * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 *
 */
package org.hibernate.search.develocity.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.develocity.Log;

import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public final class MavenConfigs {

	public static final String BUILD_CACHE_JAVA_VERSION_EXACT = "build-cache.java-version.exact";

	private MavenConfigs() {
	}

	public static String getStringProperty(MavenSession mavenSession, String key) {
		return getStringProperty( mavenSession.getResult().getProject(), key );
	}

	public static String getStringProperty(MavenProject project, String key) {
		return (String) project.getProperties().get( key );
	}

	public static boolean getBooleanProperty(MavenProject project, String key) {
		return Boolean.parseBoolean( getStringProperty( project, key ) );
	}

	public static boolean getBooleanConfig(MojoExecution mojoExecution, String key) {
		return Boolean.parseBoolean( getStringConfig( mojoExecution, key ) );
	}

	public static String getStringConfig(MojoExecution mojoExecution, String key) {
		var configElement = mojoExecution.getConfiguration().getChild( key );
		if ( configElement == null ) {
			return null;
		}
		var value = configElement.getValue();
		if ( value == null ) {
			return null;
		}
		return value.trim();
	}

	public static List<String> getStringListConfig(MojoExecution mojoExecution, String key) {
		var configElement = mojoExecution.getConfiguration().getChild( key );
		if ( configElement == null ) {
			return List.of();
		}
		List<String> children = new ArrayList<>();
		for ( Xpp3Dom configElementChild : configElement.getChildren() ) {
			var value = configElementChild.getValue();
			if ( value != null ) {
				children.add( value );
			}
		}
		return children;
	}

	public static String getFailsafeSystemProperty(MojoExecution failsafeIntegrationTestExecution, String key) {
		var systemPropertyVariables = failsafeIntegrationTestExecution.getConfiguration()
				.getChild( "systemPropertyVariables" );
		if ( systemPropertyVariables == null ) {
			return null;
		}
		var child = systemPropertyVariables.getChild( key );
		if ( child == null ) {
			return null;
		}
		return child.getValue();
	}

	public static ArtifactFilter getFailsafeClasspathFilter(MojoExecution mojoExecution) {
		List<ArtifactFilter> filters = new ArrayList<>();
		try {
			String classpathDependencyScopeExclude = getStringConfig(
					mojoExecution, "classpathDependencyScopeExclude" );
			List<String> classpathDependencyExcludes = getStringListConfig(
					mojoExecution, "classpathDependencyExcludes" );
			if ( classpathDependencyScopeExclude != null && !classpathDependencyScopeExclude.isEmpty() ) {
				var scopeIncludeFilter = new ScopeArtifactFilter( classpathDependencyScopeExclude );
				filters.add( artifact -> !scopeIncludeFilter.include( artifact ) );
			}
			if ( classpathDependencyScopeExclude != null && !classpathDependencyScopeExclude.isEmpty() ) {
				filters.add( new PatternExcludesArtifactFilter( classpathDependencyExcludes ) );
			}
		}
		catch (RuntimeException e) {
			Log.warn( "Could not interpret Failsafe classpath filters: %s", e.getMessage() );
		}
		if ( filters.isEmpty() ) {
			return ignored -> true;
		}
		return new AndArtifactFilter( filters );
	}

	public static boolean cacheExactJavaVersion(MavenSession mavenSession) {
		return Boolean.parseBoolean( (String) mavenSession.getUserProperties().get( BUILD_CACHE_JAVA_VERSION_EXACT ) );
	}
}
