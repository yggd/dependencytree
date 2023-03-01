package org.yggd.dependencytree.model

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import org.junit.jupiter.api.Test

import org.assertj.core.api.Assertions.assertThat
import org.yggd.dependencytree.reader.DependencyTree
import java.io.File

class PomTest {

    @Test
    fun pomTest() {
        val pomFile = File("testpom/terabatch541.xml")
        val pom = Pom.fromFile(pomFile)
        assertThat(pom.artifactId).isEqualTo("terabatch541")

        val dependencies = DependencyTree.traverseDependency(pom)
        assertThat(dependencies.size).isEqualTo(6)
    }

    /**
     * Scope evaluation of transitive dependence.
     * <p>
     * Transitive dependencies follow the following scope matrix.
     * If a dependency is set to the scope in the left column, a transitive dependency of that dependency
     * with the scope across the top row results in a dependency in the main project with the scope listed at
     * the intersection. If no scope is listed, it means the dependency is omitted.
     * <p>
     * <table>
     *   <tr> <td>        </td> <td>compile </td> <td>provided</td> <td>runtime </td> <td>test</td> </tr>
     *   <tr> <td>compile </td> <td>compile </td> <td>-       </td> <td>runtime </td> <td>-   </td> </tr>
     *   <tr> <td>provided</td> <td>provided</td> <td>-       </td> <td>provided</td> <td>-   </td> </tr>
     *   <tr> <td>runtime </td> <td>runtime </td> <td>-       </td> <td>runtime </td> <td>-   </td> </tr>
     *   <tr> <td>test    </td> <td>test    </td> <td>-       </td> <td>test    </td> <td>-   </td> </tr>
     * </table>
     *
     * cf) https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope
     */
    @Test
    fun transitiveDependenceMatrix() {
        val dependencyTree = DependencyTree()
        assertThat(dependencyTree.evaluateTransitiveDependency("compile" , "compile")).isEqualTo("compile")
        assertThat(dependencyTree.evaluateTransitiveDependency("compile" , "provided")).isEqualTo(null)
        assertThat(dependencyTree.evaluateTransitiveDependency("compile" , "runtime")).isEqualTo("runtime")
        assertThat(dependencyTree.evaluateTransitiveDependency("compile" , "test")).isEqualTo(null)

        assertThat(dependencyTree.evaluateTransitiveDependency("provided" , "compile")).isEqualTo("provided")
        assertThat(dependencyTree.evaluateTransitiveDependency("provided" , "provided")).isEqualTo(null)
        assertThat(dependencyTree.evaluateTransitiveDependency("provided" , "runtime")).isEqualTo("provided")
        assertThat(dependencyTree.evaluateTransitiveDependency("provided" , "test")).isEqualTo(null)

        assertThat(dependencyTree.evaluateTransitiveDependency("runtime" , "compile")).isEqualTo("runtime")
        assertThat(dependencyTree.evaluateTransitiveDependency("runtime" , "provided")).isEqualTo(null)
        assertThat(dependencyTree.evaluateTransitiveDependency("runtime" , "runtime")).isEqualTo("runtime")
        assertThat(dependencyTree.evaluateTransitiveDependency("runtime" , "test")).isEqualTo(null)

        assertThat(dependencyTree.evaluateTransitiveDependency("test" , "compile")).isEqualTo("test")
        assertThat(dependencyTree.evaluateTransitiveDependency("test" , "provided")).isEqualTo(null)
        assertThat(dependencyTree.evaluateTransitiveDependency("test" , "runtime")).isEqualTo("test")
        assertThat(dependencyTree.evaluateTransitiveDependency("test" , "test")).isEqualTo(null)
    }

    @Test
    fun versionRange() {

        val av11 = DefaultArtifactVersion("11")
        val vr18 = VersionRange.createFromVersionSpec("[1.8,)")
        assertThat(vr18.containsVersion(av11)).isEqualTo(true)

        val av14 = DefaultArtifactVersion("1.4")
        assertThat(vr18.containsVersion(av14)).isEqualTo(false)

        val av17 = DefaultArtifactVersion("17")
        assertThat(vr18.containsVersion(av17)).isEqualTo(true)

        val av13 = DefaultArtifactVersion("13")
        val vr11 = VersionRange.createFromVersionSpec("[11, 13)")
        assertThat(vr11.containsVersion(av13)).isEqualTo(false)

        val av12 = DefaultArtifactVersion("12")
        assertThat(vr11.containsVersion(av12)).isEqualTo(true)

        assertThat(vr11.containsVersion(av11)).isEqualTo(true)

        val av10 = DefaultArtifactVersion("10")
        assertThat(vr11.containsVersion(av10)).isEqualTo(false)
    }
}