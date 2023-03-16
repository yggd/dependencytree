package org.yggd.dependencytree.model

data class ActiveProfile(val jdk: String, val profileNames: List<String>) {
    fun default() = profileNames.isEmpty()

    class Builder() {

        private var jdk = System.getProperty("java.specification.version")
        private val profileNames = mutableListOf<String>()

        fun jdk(jdk: String) = this.apply {
            this.jdk = jdk
        }

        fun profile(profile: String) = this.apply {
            this.profileNames += profile
        }

        fun build() = ActiveProfile(this.jdk, this.profileNames)
    }
}
