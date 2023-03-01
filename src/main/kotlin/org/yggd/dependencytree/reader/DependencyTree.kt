package org.yggd.dependencytree.reader

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yggd.dependencytree.model.EffectiveDependency
import org.yggd.dependencytree.model.Pom
import java.util.concurrent.ConcurrentHashMap

class DependencyTree {

    private val pomCache = ConcurrentHashMap<Triple<String, String, String>, Pom?>()

    companion object {
        private val logger : Logger = LoggerFactory.getLogger(DependencyTree::class.java)
        fun traverseDependency(pom: Pom) : Set<EffectiveDependency> = DependencyTree().traverseDependency(pom)
    }

    internal fun traverseDependency(pom: Pom) : Set<EffectiveDependency> {
        val targetDependencies = mutableSetOf<EffectiveDependency>()
        pom.dependencies.forEach { d ->
            if (d.groupId == null || d.version == null) {
                logger.error("groupId and version must be not null. artifactId:{}", d.artifactId)
                return@forEach
            }
            val targetDependency = EffectiveDependency(
                groupId = d.groupId,
                artifactId = d.artifactId,
                version = d.version,
                type = d.type,
                scope = d.scope)
            targetDependency.children.addAll(traverseDependency(targetDependency))
            targetDependencies += targetDependency
        }
        return targetDependencies
    }

    private fun traverseDependency(d: EffectiveDependency) : Set<EffectiveDependency> {

        val pom = pomCache.computeIfAbsent(Triple(
            d.groupId!!,
            d.artifactId,
            d.version!!)) { t ->
            Pom.fromArtifact(t.first, t.second, t.third)
        }

        val childDependencies = mutableSetOf<EffectiveDependency>()

        if (pom == null) {
            logger.error("Can not retrieve POM model. {}:{}:{}", d.groupId, d.artifactId, d.version)
            return childDependencies
        }

        pom.dependencies.forEach { c ->
            val childScope = evaluateTransitiveDependency(d.scope, c.scope) ?: return@forEach
            childDependencies += EffectiveDependency(
                groupId = c.groupId,
                artifactId = c.artifactId,
                version = c.version,
                type = c.type,
                scope = childScope)
        }

        d.children.addAll(childDependencies)

        childDependencies.forEach { cc ->
            cc.children.addAll(traverseDependency(cc))
        }
        return childDependencies
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
    internal fun evaluateTransitiveDependency(dependencyScope: String?, childScope: String?) : String? =
        when (dependencyScope to childScope) {
            ("compile"  to "compile" ) -> "compile"
            ("compile"  to "provided") -> null
            ("compile"  to "runtime" ) -> "runtime"
            ("compile"  to "test"    ) -> null

            ("provided" to "compile" ) -> "provided"
            ("provided" to "provided") -> null
            ("provided" to "runtime" ) -> "provided"
            ("provided" to "test"    ) -> null

            ("runtime"  to "compile" ) -> "runtime"
            ("runtime"  to "provided") -> null
            ("runtime"  to "runtime" ) -> "runtime"
            ("runtime"  to "test"    ) -> null

            ("test"     to "compile" ) -> "test"
            ("test"     to "provided") -> null
            ("test"     to "runtime" ) -> "test"
            ("test"     to "test"    ) -> null

            else -> null
        }
}
