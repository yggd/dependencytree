package org.yggd.dependencytree.model

data class EffectiveDependency constructor(
    var groupId: String?,
    val artifactId: String,
    var version: String?,
    var type: String?,
    var scope: String?,
    var children: MutableSet<EffectiveDependency> = mutableSetOf()
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
        var result = groupId?.hashCode() ?: 0
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        return result
    }
}
