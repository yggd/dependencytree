package org.yggd.dependencytree.reader

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.slf4j.LoggerFactory
import org.yggd.dependencytree.model.EffectiveDependency
import org.yggd.dependencytree.model.EffectiveExclusion
import org.yggd.dependencytree.model.ModelWrapper
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.IllegalStateException

// TODO Profile settings.
class DependencyReader(private val jdk: String) {

    companion object {
        private val logger = LoggerFactory.getLogger(DependencyReader::class.java)
    }

    constructor() : this(System.getProperty("java.specification.version"))

    private val pomReader = CompositePomReader()

    private val modelCache = mutableMapOf<Triple<String, String, String>, Model>()

    fun transitDependency(file: File) : Set<EffectiveDependency> {
        return transitDependency(FileInputStream(file))
    }

    internal fun transitDependency(inputStream: InputStream) : Set<EffectiveDependency> {
        val model = inputStream.use { model(it) }
        return transitDependency(model)
    }

    internal fun transitDependency(model: Model) : Set<EffectiveDependency> {
        val dependenciesCache = mutableSetOf<EffectiveDependency>()
        val dependencies = model.dependencies.map { d ->
            convertEffectiveDependency(d, model, model, false)
        }.toSet()
        dependenciesCache.addAll(dependencies)
        for (dependency in dependencies) {
            transitDependency(dependency, model, dependenciesCache)
        }
        return dependencies
    }

    fun transitDependencyMultiModule(file: File): Map<String, Set<EffectiveDependency>> =
        modularModel(file).entries.associate { (k, v) -> k to transitDependency(v) }

    fun flattenMultiModule(file: File): Map<String, Set<EffectiveDependency>> =
        transitDependencyMultiModule(file).entries.associate { (k, v) -> k to flatten(v) }

    fun flatten(file: File) : Set<EffectiveDependency> = flatten(FileInputStream(file))

    fun flatten(inputStream: InputStream) : Set<EffectiveDependency> =
         flatten(transitDependency(inputStream))

    internal fun flatten(dependencies: Set<EffectiveDependency>) : Set<EffectiveDependency> {
        val flattenSet = mutableSetOf<EffectiveDependency>()
        if (dependencies.isEmpty()) {
            return flattenSet
        }
        flattenSet.addAll(dependencies)
        val children = dependencies.flatMap { d -> d.children }.filter { d -> !d.duplicate }.toSet()
        val flattens = flatten(children)
        flattenSet.addAll(flattens)
        return flattenSet
    }

    private fun transitDependency(d: EffectiveDependency, rootModel: Model, dependenciesCache: MutableSet<EffectiveDependency> = mutableSetOf()) {
        val scope = d.scope
        val version = d.version
        val model = model(d.groupId, d.artifactId, version)
        d.children.addAll(model.dependencies
            .filter { child ->
                (child.type == "jar" || child.type == "pom") &&
                !child.isOptional &&
                d.exclusions.find { e -> e.groupId == child.groupId && e.artifactId == child.artifactId } == null &&
                evaluateTransitiveDependencyScope(scope, child.scope) != null }
            .map { child ->
                val effectiveDependency = convertEffectiveDependency(child, rootModel, model, true)
                effectiveDependency.scope = evaluateTransitiveDependencyScope(scope, child.scope)!!
                if (checkDuplicate(effectiveDependency, dependenciesCache)) {
                    effectiveDependency.duplicate = true
                }
                effectiveDependency
            })
        d.children
            .filter  { child -> !child.duplicate }
            .forEach { child -> transitDependency(child, rootModel, dependenciesCache) }
    }

    private fun checkDuplicate(d: EffectiveDependency, dependenciesCache: MutableSet<EffectiveDependency>) : Boolean {
        val contains = dependenciesCache.contains(d)
        if (!contains) {
            dependenciesCache += d
        }
        return contains
    }

    private fun model(groupId: String, artifactId: String, version: String) =
        modelCache.computeIfAbsent(Triple(groupId, artifactId, version)) { t ->
            ModelWrapper(MavenXpp3Reader().read(pomReader.read(t.first, t.second, t.third)), jdk)
        }

    internal fun modularModel(file: File) : Map<String,Model> {
        val rawModel: Model = FileInputStream(file).use { stream ->
            MavenXpp3Reader().read(stream)
        }
        if (rawModel.modules == null || rawModel.modules?.isEmpty() == true) {
            val model = ModelWrapper(rawModel, jdk)
            modelCache.putIfAbsent(Triple(rawModel.groupId, rawModel.artifactId, rawModel.version), model)
            return mapOf(rawModel.artifactId to model)
        }
        return rawModel.modules
            .map { m -> Pair(m, file.toPath().resolveSibling(m).resolve("pom.xml").toFile()) }
            .filter { m ->
                val readable = m.second.exists() && m.second.canRead()
                if (!readable) {
                    logger.warn("Skip module:${m.first}, because modular pom:${m.second} is not readable.")
                }
                readable
            }.associate { m ->
                m.first to FileInputStream(m.second).use { stream ->
                    val model = ModelWrapper(MavenXpp3Reader().read(stream), jdk, rawModel)
                    modelCache.putIfAbsent(Triple(model.groupId, model.artifactId, model.version), model)
                    model
                }
            }
    }

    private fun model(inputStream: InputStream) : Model {
        val model = inputStream.use { ModelWrapper(MavenXpp3Reader().read(it), jdk) }
        modelCache.putIfAbsent(Triple(model.groupId, model.artifactId, model.version), model)
        return model
    }

