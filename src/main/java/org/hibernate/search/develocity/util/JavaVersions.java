package org.hibernate.search.develocity.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JavaVersions {
	private JavaVersions() {
	}

	private static final Map<String, String> versionByExecutablePath = new ConcurrentHashMap<>();

	public static String forExecutable(String executablePath) {
		// Getting the version is slow, so we cache it
		return versionByExecutablePath.computeIfAbsent( executablePath, path -> {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command( path, "-version" );
			Process process;
			try {
				process = builder.start();
				int exitCode = process.waitFor();
				if ( exitCode != 0 ) {
					throw new IllegalStateException( executablePath + " exited with code " + exitCode );
				}
				return new String( process.getInputStream().readAllBytes(), StandardCharsets.UTF_8 );
			}
			catch (IOException | InterruptedException | RuntimeException e) {
				throw new IllegalStateException(
						"Cannot guess java version for " + executablePath, e );
			}
		} );
	}
}
