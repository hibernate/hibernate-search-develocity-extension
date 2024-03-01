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
import org.hibernate.search.develocity.util.MavenConfigs;
import org.hibernate.search.develocity.util.Strings;

import com.gradle.maven.extension.api.scan.BuildScanApi;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

public final class BuildScanMetadata {

	private static final Pattern DOCKERFILE_FROM_PATTERN = Pattern.compile( "FROM (.+)" );
	private static final Pattern CONTAINER_IMAGE_SHORT_PATTERN = Pattern.compile(
			"^(?:.*/)?([^/]+:[^-.]+(?:[-.][^-.]+)?).*$" );

	private BuildScanMetadata() {
	}

	public static void addMetadataToBuildScan(BuildScanApi buildScanApi, MavenSession mavenSession) {
		// Add mvn command line
		final String mavenCommandLine = System.getenv( "MAVEN_CMD_LINE_ARGS" ) != null
				? "mvn " + System.getenv(
						"MAVEN_CMD_LINE_ARGS" )
				: "";
		if ( !isBlank( mavenCommandLine ) ) {
			buildScanApi.value( "Maven command line", "mvn " + mavenCommandLine );
		}

		buildScanApi.tag( "hibernate-search" );

		buildScanApi.value( MavenConfigs.BUILD_CACHE_JAVA_VERSION_EXACT,
				String.valueOf( MavenConfigs.cacheExactJavaVersion( mavenSession ) ) );

		recordExecutableVersion( buildScanApi, mavenSession, "java-version.main.compiler", false,
				JavaVersions::forJavacExecutable );
		recordExecutableVersion( buildScanApi, mavenSession, "java-version.test.compiler", false,
				JavaVersions::forJavacExecutable );
		recordExecutableVersion( buildScanApi, mavenSession, "java-version.test.launcher", true,
				JavaVersions::forJavaExecutable );
	}

	public static void addFailsafeMetadataToBuildScan(BuildScanApi buildScanApi, MavenSession mavenSession,
			MojoExecution mojoExecution) {
		var project = mavenSession.getCurrentProject();
		boolean dependsOnOrm = false;
		boolean dependsOnLucene = false;
		boolean dependsOnElasticsearch = false;
		var artifactFilter = MavenConfigs.getFailsafeClasspathFilter( mojoExecution );
		for ( var dependency : project.getArtifacts() ) {
			if ( !artifactFilter.include( dependency ) ) {
				continue;
			}
			var artifactId = dependency.getArtifactId();
			dependsOnOrm = dependsOnOrm || artifactId.contains( "mapper-orm" );
			dependsOnLucene = dependsOnLucene || artifactId.contains( "backend-lucene" );
			dependsOnElasticsearch = dependsOnElasticsearch || artifactId.contains( "backend-elasticsearch" );
		}

		if ( dependsOnOrm ) {
			var dbKind = MavenConfigs.getStringProperty( mavenSession, "test.database.run.kind" );
			if ( !Strings.isBlank( dbKind ) ) {
				if ( dbKind.equals( "h2" ) ) {
					// H2 doesn't use containers
					buildScanApi.tag( "h2" );
				}
				else {
					tagDockerfileShortImageRef( buildScanApi, mavenSession,
							"database/%s.Dockerfile".formatted( dbKind ), null );
				}
			}
		}

		String explicitBackend =
				MavenConfigs.getFailsafeSystemProperty( mojoExecution, "org.hibernate.search.integrationtest.backend.type" );
		if ( Strings.isBlank( explicitBackend ) ) {
			explicitBackend = null;
		}

		if ( dependsOnLucene
				&& ( explicitBackend == null || "lucene".equals( explicitBackend ) )
				&& !MavenConfigs.getBooleanProperty( project, "test.lucene.skip" ) ) {
			buildScanApi.tag( "lucene" );
		}

		if ( dependsOnElasticsearch
				&& ( explicitBackend == null || "elasticsearch".equals( explicitBackend ) )
				&& !MavenConfigs.getBooleanProperty( project, "test.elasticsearch.skip" ) ) {
			var distribution = MavenConfigs.getStringProperty( mavenSession, "test.elasticsearch.distribution" );
			tagDockerfileShortImageRef( buildScanApi, mavenSession,
					"search-backend/%s.Dockerfile".formatted( distribution ),
					MavenConfigs.getStringProperty( mavenSession, "test.elasticsearch.version" ) );
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
			buildScanApi.tag( shortImageRef.replace( ':', '-' ) );
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
			String propertyName, boolean tag, Function<String, String> executableToVersion) {
		String javaExecutable = MavenConfigs.getStringProperty( mavenSession, propertyName );
		String javaVersion = executableToVersion.apply( javaExecutable );
		if ( tag ) {
			buildScanApi.tag( "jdk-%s".formatted( JavaVersions.toJdkMajor( javaVersion, "unknown" ) ) );
		}
		buildScanApi.value( propertyName, "Path: %s\nResolved version: %s".formatted( javaExecutable, javaVersion ) );
	}

}