    private fun resolveTransitiveVersion(d: Dependency, rootModel: Model, model: Model) : String {
        var versionCandidate = resolveVersion(d.groupId, d.artifactId, rootModel) ?: resolveVersion(d.groupId, d.artifactId, model)
        return when {
            // priority 1st: rootModel(argument-defined POM)
            // priority 2nd: parentModel(dependency's POM)
            versionCandidate != null -> {
                versionCandidate
            }
            // priority 3rd: dependency's version
            d.version != null -> {
                versionCandidate = resolveProperty(d.version, rootModel)
                if (versionCandidate.startsWith("\${")) {
                    versionCandidate = resolveProperty(d.version, model)
                }
                versionCandidate
            }
            else -> {
                throw IllegalStateException("Can not resolve transitive version. ${d.groupId}:${d.artifactId}:${d.version}")
            }
        }
    }

    private fun convertEffectiveDependency(d: Dependency, rootModel: Model, model: Model, transitive: Boolean) : EffectiveDependency {
        val version = if (!transitive && d.version != null) {
            resolveProperty(d.version, rootModel)
        } else {
            resolveTransitiveVersion(d, rootModel, model)
        }
        val scope = d.scope ?: "compile"
        return EffectiveDependency(d.groupId, d.artifactId, version, d.type, scope, exclusions = convertExclusion(d.exclusions))
    }

    private fun convertExclusion(exclusions: List<Exclusion>) : LinkedHashSet<EffectiveExclusion> =
        LinkedHashSet<EffectiveExclusion>().apply {
            this.addAll(exclusions.map { e -> EffectiveExclusion(e.groupId, e.artifactId) })
        }

    private fun resolveVersion(groupId: String, artifactId: String, model: Model) :String? {
        // find from dependencyManagement
        var version = model.dependencyManagement?.dependencies
            ?.find { d -> d.groupId == groupId && d.artifactId == artifactId }
            ?.version
        if (version != null) {
            return resolveProperty(version, model)
        }

        // find from parent POM dependencyManagement
        if (model.parent?.artifactId != null) {
            if (groupId == model.parent?.groupId && artifactId == model.parent?.artifactId && model.parent?.version != null) {
                return resolveProperty(model.parent.version, model)
            }
            val parentModel = model(model.parent.groupId, model.parent.artifactId, model.parent.version)
            version = resolveVersion(groupId, artifactId, parentModel)
            if (version != null) {
                return resolveProperty(version, parentModel)
            }
        }

        // find from BOM dependencyManagement
        model.dependencyManagement?.dependencies
            ?.filter { d -> d.type == "pom" && d.scope == "import" }
            ?.map { d -> model(d.groupId, d.artifactId, resolveProperty(d.version, model)) }
            ?.forEach { bom ->
                val versionByBom = findByBomVersion(groupId, artifactId, bom)
                if (versionByBom != null) {
                    return versionByBom
                }
            }
        return null
    }

    private fun findByBomVersion(groupId: String, artifactId: String, bom: Model) : String? {

        // find from Bom's dependencyManagement
        val dependencyManagement = bom.dependencyManagement?.dependencies
            ?.find { d -> d.groupId == groupId && d.artifactId == artifactId }

        if (dependencyManagement != null) {
            return resolveProperty(dependencyManagement.version, bom)
        }

        // find from BOM's parent
        if (bom.parent != null) {
            val bomParent = model(bom.parent.groupId, bom.parent.artifactId, bom.parent.version)
            val parentDependency = findByBomVersion(bom.parent.groupId, bom.parent.artifactId, bomParent)
            if (parentDependency != null) {
                return parentDependency
            }
        }

        // find from nested BOMs
        bom.dependencyManagement?.dependencies
            ?.filter { d -> d.type == "pom" && d.scope == "import" }
            ?.map { d -> model(d.groupId, d.artifactId, resolveProperty(d.version, bom)) }
            ?.forEach { b ->
                val version = findByBomVersion(groupId, artifactId, b)
                if (version != null) {
                    return resolveProperty(version, b)
                }
        }
        return null
    }

    private fun resolveProperty(key: String, model: Model) : String {
        if (!key.startsWith("\${")) {
            return key
        }
        val actualKey = key.substring("\${".length, key.length - 1)
        if (actualKey == "project.version") {
            return model.version
        }
        val value = model.properties[actualKey]?.toString()
        if (value != null) {
            if (value.startsWith("\${")) {
                return resolveProperty(value, model)
            }
            return value
        }
        val parent = model.parent ?: return key
        val parentModel = model(parent.groupId, parent.artifactId, parent.version)
        return resolveProperty(key, parentModel)
    }

    /**
     * https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope
     */
    private fun evaluateTransitiveDependencyScope(dependencyScope: String?, childScope: String?) : String? =
        when (dependencyScope to childScope) {
            ("compile"  to null      ) -> "compile"
            ("compile"  to "compile" ) -> "compile"
            ("compile"  to "provided") -> null
            ("compile"  to "runtime" ) -> "runtime"
            ("compile"  to "test"    ) -> null

            ("provided" to null      ) -> "provided"
            ("provided" to "compile" ) -> "provided"
            ("provided" to "provided") -> null
            ("provided" to "runtime" ) -> "provided"
            ("provided" to "test"    ) -> null

            ("runtime"  to null      ) -> "runtime"
            ("runtime"  to "compile" ) -> "runtime"
            ("runtime"  to "provided") -> null
            ("runtime"  to "runtime" ) -> "runtime"
            ("runtime"  to "test"    ) -> null

            ("test"     to null      ) -> "test"
            ("test"     to "compile" ) -> "test"
            ("test"     to "provided") -> null
            ("test"     to "runtime" ) -> "compile" // TODO ??? org.objenesis:objenesis:3.2 (transit from org.mockito:mockito-core) may be "test" but "compile"
            ("test"     to "test"    ) -> null

            else -> null
        }
}