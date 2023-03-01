package org.yggd.dependencytree.model

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import org.apache.maven.model.Activation
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Parent
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yggd.dependencytree.reader.CompositePomReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.Properties

// TODO Modular POMs are not implemented.
class Pom private constructor (
    var groupId: String?,
    val artifactId: String,
    var version: String?) {

    val dependencies = mutableListOf<EffectiveDependency>()
    val dependencyManagements = mutableListOf<EffectiveDependency>()
    val properties = mutableMapOf<String, String?>()

    private val pomReader = CompositePomReader()

    companion object {

        private val logger : Logger = LoggerFactory.getLogger(Pom::class.java)

        fun fromFile(pomFile: File) : Pom {
            val model = model(FileInputStream(pomFile))
            return Pom(model.groupId, model.artifactId, model.version).process(model)
        }

        fun fromArtifact(groupId: String, artifactId: String, version: String) : Pom? =
            Pom(groupId, artifactId, version).process()

        private fun model(inputStream: InputStream) : Model =
            MavenXpp3Reader().read(inputStream, false)
    }

    private fun process() : Pom? {
        if (groupId == null || version == null) {
            throw IllegalArgumentException("groupId and version must be not null.")
        }
        val inputStream = pomReader.read(groupId!!, artifactId, version!!) ?: return null
        return process(inputStream)
    }

    private fun process(inputStream: InputStream) : Pom =
        process(model(inputStream))

    private fun process(model: Model) : Pom {
        // construct POM
        val workDependencies = reflectModel(model)
        dependencies.addAll(workDependencies)

        // refill dependency attributes.
        refillDependencyAttributes()

        return this
    }

    private fun refillDependencyAttributes() {
        dependencies.forEach { d ->
            val dm : EffectiveDependency? = dependencyManagements
                .firstOrNull { dm -> d.artifactId == dm.artifactId }
            val vgroupId = d.groupId ?: dm?.groupId
            val vversion = d.version ?: dm?.version
            val vtype = d.type ?: dm?.type
            val vscope = d.scope ?: dm?.scope ?: "compile"

            d.groupId = vgroupId
            d.version = resolveVersion(vversion)
            d.type = vtype
            d.scope = vscope
        }
    }

    private fun reflectModel(model: Model) : List<EffectiveDependency> {

        val workDependencies = mutableListOf<EffectiveDependency>()

        // profile activation
        model.profiles.filter { p -> evaluateActivation(p.activation) }.forEach { p ->
            storeProperties(p?.properties)
            workDependencies.addAll(storeDependencies(p?.dependencies))
            storeDependencyManagements(p?.dependencyManagement?.dependencies)
            workDependencies.addAll(resolveBom(p?.dependencyManagement?.dependencies))
        }

        storeProperties(model.properties)
        workDependencies.addAll(storeDependencies(model.dependencies))
        storeDependencyManagements(model.dependencyManagement?.dependencies)
        workDependencies.addAll(resolveBom(model.dependencyManagement?.dependencies))

        // resolve Parent
        if (model.parent != null) {
            workDependencies.addAll(reflectParentModel(model.parent))
        }
        return workDependencies;
    }

    private fun evaluateActivation(activation: Activation?) : Boolean {

        if (activation == null) {
            return false
        }

        // TODO Only evaluation of JDK version range.
        var jdkRange = activation.jdk ?: return false
        if (jdkRange.endsWith(",")) {
            // To avoid InvalidVersionSpecificationException: Unbounded range: [9,
            jdkRange = "${jdkRange})"
        }
        val jvmSpecVer = System.getProperty("java.specification.version")

        logger.info("JDK Version Check, jvmSpecVer:{} profile activation jdk range:{}", jvmSpecVer, activation.jdk)

        return VersionRange.createFromVersionSpec(jdkRange).containsVersion(DefaultArtifactVersion(jvmSpecVer))
    }

    private fun storeProperties(prop: Properties?) {
        prop?.entries?.forEach { e ->
            if (!properties.contains(e.key.toString())) {
                properties += e.key.toString() to e.value?.toString()
            }
        }
    }

    private fun storeDependencies(dependencies: List<Dependency>?) : List<EffectiveDependency> {
        val effectiveDependencies = mutableListOf<EffectiveDependency>()
        dependencies?.filter { d -> !d.isOptional }?.forEach { d ->
            effectiveDependencies += convertEffectiveDependency(d)
        }
        return effectiveDependencies
    }

    private fun storeDependencyManagements(dependencies: List<Dependency>?) {
        dependencies?.forEach { d ->
            dependencyManagements += convertEffectiveDependency(d)
        }
    }

    private fun resolveBom(dependencies: List<Dependency>?) : List<EffectiveDependency> {
        val effectiveDependencies = mutableListOf<EffectiveDependency>()
        dependencies?.filter { d -> "pom" == d.type && "import" == d.scope }?.forEach { d ->
            val resolvedVersion = resolveVersion(d.version) ?: return@forEach
            pomReader.read(d.groupId, d.artifactId, resolvedVersion)?.use { reader ->
                effectiveDependencies.addAll(reflectModel(model(reader)))
            }
        }
        return effectiveDependencies
    }

    private fun reflectParentModel(parent: Parent) : List<EffectiveDependency> {
        val reader = pomReader.read(parent.groupId, parent.artifactId, parent.version)
            ?: throw IllegalStateException("reader is null.")
        val model = model(reader)
        if (groupId == null) {
            groupId = model.groupId
        }
        if (version == null) {
            version = model.version
        }
        return reflectModel(model)
    }

    private fun resolveVersion(versionProp: String?, recursive: Boolean = false) : String? {
        if (versionProp == null) {
            return null
        }
        if (!versionProp.startsWith("\${")) {
            return versionProp
        }
        if ("\${project.version}" == versionProp) {
            return version
        }
        val versionCandidate = properties[versionProp.substring("\${".length, versionProp.length - 1)] ?: versionProp
        if (versionCandidate.startsWith("\${")) {
            if (recursive) {
                logger.warn("property may be circular reference key:{} value:{}", versionProp, versionCandidate)
                return versionCandidate
            }
            return resolveVersion(versionCandidate, true)
        }
        return versionCandidate
    }

    private fun convertEffectiveDependency(d: Dependency) =
        EffectiveDependency(groupId = d.groupId, artifactId = d.artifactId, version = d.version, type = d.type, scope = d.scope)
}