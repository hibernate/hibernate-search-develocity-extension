package org.hibernate.search.develocity.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.develocity.Log;

import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public final class MavenMojoExecutionConfig {
	private final MojoExecution mojoExecution;

	public MavenMojoExecutionConfig(MojoExecution mojoExecution) {
		this.mojoExecution = mojoExecution;
	}

	public Boolean getBoolean(String key) {
		return Boolean.parseBoolean( getString( key ) );
	}

	public String getString(String key) {
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

	public List<String> getStringList(String key) {
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

	public Map<String, String> getStringMap(String key) {
		var configElement = mojoExecution.getConfiguration()
				.getChild( key );
		if ( configElement == null ) {
			return Map.of();
		}
		Map<String, String> result = new LinkedHashMap<>();
		for ( Xpp3Dom configElementChild : configElement.getChildren() ) {
			result.put( configElementChild.getName(), configElementChild.getValue() );
		}
		return result;
	}

	public String getFailsafeSystemProperty(String key) {
		var systemPropertyVariables = mojoExecution.getConfiguration()
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

	public ArtifactFilter getFailsafeClasspathFilter() {
		List<ArtifactFilter> filters = new ArrayList<>();
		try {
			String classpathDependencyScopeExclude = getString( "classpathDependencyScopeExclude" );
			List<String> classpathDependencyExcludes = getStringList( "classpathDependencyExcludes" );
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
}
