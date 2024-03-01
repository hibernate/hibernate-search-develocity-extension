/*
 *
 *  * Hibernate Search, full-text search for your domain model
 *  *
 *  * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 *  * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 *
 */
package org.hibernate.search.develocity.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BuildScanMetadataTest {

	@ParameterizedTest
	@CsvSource(textBlock = """
			elasticsearch:7.10,docker.io/elastic/elasticsearch:7.10.1
			elasticsearch:latest,docker.io/elastic/elasticsearch:latest
			elasticsearch:7.10,elasticsearch:7.10.1
			elasticsearch:latest,elasticsearch:latest
			elasticsearch:7-foo,docker.io/elastic/elasticsearch:7-foo
			""")
	void toShortImageRef(String expected, String ref) {
		assertEquals( expected, BuildScanMetadata.toShortImageRef( ref ) );
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			17,'javac 17.0.7
			openjdk version "17.0.7" 2023-04-18
			OpenJDK Runtime Environment Temurin-17.0.7+7 (build 17.0.7+7)
			OpenJDK 64-Bit Server VM Temurin-17.0.7+7 (build 17.0.7+7, mixed mode, sharing)'
			11,'javac 17.0.7
			openjdk version "11.0.7" 2023-04-18
			OpenJDK Runtime Environment Temurin-17.0.7+7 (build 17.0.7+7)
			OpenJDK 64-Bit Server VM Temurin-17.0.7+7 (build 17.0.7+7, mixed mode, sharing)'
			17,'openjdk version "17.0.7" 2023-04-18
			OpenJDK Runtime Environment Temurin-17.0.7+7 (build 17.0.7+7)
			OpenJDK 64-Bit Server VM Temurin-17.0.7+7 (build 17.0.7+7, mixed mode, sharing)'
			11,'openjdk version "11.0.7" 2023-04-18
			OpenJDK Runtime Environment Temurin-17.0.7+7 (build 17.0.7+7)
			OpenJDK 64-Bit Server VM Temurin-17.0.7+7 (build 17.0.7+7, mixed mode, sharing)'
			17,17.0.7
			11,11.0.7
			17,foobar 17.0 something
			""")
	void toJdkMajor(String expected, String fullVersionText) {
		assertEquals( expected, BuildScanMetadata.toJdkMajor( fullVersionText ) );
	}

}