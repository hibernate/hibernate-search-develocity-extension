package org.hibernate.infra.develocity.scan;

import static org.hibernate.infra.develocity.util.Strings.isBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.hibernate.infra.develocity.GoalMetadataProvider;
import org.hibernate.infra.develocity.Log;
import org.hibernate.infra.develocity.util.JavaVersions;
import org.hibernate.infra.develocity.util.MavenProperties;
import org.hibernate.infra.develocity.util.Strings;

import com.gradle.develocity.agent.maven.api.scan.BuildScanApi;

public final class BuildScanMetadata {

	private static final Pattern DOCKERFILE_FROM_PATTERN = Pattern.compile( "FROM (.+)" );
	private static final Pattern CONTAINER_IMAGE_SHORT_PATTERN = Pattern.compile(
			"^(?:.*/)?([^/]+:[^-.]+(?:[-.][^-.]+)?).*$" );

	private BuildScanMetadata() {
	}

	public static void addMainMetadata(BuildScanApi buildScanApi, MavenSession mavenSession) {
		// Add mvn command line
		final String mavenCommandLine = System.getenv( "MAVEN_CMD_LINE_ARGS" ) != null
				? "mvn " + System.getenv(
						"MAVEN_CMD_LINE_ARGS" )
				: "";
		if ( !isBlank( mavenCommandLine ) ) {
			buildScanApi.value( "Maven command line", mavenCommandLine );
		}
		buildScanApi.tag(mavenSession.getTopLevelProject().getGroupId());
	}


	public static void addCompilerMetadata(GoalMetadataProvider.Context context) {
		var buildScanApi = context.buildScan();
		String compilerId = context.configuration().getString( "compilerId" );
		if ( !Strings.isBlank( compilerId )
				// Yes it's weird but it can happen.
				&& !"${maven.compiler.compilerId}".equals( compilerId ) ) {
			buildScanApi.tag( "compiler-%s".formatted( compilerId ) );
		}
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
		context.buildScanDeduplicatedValue( "%s.%s.jdk".formatted( plugin, goal ),
				"Path: %s\nResolved version: %s".formatted( javaExecutable, javaVersion ) );
	}

	public static void addFailsafeMetadata(GoalMetadataProvider.Context context) {
		var buildScanApi = context.buildScan();

		var project = context.metadata().getProject();
		if ("org.hibernate.search".equals(project.getGroupId())) {
			boolean dependsOnOrm = false;
			boolean dependsOnLucene = false;
			boolean dependsOnElasticsearch = false;
			var artifactFilter = context.configuration().getFailsafeClasspathFilter();
			for (var dependency : project.getArtifacts()) {
				if (!artifactFilter.include(dependency)) {
					continue;
				}
				var artifactId = dependency.getArtifactId();
				dependsOnOrm = dependsOnOrm || artifactId.contains("mapper-orm");
				dependsOnLucene = dependsOnLucene || artifactId.contains("backend-lucene");
				dependsOnElasticsearch = dependsOnElasticsearch || artifactId.contains("backend-elasticsearch");
			}

			if (dependsOnOrm) {
				var dbKind = context.properties().getString("test.database.run.kind");
				if (!Strings.isBlank(dbKind)) {
					if (dbKind.equals("h2")) {
						// H2 doesn't use containers
						buildScanApi.tag("h2");
					} else {
						addDockerfileMetadata(context,
								"database/%s.Dockerfile".formatted(dbKind), dbKind, null, false);
					}
				}
			}

			String explicitBackend = context.configuration()
					.getFailsafeSystemProperty("org.hibernate.search.integrationtest.backend.type");
			if (Strings.isBlank(explicitBackend)) {
				explicitBackend = null;
			}

			if (dependsOnLucene
					&& (explicitBackend == null || "lucene".equals(explicitBackend))
					&& !context.properties().getBoolean("test.lucene.skip")) {
				buildScanApi.tag("lucene");
			}

			if (dependsOnElasticsearch
					&& (explicitBackend == null || "elasticsearch".equals(explicitBackend))
					&& !context.properties().getBoolean("test.elasticsearch.skip")) {
				var distribution = context.properties().getString("test.elasticsearch.distribution");
				addDockerfileMetadata(context,
						"search-backend/%s.Dockerfile".formatted(distribution),
						"elastic".equals(distribution) ? "elasticsearch" : distribution,
						context.properties().getString("test.elasticsearch.version"), true);
			}
		} else if (!"org.hibernate.validator".equals(project.getGroupId())) {
			Log.warn("Project %s:%s is unknown, no specific Failsafe metadata added.".formatted(project.getGroupId(), project.getArtifactId()));
		}
	}

	public static void addSurefireMetadata(GoalMetadataProvider.Context context) {
		var buildScanApi = context.buildScan();
		var project = context.metadata().getProject();
		if ("org.hibernate.validator".equals(project.getGroupId())) {
			if ("hibernate-validator-tck-runner".equals(project.getArtifactId())) {
				project.getActiveProfiles().stream().map(Profile::getId)
						.filter("local"::equals).findAny()
						.ifPresent(id -> buildScanApi.tag("Standalone-TCK"));

				project.getActiveProfiles().stream().map(Profile::getId)
						.filter(id -> id.startsWith("incontainer")).findAny()
						.ifPresent(id -> buildScanApi.tag("In-Container-TCK"));

				project.getArtifacts().stream()
						.filter(a -> "validation-tck-tests".equals(a.getArtifactId()))
						.findAny()
						.ifPresent(a -> buildScanApi.tag("Validation-TCK-%s".formatted(a.getVersion())));
			}
		} else if (!"org.hibernate.search".equals(project.getGroupId())) {
			Log.warn("Project %s:%s is unknown, no specific Surefire metadata added.".formatted(project.getGroupId(), project.getArtifactId()));
		}
	}

	private static void addDockerfileMetadata(GoalMetadataProvider.Context context,
			String dockerfileRelativePath, String tag, String versionOverride, boolean tagVersion) {
		var buildScanApi = context.buildScan();
		var path = Path.of( context.metadata().getSession().getExecutionRootDirectory(),
				"build/container", dockerfileRelativePath );
		if ( tag != null ) {
			// Tag without version
			buildScanApi.tag( tag );
		}
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
			context.buildScanDeduplicatedValue( "Container", ref );

			// Tag with version
			if ( tag != null && tagVersion ) {
				String shortImageRef = toShortImageRef( ref );
				int colonIndex = shortImageRef.indexOf( ':' );
				if ( colonIndex >= 0 ) {
					buildScanApi.tag( tag + "-" + shortImageRef.substring( colonIndex + 1 ) );
				}
			}
		}
		catch (RuntimeException | IOException e) {
			Log.warn( "Unable to add metadata from Dockerfile at %s: %s".formatted( path, e.getMessage() ) );
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
