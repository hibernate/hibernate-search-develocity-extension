package org.hibernate.infra.develocity.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class JavaVersions {
	private static final Pattern JDK_VERSION_MAJOR_PATTERN = Pattern.compile(
			"^.*version \"(\\d+).*$", Pattern.DOTALL );
	private static final Pattern JDK_VERSION_MAJOR_FALLBACK_PATTERN = Pattern.compile( "(\\d+)\\." );

	private JavaVersions() {
	}

	private static final Map<String, String> versionByExecutablePath = new ConcurrentHashMap<>();

	public static String forJavaExecutable(String javaPath) {
		if ( javaPath == null || javaPath.isBlank() ) {
			return Runtime.version().toString();
		}
		return JavaVersions.forExecutable( javaPath );
	}

	public static String forJavacExecutable(String javacPath) {
		if ( javacPath == null || javacPath.isBlank() ) {
			return Runtime.version().toString();
		}
		return JavaVersions.forExecutable( javacPath )
			   // This is necessary even if we have the version from javac above,
			   // because javac -version only prints the major
			   // and in particular doesn't print the EA/build number version for Early Access releases
			   + JavaVersions.forExecutable( javacPath.replaceAll( "/javac$", "/java" ) );
	}

	private static String forExecutable(String executablePath) {
		// Getting the version is slow, so we cache it
		return versionByExecutablePath.computeIfAbsent( executablePath, path -> {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command( path, "-version" );
			// If this is set, `java` will display it on startup and this will mess up the output.
			builder.environment().remove("JAVA_TOOL_OPTIONS");
			Process process;
			try {
				process = builder.start();
				int exitCode = process.waitFor();
				if ( exitCode != 0 ) {
					throw new IllegalStateException( executablePath + " exited with code " + exitCode );
				}
				// javac outputs the version on stdout
				return new String( process.getInputStream().readAllBytes(), StandardCharsets.UTF_8 )
					   // java outputs the version on stderr
					   + new String( process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8 );
			}
			catch (IOException | InterruptedException | RuntimeException e) {
				throw new IllegalStateException(
						"Cannot guess java version for " + executablePath, e );
			}
		} );
	}

	public static String toJdkMajor(String fullVersionText, String defaultValue) {
		var matcher = JDK_VERSION_MAJOR_PATTERN.matcher( fullVersionText );
		if ( matcher.matches() ) {
			return matcher.group( 1 );
		}
		// As a fallback, try to match a simple version string
		// such as the one coming from Runtime.version().toString()
		matcher = JDK_VERSION_MAJOR_FALLBACK_PATTERN.matcher( fullVersionText );
		if ( matcher.find() ) {
			return matcher.group( 1 );
		}
		return defaultValue;
	}
}
