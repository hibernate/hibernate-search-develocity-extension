package org.hibernate.search.develocity.scan;

import static org.hibernate.search.develocity.util.Strings.isBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

import org.hibernate.search.develocity.GoalMetadataProvider;
import org.hibernate.search.develocity.Log;
import org.hibernate.search.develocity.util.JavaVersions;
import org.hibernate.search.develocity.util.MavenProperties;
import org.hibernate.search.develocity.util.Strings;

import com.gradle.maven.extension.api.scan.BuildScanApi;

public final class BuildScanMetadata {

	private static final Pattern DOCKERFILE_FROM_PATTERN = Pattern.compile( "FROM (.+)" );
	private static final Pattern CONTAINER_IMAGE_SHORT_PATTERN = Pattern.compile(
			"^(?:.*/)?([^/]+:[^-.]+(?:[-.][^-.]+)?).*$" );

	private BuildScanMetadata() {
	}

	public static void addMainMetadata(BuildScanApi buildScanApi) {
		// Add mvn command line
		final String mavenCommandLine = System.getenv( "MAVEN_CMD_LINE_ARGS" ) != null
				? "mvn " + System.getenv(
						"MAVEN_CMD_LINE_ARGS" )
				: "";
		if ( !isBlank( mavenCommandLine ) ) {
			buildScanApi.value( "Maven command line", mavenCommandLine );
		}

		buildScanApi.tag( "hibernate-search" );
	}

	public static void addJavaExecutableVersion(GoalMetadataProvider.Context context,
			String javaExecutable, String javaVersion,
			boolean canCacheExactVersion) {
		context.buildScanDeduplicatedValue( MavenProperties.BUILD_CACHE_JAVA_VERSION_EXACT,
				String.valueOf( canCacheExactVersion ) );
		var buildScanApi = context.buildScan();
		String plugin = context.metadata().getMojoExecution().getArtifactId();
		if ( plugin.equals( "maven-surefire-plugin" ) || plugin.equals( "maven-failsafe-plugin" ) ) {
			buildScanApi.tag( "jdk-%s".formatted( JavaVersions.toJdkMajor( javaVersion, "unknown" ) ) );
		}

		String goal = context.metadata().getMojoExecution().getGoal();
		context.buildScanDeduplicatedValue( "%s.%s.jdk".formatted( plugin, goal ), "Path: %s\nResolved version: %s".formatted( javaExecutable, javaVersion ) );
	}

	public static void addFailsafeMetadata(GoalMetadataProvider.Context context) {
		var buildScanApi = context.buildScan();

		var project = context.metadata().getProject();
		boolean dependsOnOrm = false;
		boolean dependsOnLucene = false;
		boolean dependsOnElasticsearch = false;
		var artifactFilter = context.configuration().getFailsafeClasspathFilter();
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
			var dbKind = context.properties().getString( "test.database.run.kind" );
			if ( !Strings.isBlank( dbKind ) ) {
				if ( dbKind.equals( "h2" ) ) {
					// H2 doesn't use containers
					buildScanApi.tag( "h2" );
				}
				else {
					addDockerfileShortImageRef( context,
							"database/%s.Dockerfile".formatted( dbKind ), null );
				}
			}
		}

		String explicitBackend = context.configuration()
				.getFailsafeSystemProperty( "org.hibernate.search.integrationtest.backend.type" );
		if ( Strings.isBlank( explicitBackend ) ) {
			explicitBackend = null;
		}

		if ( dependsOnLucene
				&& ( explicitBackend == null || "lucene".equals( explicitBackend ) )
				&& !context.properties().getBoolean( "test.lucene.skip" ) ) {
			buildScanApi.tag( "lucene" );
		}

		if ( dependsOnElasticsearch
				&& ( explicitBackend == null || "elasticsearch".equals( explicitBackend ) )
				&& !context.properties().getBoolean( "test.elasticsearch.skip" ) ) {
			var distribution = context.properties().getString( "test.elasticsearch.distribution" );
			addDockerfileShortImageRef( context,
					"search-backend/%s.Dockerfile".formatted( distribution ),
					context.properties().getString( "test.elasticsearch.version" ) );
		}
	}

	private static void addDockerfileShortImageRef(GoalMetadataProvider.Context context,
			String dockerfileRelativePath, String versionOverride) {
		var buildScanApi = context.buildScan();
		var path = Path.of( context.metadata().getSession().getExecutionRootDirectory(),
				"build/container", dockerfileRelativePath );
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

}
