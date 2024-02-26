package org.hibernate.search.develocity.scan;

import static org.hibernate.search.develocity.util.Strings.isBlank;

import com.gradle.maven.extension.api.scan.BuildScanApi;

public final class BuildScanMetadata {

    private BuildScanMetadata() {
    }

    public static void addMetadataToBuildScan(BuildScanApi buildScanApi) {
        // Add mvn command line
        final String mavenCommandLine = System.getenv("MAVEN_CMD_LINE_ARGS") != null ? "mvn " + System.getenv("MAVEN_CMD_LINE_ARGS") : "";
        if (!isBlank(mavenCommandLine)) {
            buildScanApi.value("Maven command line", "mvn " + mavenCommandLine);
        }

        buildScanApi.tag("hibernate-search");
    }
}
