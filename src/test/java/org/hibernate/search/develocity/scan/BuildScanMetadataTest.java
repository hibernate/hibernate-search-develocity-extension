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

import org.hibernate.search.develocity.util.JavaVersions;

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

}