package org.hibernate.search.develocity.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JavaVersions {
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
}
