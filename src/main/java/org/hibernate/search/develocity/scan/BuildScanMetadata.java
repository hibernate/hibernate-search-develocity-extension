package org.hibernate.search.develocity.scan;

import static org.hibernate.search.develocity.util.Strings.isBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.hibernate.search.develocity.Log;
import org.hibernate.search.develocity.util.JavaVersions;
import org.hibernate.search.develocity.util.Strings;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

public final class BuildScanMetadata {

	private static final Pattern DOCKERFILE_FROM_PATTERN = Pattern.compile( "FROM (.+)" );
	private static final Pattern CONTAINER_IMAGE_SHORT_PATTERN = Pattern.compile(
			"^(?:.*/)?([^/]+:[^-.]+(?:[-.][^-.]+)?).*$" );

	private BuildScanMetadata() {
	}

	public static void addMetadataToBuildScan(BuildScanApi buildScanApi, MavenSession mavenSession) {
		// Add mvn command line
		final String mavenCommandLine = System.getenv( "MAVEN_CMD_LINE_ARGS" ) != null ? "mvn " + System.getenv(
				"MAVEN_CMD_LINE_ARGS" ) : "";
		if ( !isBlank( mavenCommandLine ) ) {
			buildScanApi.value( "Maven command line", "mvn " + mavenCommandLine );
		}

		buildScanApi.tag( "hibernate-search" );

		for ( MavenProject project : mavenSession.getProjects() ) {
			tagIntegrations( buildScanApi, mavenSession, project );
		}

		recordExecutableVersion(
				buildScanApi, mavenSession, "java-version.main.compiler", JavaVersions::forJavacExecutable );
		recordExecutableVersion(
				buildScanApi, mavenSession, "java-version.test.compiler", JavaVersions::forJavacExecutable );
		recordExecutableVersion(
				buildScanApi, mavenSession, "java-version.test.launcher", JavaVersions::forJavaExecutable );
	}

	private static void tagIntegrations(BuildScanApi buildScanApi, MavenSession mavenSession, MavenProject project) {
		var dbKind = getStringProperty( project, "test.database.run.kind" );
		if ( !Strings.isBlank( dbKind ) ) {
			if ( dbKind.equals( "h2" ) ) {
				// H2 doesn't use containers
				buildScanApi.tag( "h2" );
			}
			else {
				tagDockerfileShortImageRef( buildScanApi, mavenSession,
						"database/%s.Dockerfile".formatted( dbKind ), null
				);
			}
		}
		if ( !getBooleanProperty( project, "test.lucene.skip" ) ) {
			buildScanApi.tag( "lucene" );
		}
		if ( !getBooleanProperty( project, "test.elasticsearch.skip" )
			 && getBooleanProperty( project, "test.elasticsearch.run.image.pull" ) ) {
			var distribution = getStringProperty( mavenSession, "test.elasticsearch.distribution" );
			tagDockerfileShortImageRef( buildScanApi, mavenSession,
					"search-backend/%s.Dockerfile".formatted( distribution ),
					getStringProperty( mavenSession, "test.elasticsearch.version" )
			);
		}
	}

	private static void tagDockerfileShortImageRef(BuildScanApi buildScanApi, MavenSession mavenSession,
			String dockerfileRelativePath, String versionOverride) {
		var path = Path.of( mavenSession.getExecutionRootDirectory(), "build/container", dockerfileRelativePath );
		try {
			String ref;
			try ( var stream = Files.lines( path ) ) {
				ref = stream.map( line -> {
							var matcher = DOCKERFILE_FROM_PATTERN.matcher( line );
							if ( matcher.matches() ) {
								return matcher.group( 1 ).trim();
							}
							else {
								return null;
							}
						} )
						.filter( Objects::nonNull )
						.findFirst()
						.orElseThrow();
			}
			if ( !Strings.isBlank( versionOverride ) ) {
				ref = ref.substring( 0, ref.lastIndexOf( ':' ) + 1 ) + versionOverride;
			}
			String shortImageRef = toShortImageRef( ref );
			buildScanApi.tag( shortImageRef );
			buildScanApi.value(
					shortImageRef.substring( 0, shortImageRef.lastIndexOf( ':' ) ),
					ref.substring( ref.lastIndexOf( ':' ) + 1 )
			);
		}
		catch (RuntimeException | IOException e) {
			Log.warn( "Unable to add tag from Dockerfile at %s: %s".formatted( path, e.getMessage() ) );
		}
	}

	static String toShortImageRef(String ref) {
		var matcher = CONTAINER_IMAGE_SHORT_PATTERN.matcher( ref );
		if ( matcher.matches() ) {
			return matcher.group( 1 );
		}
		else {
			return ref;
		}
	}

	private static void recordExecutableVersion(BuildScanApi buildScanApi, MavenSession mavenSession,
			String propertyName, Function<String, String> executableToVersion) {
		String javaExecutable = getStringProperty( mavenSession, propertyName );
		String javaVersion = executableToVersion.apply( javaExecutable );
		buildScanApi.value( propertyName, "Path: %s\nResolved version: %s".formatted( javaExecutable, javaVersion ) );
	}

	private static String getStringProperty(MavenSession mavenSession, String key) {
		return getStringProperty( mavenSession.getResult().getProject(), key );
	}

	private static String getStringProperty(MavenProject project, String key) {
		return (String) project.getProperties().get( key );
	}

	private static boolean getBooleanProperty(MavenProject project, String key) {
		return Boolean.parseBoolean( getStringProperty( project, key ) );
	}
}
