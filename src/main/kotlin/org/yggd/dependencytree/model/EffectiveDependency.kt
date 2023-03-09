package org.yggd.dependencytree.model

data class EffectiveDependency(
    var groupId: String,
    val artifactId: String,
    var version: String,
    var type: String?,
    var scope: String?,
    var children: LinkedHashSet<EffectiveDependency> = linkedSetOf(),
    var exclusions: LinkedHashSet<EffectiveExclusion> = linkedSetOf(),
    var duplicate: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EffectiveDependency

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + (version.hashCode())
        return result
    }

    override fun toString(): String {
        return "EffectiveDependency(groupId='$groupId', artifactId='$artifactId', version=$version, type=$type, scope=$scope, duplicate=$duplicate)"
    }
}

data class EffectiveExclusion constructor(val groupId: String, val artifactId: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EffectiveExclusion

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        return result
    }

    override fun toString(): String {
        return "EffectiveExclusion(groupId='$groupId', artifactId='$artifactId')"
    }
}
