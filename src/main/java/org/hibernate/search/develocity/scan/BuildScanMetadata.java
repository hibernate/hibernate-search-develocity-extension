package org.hibernate.search.develocity.scan;

import static org.hibernate.search.develocity.util.Strings.isBlank;

import java.util.function.Function;

import org.hibernate.search.develocity.Log;
import org.hibernate.search.develocity.util.JavaVersions;

import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.execution.MavenSession;

public final class BuildScanMetadata {

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

		recordExecutableVersion( buildScanApi, mavenSession, "java-version.main.compiler", JavaVersions::forJavacExecutable );
		recordExecutableVersion( buildScanApi, mavenSession, "java-version.test.compiler", JavaVersions::forJavacExecutable );
		recordExecutableVersion( buildScanApi, mavenSession, "java-version.test.launcher", JavaVersions::forJavaExecutable );
	}

	private static void recordExecutableVersion(BuildScanApi buildScanApi, MavenSession mavenSession,
			String propertyName, Function<String, String> executableToVersion) {
		String javaExecutable = (String) mavenSession.getResult().getProject().getProperties().get( propertyName );
		String javaVersion = executableToVersion.apply( javaExecutable );
		buildScanApi.value( propertyName, "Path: %s\nResolved version: %s".formatted( javaExecutable, javaVersion ) );
	}
}
