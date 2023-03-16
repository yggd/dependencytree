package org.yggd.dependencytree.model

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.apache.maven.artifact.versioning.VersionRange
import org.apache.maven.model.*
import java.util.*

class ModelWrapper(private val delegate: Model,
                   private val profile: ActiveProfile,
                   private val parentModel: Model? = null) : Model() {

    private val delegationProfiles : List<Profile> = join(parentModel?.profiles, delegate.profiles).filter { p -> isActive(p) }

    private fun join(prof1: List<Profile?>?, prof2: List<Profile?>): List<Profile> {
        val profiles = mutableListOf<Profile>()
        if (prof1 != null) {
            for (prof in prof1) {
                if (prof != null) {
                    profiles += prof
                }
            }
        }
        for (prof in prof2) {
            if (prof != null) {
                profiles += prof
            }
        }
        return profiles
    }

    private fun isActive(pomProfile: Profile) : Boolean {
        if (pomProfile.activation?.isActiveByDefault == true && profile.default()) {
            return true
        }
        if (profile.profileNames.contains(pomProfile.id)) {
            return true
        }
        var jdkRange = pomProfile.activation?.jdk ?: return false
        if (jdkRange.endsWith(",")) {
            // To avoid InvalidVersionSpecificationException: Unbounded range: [9,
            jdkRange = "${jdkRange})"
        } else if (!jdkRange.startsWith("[") && !jdkRange.startsWith("(")) {
            jdkRange = "[${jdkRange}]"
        }
        val contains = VersionRange
            .createFromVersionSpec(jdkRange)
            .containsVersion(DefaultArtifactVersion(actualJdk()))
        return contains
    }

    private fun actualJdk() = when (profile.jdk) {
        "8" -> "1.8"
        else -> profile.jdk
    }

    override fun getDependencies(): MutableList<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val profileDependencies = delegationProfiles.flatMap { p -> p.dependencies }
        dependencies.addAll(profileDependencies)
        dependencies.addAll(delegate.dependencies)
        dependencies.map { d ->
            d.groupId = convertProperty(d.groupId)
            d.artifactId = convertProperty(d.artifactId)
            d.version = convertProperty(d.version)
            d
        }
        return dependencies
    }

    override fun getParent(): Parent? {
        if (parentModel != null) {
            return parentModel.parent
        }
        return delegate.parent
    }

    override fun getDependencyManagement(): DependencyManagement {
        val dependencyManagements = mutableListOf<DependencyManagement>()
        val profileDependencyManagements = delegationProfiles.map { p -> p.dependencyManagement }
        for (profileDm in profileDependencyManagements) {
            if (profileDm != null) {
                dependencyManagements += profileDm
            }
        }
        val parentDm = parentModel?.dependencyManagement
        if (parentDm != null) {
            dependencyManagements += parentDm
        }
        val dm = delegate.dependencyManagement
        if (dm != null) {
            dependencyManagements += dm
        }
        dependencyManagements.forEach {
            it.dependencies?.map { d ->
                d.groupId = convertProperty(d.groupId)
                d.artifactId = convertProperty(d.artifactId)
                d.version = convertProperty(d.version)
                d
            }
        }
        return DependencyManagementWrapper(dependencyManagements)
    }

    override fun getProperties(): Properties {
        val properties = Properties()
        parentModel?.properties?.entries?.forEach { e ->
            properties[e.key] = e.value
        }
        delegate.properties.entries.forEach { e ->
            properties[e.key] = e.value
        }
        delegationProfiles.flatMap { p -> p.properties.entries }.forEach { e ->
            properties[e.key] = e.value
        }
        return properties
    }

    private fun convertProperty(key: String?) : String? {
        if (key == null) {
            return null
        }
        if (!key.startsWith("\${")) {
            return key
        }
        val actualKey = key.substring("\${".length, key.length - 1)
        when (actualKey) {
            "project.version"    -> return convertProperty(version)
            "project.groupId"    -> return convertProperty(groupId)
            "project.artifactId" -> return convertProperty(artifactId)
        }
        return properties[actualKey]?.toString() ?: key
    }

    override fun getArtifactId(): String {
        return convertProperty(delegate.artifactId)!!
    }

    override fun getGroupId(): String {
        return convertProperty(delegate.groupId) ?: convertProperty(delegate.parent.groupId)!!
    }

    override fun getVersion(): String {
        return convertProperty(delegate.version) ?: convertProperty(delegate.parent.version)!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelWrapper

        if (delegate != other.delegate) return false
        if (profile  != other.profile) return false
        if (delegationProfiles != other.delegationProfiles) return false

        return true
    }

    override fun hashCode(): Int {
        var result = delegate.hashCode()
        result = 31 * result + profile.hashCode()
        result = 31 * result + (delegationProfiles.hashCode())
        return result
    }

    override fun toString(): String {
        return "ModelWrapper(delegate=$delegate) (parent=$parentModel)"
    }
}

class DependencyManagementWrapper(private val dm: List<DependencyManagement>) : DependencyManagement() {
    override fun getDependencies(): MutableList<Dependency> {
        return dm.flatMap { d -> d.dependencies }.toMutableList()
    }
}