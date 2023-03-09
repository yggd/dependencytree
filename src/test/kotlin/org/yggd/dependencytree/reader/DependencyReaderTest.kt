package org.yggd.dependencytree.reader

import org.junit.jupiter.api.Test

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import java.io.File

class DependencyReaderTest {

    @Test
    fun rootDependencies() {
        val dependencyReader = DependencyReader("11")
        val dependencies = dependencyReader.transitDependency(File("testpom/terabatch541.xml"))

        assertThat(dependencies)
            .isNotEmpty
            .hasSize(6)
            .extracting("artifactId")
            .contains("terasoluna-batch")

        val terasolunaBatch = dependencies.find { d -> d.artifactId == "terasoluna-batch" }
        assertThat(terasolunaBatch)
            .isNotNull
            .extracting("groupId", "artifactId", "version", "scope")
            .isEqualTo(listOf("org.terasoluna.batch", "terasoluna-batch", "5.4.1.RELEASE", "compile"))
        assertThat(terasolunaBatch?.children)
            .isNotEmpty.hasSize(7)
            .extracting("artifactId")
            .contains("commons-dbcp2")

        // exclusion commons-logging check
        val commonsDbcp2 = terasolunaBatch?.children?.find { c -> c.artifactId == "commons-dbcp2" }
        assertThat(commonsDbcp2)
            .extracting("groupId", "artifactId", "version", "scope")
            .isEqualTo(listOf("org.apache.commons", "commons-dbcp2", "2.9.0", "runtime"))
        assertThat(commonsDbcp2?.children)
            .isNotEmpty
            .hasSize(1)
            .extracting("artifactId")
            .contains("commons-pool2")

        // transitive scope check compile -> runtime
        val commonsPool2 = commonsDbcp2?.children?.find { c -> c.artifactId == "commons-pool2" }
        assertThat(commonsPool2)
            .extracting("groupId", "artifactId", "version", "scope")
            .isEqualTo(listOf("org.apache.commons", "commons-pool2", "2.11.1", "runtime"))

        // duplicate check.
        val springBatchInfrastructure = terasolunaBatch?.children
            ?.find { c -> c.artifactId == "spring-batch-core" }?.children
            ?.find { c -> c.artifactId == "spring-batch-infrastructure" }
        assertThat(springBatchInfrastructure?.children)
            .isNotEmpty
            .hasSize(2)
            .extracting("artifactId")
            .contains("spring-retry", "spring-core")

        val springCore = springBatchInfrastructure?.children
            ?.find { c -> c.artifactId == "spring-core" }
        assertThat(springCore)
            .isNotNull
            .extracting("groupId", "artifactId", "version", "scope", "duplicate")
            .isEqualTo(listOf("org.springframework", "spring-core", "5.3.13", "compile", true))
        assertThat(springCore?.children).isEmpty()

        val springAop = terasolunaBatch?.children
            ?.find { c -> c.artifactId == "spring-batch-core" }?.children
            ?.find { c -> c.artifactId == "spring-aop"}
        assertThat(springAop)
            .isNotNull
            .extracting("groupId", "artifactId", "version", "scope", "duplicate")
            .isEqualTo(listOf("org.springframework", "spring-aop", "5.3.13", "compile", false))
        assertThat(springAop?.children)
            .isNotEmpty
            .hasSize(2)
            .extracting("artifactId")
            .contains("spring-beans", "spring-core")
        val springBeans = springAop?.children?.find { c -> c.artifactId == "spring-beans" }
        assertThat(springBeans)
            .isNotNull
            .extracting("groupId", "artifactId", "version", "scope", "duplicate")
            .isEqualTo(listOf("org.springframework", "spring-beans", "5.3.13", "compile", true))
        val springCore2 = springAop?.children?.find { c -> c.artifactId == "spring-core"}
        assertThat(springCore2)
            .isNotNull
            .extracting("groupId", "artifactId", "version", "scope", "duplicate")
            .isEqualTo(listOf("org.springframework", "spring-core", "5.3.13", "compile", true))
    }

    @Test
    fun flattenSingleModule() {
        val dependencyReaderJdk8 = DependencyReader("1.8")
        var flatten = dependencyReaderJdk8.flatten(File("testpom/terabatch541.xml"))
        assertThat(flatten)
            .hasSize(37)
            .extracting("groupId", "artifactId", "type", "version", "scope")
            .contains(
                tuple("org.terasoluna.batch","terasoluna-batch","jar","5.4.1.RELEASE","compile"),
                tuple("org.springframework.batch","spring-batch-core","jar","4.3.4","compile"),
                tuple("com.fasterxml.jackson.core","jackson-databind","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-annotations","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-core","jar","2.13.0","compile"),
                tuple("io.micrometer","micrometer-core","jar","1.8.0","compile"),
                tuple("org.hdrhistogram","HdrHistogram","jar","2.1.12","compile"),
                tuple("org.latencyutils","LatencyUtils","jar","2.0.3","runtime"),
                tuple("javax.batch","javax.batch-api","jar","1.0","compile"),
                tuple("org.codehaus.jettison","jettison","jar","1.2","compile"),
                tuple("org.springframework.batch","spring-batch-infrastructure","jar","4.3.4","compile"),
                tuple("org.springframework.retry","spring-retry","jar","1.3.1","compile"),
                tuple("org.springframework","spring-aop","jar","5.3.13","compile"),
                tuple("org.springframework","spring-beans","jar","5.3.13","compile"),
                tuple("org.springframework","spring-context","jar","5.3.13","compile"),
                tuple("org.springframework","spring-expression","jar","5.3.13","compile"),
                tuple("org.springframework","spring-core","jar","5.3.13","compile"),
                tuple("org.springframework","spring-jcl","jar","5.3.13","compile"),
                tuple("org.springframework","spring-tx","jar","5.3.13","compile"),
                tuple("org.springframework","spring-jdbc","jar","5.3.13","compile"),
                tuple("org.mybatis","mybatis","jar","3.5.7","compile"),
                tuple("org.mybatis","mybatis-spring","jar","2.0.6","compile"),
                tuple("org.slf4j","jcl-over-slf4j","jar","1.7.32","compile"),
                tuple("org.apache.commons","commons-dbcp2","jar","2.9.0","runtime"),
                tuple("org.apache.commons","commons-pool2","jar","2.11.1","runtime"),
                tuple("jakarta.inject","jakarta.inject-api","jar","1.0.5","compile"),
                tuple("org.hibernate.validator","hibernate-validator","jar","6.2.0.Final","compile"),
                tuple("jakarta.validation","jakarta.validation-api","jar","2.0.2","compile"),
                tuple("org.jboss.logging","jboss-logging","jar","3.4.2.Final","compile"),
                tuple("com.fasterxml","classmate","jar","1.5.1","compile"),
                tuple("org.glassfish","jakarta.el","jar","3.0.4","runtime"),
                tuple("ch.qos.logback","logback-classic","jar","1.2.7","runtime"),
                tuple("ch.qos.logback","logback-core","jar","1.2.7","runtime"),
                tuple("org.slf4j","slf4j-api","jar","1.7.32","compile"),
                tuple("com.h2database","h2","jar","1.4.200","runtime"),
                tuple("org.postgresql","postgresql","jar","42.3.1","runtime"),
                tuple("org.checkerframework","checker-qual","jar","3.5.0","runtime")
            )

        val dependencyReaderJdk11 = DependencyReader("11")
        flatten = dependencyReaderJdk11.flatten(File("testpom/terabatch541.xml"))
        assertThat(flatten)
            .hasSize(38)
            .extracting("groupId", "artifactId", "type", "version", "scope")
            .contains(
                tuple("org.terasoluna.batch","terasoluna-batch","jar","5.4.1.RELEASE","compile"),
                tuple("org.springframework.batch","spring-batch-core","jar","4.3.4","compile"),
                tuple("com.fasterxml.jackson.core","jackson-databind","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-annotations","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-core","jar","2.13.0","compile"),
                tuple("io.micrometer","micrometer-core","jar","1.8.0","compile"),
                tuple("org.hdrhistogram","HdrHistogram","jar","2.1.12","compile"),
                tuple("org.latencyutils","LatencyUtils","jar","2.0.3","runtime"),
                tuple("javax.batch","javax.batch-api","jar","1.0","compile"),
                tuple("org.codehaus.jettison","jettison","jar","1.2","compile"),
                tuple("org.springframework.batch","spring-batch-infrastructure","jar","4.3.4","compile"),
                tuple("org.springframework.retry","spring-retry","jar","1.3.1","compile"),
                tuple("javax.annotation","javax.annotation-api","jar","1.3.2","compile"),
                tuple("org.springframework","spring-aop","jar","5.3.13","compile"),
                tuple("org.springframework","spring-beans","jar","5.3.13","compile"),
                tuple("org.springframework","spring-context","jar","5.3.13","compile"),
                tuple("org.springframework","spring-expression","jar","5.3.13","compile"),
                tuple("org.springframework","spring-core","jar","5.3.13","compile"),
                tuple("org.springframework","spring-jcl","jar","5.3.13","compile"),
                tuple("org.springframework","spring-tx","jar","5.3.13","compile"),
                tuple("org.springframework","spring-jdbc","jar","5.3.13","compile"),
                tuple("org.mybatis","mybatis","jar","3.5.7","compile"),
                tuple("org.mybatis","mybatis-spring","jar","2.0.6","compile"),
                tuple("org.slf4j","jcl-over-slf4j","jar","1.7.32","compile"),
                tuple("org.apache.commons","commons-dbcp2","jar","2.9.0","runtime"),
                tuple("org.apache.commons","commons-pool2","jar","2.11.1","runtime"),
                tuple("jakarta.inject","jakarta.inject-api","jar","1.0.5","compile"),
                tuple("org.hibernate.validator","hibernate-validator","jar","6.2.0.Final","compile"),
                tuple("jakarta.validation","jakarta.validation-api","jar","2.0.2","compile"),
                tuple("org.jboss.logging","jboss-logging","jar","3.4.2.Final","compile"),
                tuple("com.fasterxml","classmate","jar","1.5.1","compile"),
                tuple("org.glassfish","jakarta.el","jar","3.0.4","runtime"),
                tuple("ch.qos.logback","logback-classic","jar","1.2.7","runtime"),
                tuple("ch.qos.logback","logback-core","jar","1.2.7","runtime"),
                tuple("org.slf4j","slf4j-api","jar","1.7.32","compile"),
                tuple("com.h2database","h2","jar","1.4.200","runtime"),
                tuple("org.postgresql","postgresql","jar","42.3.1","runtime"),
                tuple("org.checkerframework","checker-qual","jar","3.5.0","runtime"))

        val dependencyReaderJdk17 = DependencyReader("17")
        flatten = dependencyReaderJdk17.flatten(File("testpom/terabatch541.xml"))
        assertThat(flatten)
            .hasSize(38)
            .extracting("groupId", "artifactId", "type", "version", "scope")
            .contains(
                tuple("org.terasoluna.batch","terasoluna-batch","jar","5.4.1.RELEASE","compile"),
                tuple("org.springframework.batch","spring-batch-core","jar","4.3.4","compile"),
                tuple("com.fasterxml.jackson.core","jackson-databind","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-annotations","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-core","jar","2.13.0","compile"),
                tuple("io.micrometer","micrometer-core","jar","1.8.0","compile"),
                tuple("org.hdrhistogram","HdrHistogram","jar","2.1.12","compile"),
                tuple("org.latencyutils","LatencyUtils","jar","2.0.3","runtime"),
                tuple("javax.batch","javax.batch-api","jar","1.0","compile"),
                tuple("org.codehaus.jettison","jettison","jar","1.2","compile"),
                tuple("org.springframework.batch","spring-batch-infrastructure","jar","4.3.4","compile"),
                tuple("org.springframework.retry","spring-retry","jar","1.3.1","compile"),
                tuple("javax.annotation","javax.annotation-api","jar","1.3.2","compile"),
                tuple("org.springframework","spring-aop","jar","5.3.13","compile"),
                tuple("org.springframework","spring-beans","jar","5.3.13","compile"),
                tuple("org.springframework","spring-context","jar","5.3.13","compile"),
                tuple("org.springframework","spring-expression","jar","5.3.13","compile"),
                tuple("org.springframework","spring-core","jar","5.3.13","compile"),
                tuple("org.springframework","spring-jcl","jar","5.3.13","compile"),
                tuple("org.springframework","spring-tx","jar","5.3.13","compile"),
                tuple("org.springframework","spring-jdbc","jar","5.3.13","compile"),
                tuple("org.mybatis","mybatis","jar","3.5.7","compile"),
                tuple("org.mybatis","mybatis-spring","jar","2.0.6","compile"),
                tuple("org.slf4j","jcl-over-slf4j","jar","1.7.32","compile"),
                tuple("org.apache.commons","commons-dbcp2","jar","2.9.0","runtime"),
                tuple("org.apache.commons","commons-pool2","jar","2.11.1","runtime"),
                tuple("jakarta.inject","jakarta.inject-api","jar","1.0.5","compile"),
                tuple("org.hibernate.validator","hibernate-validator","jar","6.2.0.Final","compile"),
                tuple("jakarta.validation","jakarta.validation-api","jar","2.0.2","compile"),
                tuple("org.jboss.logging","jboss-logging","jar","3.4.2.Final","compile"),
                tuple("com.fasterxml","classmate","jar","1.5.1","compile"),
                tuple("org.glassfish","jakarta.el","jar","3.0.4","runtime"),
                tuple("ch.qos.logback","logback-classic","jar","1.2.7","runtime"),
                tuple("ch.qos.logback","logback-core","jar","1.2.7","runtime"),
                tuple("org.slf4j","slf4j-api","jar","1.7.32","compile"),
                tuple("com.h2database","h2","jar","1.4.200","runtime"),
                tuple("org.postgresql","postgresql","jar","42.3.1","runtime"),
                tuple("org.checkerframework","checker-qual","jar","3.5.0","runtime")
            )
    }

    @Test
    fun flattenMultiModule() {
        val dependencyReader = DependencyReader("1.8")
        val modules = dependencyReader.flattenMultiModule(File("testPom/teraserver571sp1.xml"))
        assertThat(modules)
            .hasSize(5)
        val moduleNames = modules.entries.map { e -> e.key }
        assertThat(moduleNames)
            .contains("todo-domain", "todo-env", "todo-initdb", "todo-selenium", "todo-web")

        // domain module
        val flattenDomain = modules["todo-domain"]
        assertThat(flattenDomain)
            .extracting("groupId", "artifactId", "type", "version", "scope")
            .contains(
                tuple("org.terasoluna.gfw","terasoluna-gfw-common-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-common","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("org.aspectj","aspectjrt","jar","1.9.7","compile"),
                tuple("org.aspectj","aspectjweaver","jar","1.9.7","compile"),
                tuple("ch.qos.logback","logback-classic","jar","1.2.7","compile"),
                tuple("ch.qos.logback","logback-core","jar","1.2.7","compile"),
                tuple("org.springframework","spring-context-support","jar","5.3.18","compile"),
                tuple("org.springframework","spring-orm","jar","5.3.18","compile"),
                tuple("org.springframework","spring-jdbc","jar","5.3.18","compile"),
                tuple("org.springframework","spring-tx","jar","5.3.18","compile"),
                tuple("org.springframework.data","spring-data-commons","jar","2.6.0","compile"),
                tuple("com.google.guava","guava","jar","30.1.1-jre","compile"),
                tuple("com.google.guava","failureaccess","jar","1.0.1","compile"),
                tuple("com.google.guava","listenablefuture","jar","9999.0-empty-to-avoid-conflict-with-guava","compile"),
                tuple("com.google.code.findbugs","jsr305","jar","3.0.2","compile"),
                tuple("org.checkerframework","checker-qual","jar","3.8.0","compile"),
                tuple("com.google.errorprone","error_prone_annotations","jar","2.5.1","compile"),
                tuple("com.google.j2objc","j2objc-annotations","jar","1.3","compile"),
                tuple("com.fasterxml.jackson.core","jackson-databind","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-annotations","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-core","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-jsr310","jar","2.13.0","compile"),
                tuple("org.slf4j","slf4j-api","jar","1.7.32","compile"),
                tuple("org.springframework","spring-aop","jar","5.3.18","compile"),
                tuple("org.springframework","spring-beans","jar","5.3.18","compile"),
                tuple("org.springframework","spring-aspects","jar","5.3.18","compile"),
                tuple("org.springframework","spring-context","jar","5.3.18","compile"),
                tuple("org.springframework","spring-expression","jar","5.3.18","compile"),
                tuple("org.hibernate.validator","hibernate-validator","jar","6.2.0.Final","compile"),
                tuple("jakarta.validation","jakarta.validation-api","jar","2.0.2","compile"),
                tuple("org.jboss.logging","jboss-logging","jar","3.4.2.Final","compile"),
                tuple("com.fasterxml","classmate","jar","1.5.1","compile"),
                tuple("jakarta.inject","jakarta.inject-api","jar","1.0.5","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("joda-time","joda-time","jar","2.10.9","compile"),
                tuple("joda-time","joda-time-jsptags","jar","1.1.1","compile"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-joda","jar","2.13.0","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-security-core-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.springframework.security","spring-security-config","jar","5.6.0","compile"),
                tuple("org.springframework.security","spring-security-core","jar","5.6.0","compile"),
                tuple("org.springframework.security","spring-security-crypto","jar","5.6.0","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-mybatis3-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.mybatis","mybatis","jar","3.5.7","compile"),
                tuple("org.mybatis","mybatis-spring","jar","2.0.6","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-recommended-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("commons-beanutils","commons-beanutils","jar","1.9.4","compile"),
                tuple("commons-logging","commons-logging","jar","1.2","compile"),
                tuple("commons-collections","commons-collections","jar","3.2.2","compile"),
                tuple("commons-io","commons-io","jar","2.11.0","compile"),
                tuple("org.apache.commons","commons-dbcp2","jar","2.9.0","compile"),
                tuple("org.apache.commons","commons-pool2","jar","2.11.1","compile"),
                tuple("org.apache.commons","commons-lang3","jar","3.12.0","compile"),
                tuple("com.github.dozermapper","dozer-core","jar","6.5.2","compile"),
                tuple("org.slf4j","jcl-over-slf4j","jar","1.7.32","compile"),
                tuple("com.github.dozermapper","dozer-spring4","jar","6.5.2","compile"),
                tuple("junit","junit","jar","4.13.2","test"),
                tuple("org.hamcrest","hamcrest-core","jar","2.2","test"),
                tuple("org.hamcrest","hamcrest","jar","2.2","test"),
                tuple("org.mockito","mockito-core","jar","4.0.0","test"),
                tuple("net.bytebuddy","byte-buddy","jar","1.11.22","test"),
                tuple("net.bytebuddy","byte-buddy-agent","jar","1.11.22","test"),
                tuple("org.objenesis","objenesis","jar","3.2","compile"),
                tuple("org.springframework","spring-test","jar","5.3.18","test"),
                tuple("org.springframework","spring-core","jar","5.3.18","compile"),
                tuple("org.springframework","spring-jcl","jar","5.3.18","compile"),
                tuple("org.apache.tomcat.embed","tomcat-embed-el","jar","9.0.55","test"))

        // env module
        val flattenEnv = modules["todo-env"]
        assertThat(flattenEnv)
            .extracting("groupId", "artifactId", "type", "version", "scope")
            .contains(
                tuple("org.terasoluna.gfw","terasoluna-gfw-common-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-common","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("org.aspectj","aspectjrt","jar","1.9.7","compile"),
                tuple("org.aspectj","aspectjweaver","jar","1.9.7","compile"),
                tuple("ch.qos.logback","logback-classic","jar","1.2.7","compile"),
                tuple("ch.qos.logback","logback-core","jar","1.2.7","compile"),
                tuple("org.springframework","spring-context-support","jar","5.3.18","compile"),
                tuple("org.springframework","spring-orm","jar","5.3.18","compile"),
                tuple("org.springframework","spring-jdbc","jar","5.3.18","compile"),
                tuple("org.springframework","spring-tx","jar","5.3.18","compile"),
                tuple("org.springframework.data","spring-data-commons","jar","2.6.0","compile"),
                tuple("com.google.guava","guava","jar","30.1.1-jre","compile"),
                tuple("com.google.guava","failureaccess","jar","1.0.1","compile"),
                tuple("com.google.guava","listenablefuture","jar","9999.0-empty-to-avoid-conflict-with-guava","compile"),
                tuple("com.google.code.findbugs","jsr305","jar","3.0.2","compile"),
                tuple("org.checkerframework","checker-qual","jar","3.8.0","compile"),
                tuple("com.google.errorprone","error_prone_annotations","jar","2.5.1","compile"),
                tuple("com.google.j2objc","j2objc-annotations","jar","1.3","compile"),
                tuple("com.fasterxml.jackson.core","jackson-databind","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-annotations","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-core","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-jsr310","jar","2.13.0","compile"),
                tuple("org.slf4j","slf4j-api","jar","1.7.32","compile"),
                tuple("org.springframework","spring-aop","jar","5.3.18","compile"),
                tuple("org.springframework","spring-beans","jar","5.3.18","compile"),
                tuple("org.springframework","spring-core","jar","5.3.18","compile"),
                tuple("org.springframework","spring-jcl","jar","5.3.18","compile"),
                tuple("org.springframework","spring-aspects","jar","5.3.18","compile"),
                tuple("org.springframework","spring-context","jar","5.3.18","compile"),
                tuple("org.springframework","spring-expression","jar","5.3.18","compile"),
                tuple("org.hibernate.validator","hibernate-validator","jar","6.2.0.Final","compile"),
                tuple("jakarta.validation","jakarta.validation-api","jar","2.0.2","compile"),
                tuple("org.jboss.logging","jboss-logging","jar","3.4.2.Final","compile"),
                tuple("com.fasterxml","classmate","jar","1.5.1","compile"),
                tuple("jakarta.inject","jakarta.inject-api","jar","1.0.5","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("joda-time","joda-time","jar","2.10.9","compile"),
                tuple("joda-time","joda-time-jsptags","jar","1.1.1","compile"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-joda","jar","2.13.0","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-mybatis3-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.mybatis","mybatis","jar","3.5.7","compile"),
                tuple("org.mybatis","mybatis-spring","jar","2.0.6","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-recommended-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("commons-beanutils","commons-beanutils","jar","1.9.4","compile"),
                tuple("commons-logging","commons-logging","jar","1.2","compile"),
                tuple("commons-collections","commons-collections","jar","3.2.2","compile"),
                tuple("commons-io","commons-io","jar","2.11.0","compile"),
                tuple("org.apache.commons","commons-dbcp2","jar","2.9.0","compile"),
                tuple("org.apache.commons","commons-pool2","jar","2.11.1","compile"),
                tuple("org.apache.commons","commons-lang3","jar","3.12.0","compile"),
                tuple("com.github.dozermapper","dozer-core","jar","6.5.2","compile"),
                tuple("org.slf4j","jcl-over-slf4j","jar","1.7.32","compile"),
                tuple("org.objenesis","objenesis","jar","2.6","compile"),
                tuple("com.github.dozermapper","dozer-spring4","jar","6.5.2","compile"),
                tuple("com.h2database","h2","jar","1.4.200","runtime"))

        // initdb module
        val flattenInitdb = modules["todo-initdb"]
        assertThat(flattenInitdb).isNotNull.isEmpty()

        // selenium module
        val flattenSelenium = modules["todo-selenium"]
        assertThat(flattenSelenium)
            .extracting("groupId", "artifactId", "type", "version", "scope")
            .contains(
                tuple("com.example.todo","todo-domain","jar","1.0.0-SNAPSHOT","test"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-common-dependencies","pom","5.7.1.SP1.RELEASE","test"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-common","jar","5.7.1.SP1.RELEASE","test"),
                tuple("org.aspectj","aspectjrt","jar","1.9.7","test"),
                tuple("org.aspectj","aspectjweaver","jar","1.9.7","test"),
                tuple("ch.qos.logback","logback-classic","jar","1.2.7","test"),
                tuple("ch.qos.logback","logback-core","jar","1.2.7","test"),
                tuple("org.springframework","spring-context-support","jar","5.3.18","test"),
                tuple("org.springframework","spring-orm","jar","5.3.18","test"),
                tuple("org.springframework","spring-jdbc","jar","5.3.18","test"),
                tuple("org.springframework","spring-tx","jar","5.3.18","test"),
                tuple("org.springframework.data","spring-data-commons","jar","2.6.0","test"),
                tuple("com.fasterxml.jackson.core","jackson-databind","jar","2.13.0","test"),
                tuple("com.fasterxml.jackson.core","jackson-annotations","jar","2.13.0","test"),
                tuple("com.fasterxml.jackson.core","jackson-core","jar","2.13.0","test"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-jsr310","jar","2.13.0","test"),
                tuple("org.slf4j","slf4j-api","jar","1.7.32","test"),
                tuple("org.springframework","spring-aop","jar","5.3.18","test"),
                tuple("org.springframework","spring-beans","jar","5.3.18","test"),
                tuple("org.springframework","spring-aspects","jar","5.3.18","test"),
                tuple("org.springframework","spring-context","jar","5.3.18","test"),
                tuple("org.springframework","spring-expression","jar","5.3.18","test"),
                tuple("org.hibernate.validator","hibernate-validator","jar","6.2.0.Final","test"),
                tuple("jakarta.validation","jakarta.validation-api","jar","2.0.2","test"),
                tuple("org.jboss.logging","jboss-logging","jar","3.4.2.Final","test"),
                tuple("com.fasterxml","classmate","jar","1.5.1","test"),
                tuple("jakarta.inject","jakarta.inject-api","jar","1.0.5","test"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime-dependencies","pom","5.7.1.SP1.RELEASE","test"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime","jar","5.7.1.SP1.RELEASE","test"),
                tuple("joda-time","joda-time","jar","2.10.9","test"),
                tuple("joda-time","joda-time-jsptags","jar","1.1.1","test"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-joda","jar","2.13.0","test"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-security-core-dependencies","pom","5.7.1.SP1.RELEASE","test"),
                tuple("org.springframework.security","spring-security-config","jar","5.6.0","test"),
                tuple("org.springframework.security","spring-security-core","jar","5.6.0","test"),
                tuple("org.springframework.security","spring-security-crypto","jar","5.6.0","test"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-mybatis3-dependencies","pom","5.7.1.SP1.RELEASE","test"),
                tuple("org.mybatis","mybatis","jar","3.5.7","test"),
                tuple("org.mybatis","mybatis-spring","jar","2.0.6","test"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-recommended-dependencies","pom","5.7.1.SP1.RELEASE","test"),
                tuple("commons-beanutils","commons-beanutils","jar","1.9.4","test"),
                tuple("commons-logging","commons-logging","jar","1.2","test"),
                tuple("commons-collections","commons-collections","jar","3.2.2","test"),
                tuple("commons-io","commons-io","jar","2.11.0","test"),
                tuple("org.apache.commons","commons-dbcp2","jar","2.9.0","test"),
                tuple("org.apache.commons","commons-pool2","jar","2.11.1","test"),
                tuple("org.apache.commons","commons-lang3","jar","3.12.0","test"),
                tuple("com.github.dozermapper","dozer-core","jar","6.5.2","test"),
                tuple("org.slf4j","jcl-over-slf4j","jar","1.7.32","test"),
                tuple("org.objenesis","objenesis","jar","2.6","test"),
                tuple("com.github.dozermapper","dozer-spring4","jar","6.5.2","test"),
                tuple("org.springframework","spring-test","jar","5.3.18","test"),
                tuple("org.springframework","spring-core","jar","5.3.18","test"),
                tuple("org.springframework","spring-jcl","jar","5.3.18","test"),
                tuple("junit","junit","jar","4.13.2","test"),
                tuple("org.hamcrest","hamcrest-core","jar","2.2","test"),
                tuple("org.hamcrest","hamcrest","jar","2.2","test"),
                tuple("org.seleniumhq.selenium","selenium-java","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-api","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-chrome-driver","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-edge-driver","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-firefox-driver","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-ie-driver","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-opera-driver","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-remote-driver","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-safari-driver","jar","3.141.59","test"),
                tuple("org.seleniumhq.selenium","selenium-support","jar","3.141.59","test"),
                tuple("net.bytebuddy","byte-buddy","jar","1.11.22","test"),
                tuple("org.apache.commons","commons-exec","jar","1.3","test"),
                tuple("com.google.guava","guava","jar","30.1.1-jre","test"),
                tuple("com.google.guava","failureaccess","jar","1.0.1","test"),
                tuple("com.google.guava","listenablefuture","jar","9999.0-empty-to-avoid-conflict-with-guava","test"),
                tuple("com.google.code.findbugs","jsr305","jar","3.0.2","test"),
                tuple("org.checkerframework","checker-qual","jar","3.8.0","test"),
                tuple("com.google.errorprone","error_prone_annotations","jar","2.5.1","test"),
                tuple("com.google.j2objc","j2objc-annotations","jar","1.3","test"),
                tuple("com.squareup.okhttp3","okhttp","jar","3.14.9","test"),
                tuple("com.squareup.okio","okio","jar","1.14.0","test"))

        // web module
        val flattenWeb = modules["todo-web"]
        assertThat(flattenWeb)
            .extracting("groupId", "artifactId", "type", "version", "scope")
            .contains(
                tuple("com.example.todo","todo-domain","jar","1.0.0-SNAPSHOT","compile"),
                tuple("com.example.todo","todo-domain","jar","1.0.0-SNAPSHOT","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-common-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-common","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("org.aspectj","aspectjrt","jar","1.9.7","compile"),
                tuple("org.aspectj","aspectjweaver","jar","1.9.7","compile"),
                tuple("ch.qos.logback","logback-classic","jar","1.2.7","compile"),
                tuple("ch.qos.logback","logback-core","jar","1.2.7","compile"),
                tuple("org.springframework","spring-context-support","jar","5.3.18","compile"),
                tuple("org.springframework","spring-orm","jar","5.3.18","compile"),
                tuple("org.springframework","spring-jdbc","jar","5.3.18","compile"),
                tuple("org.springframework","spring-tx","jar","5.3.18","compile"),
                tuple("org.springframework.data","spring-data-commons","jar","2.6.0","compile"),
                tuple("com.google.guava","guava","jar","30.1.1-jre","compile"),
                tuple("com.google.guava","failureaccess","jar","1.0.1","compile"),
                tuple("com.google.guava","listenablefuture","jar","9999.0-empty-to-avoid-conflict-with-guava","compile"),
                tuple("com.google.code.findbugs","jsr305","jar","3.0.2","compile"),
                tuple("org.checkerframework","checker-qual","jar","3.8.0","compile"),
                tuple("com.google.errorprone","error_prone_annotations","jar","2.5.1","compile"),
                tuple("com.google.j2objc","j2objc-annotations","jar","1.3","compile"),
                tuple("com.fasterxml.jackson.core","jackson-databind","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-annotations","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.core","jackson-core","jar","2.13.0","compile"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-jsr310","jar","2.13.0","compile"),
                tuple("org.slf4j","slf4j-api","jar","1.7.32","compile"),
                tuple("org.springframework","spring-aop","jar","5.3.18","compile"),
                tuple("org.springframework","spring-beans","jar","5.3.18","compile"),
                tuple("org.springframework","spring-aspects","jar","5.3.18","compile"),
                tuple("org.springframework","spring-context","jar","5.3.18","compile"),
                tuple("org.springframework","spring-expression","jar","5.3.18","compile"),
                tuple("org.hibernate.validator","hibernate-validator","jar","6.2.0.Final","compile"),
                tuple("jakarta.validation","jakarta.validation-api","jar","2.0.2","compile"),
                tuple("org.jboss.logging","jboss-logging","jar","3.4.2.Final","compile"),
                tuple("com.fasterxml","classmate","jar","1.5.1","compile"),
                tuple("jakarta.inject","jakarta.inject-api","jar","1.0.5","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-jodatime","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("joda-time","joda-time","jar","2.10.9","compile"),
                tuple("joda-time","joda-time-jsptags","jar","1.1.1","compile"),
                tuple("com.fasterxml.jackson.datatype","jackson-datatype-joda","jar","2.13.0","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-security-core-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.springframework.security","spring-security-config","jar","5.6.0","compile"),
                tuple("org.springframework.security","spring-security-core","jar","5.6.0","compile"),
                tuple("org.springframework.security","spring-security-crypto","jar","5.6.0","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-mybatis3-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.mybatis","mybatis","jar","3.5.7","compile"),
                tuple("org.mybatis","mybatis-spring","jar","2.0.6","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-recommended-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("commons-beanutils","commons-beanutils","jar","1.9.4","compile"),
                tuple("commons-logging","commons-logging","jar","1.2","compile"),
                tuple("commons-collections","commons-collections","jar","3.2.2","compile"),
                tuple("commons-io","commons-io","jar","2.11.0","compile"),
                tuple("org.apache.commons","commons-dbcp2","jar","2.9.0","compile"),
                tuple("org.apache.commons","commons-pool2","jar","2.11.1","compile"),
                tuple("org.apache.commons","commons-lang3","jar","3.12.0","compile"),
                tuple("com.github.dozermapper","dozer-core","jar","6.5.2","compile"),
                tuple("com.github.dozermapper","dozer-spring4","jar","6.5.2","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-web-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-web","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("org.springframework","spring-webmvc","jar","5.3.18","compile"),
                tuple("org.springframework","spring-web","jar","5.3.18","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-web-jsp-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-web-jsp","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-security-web-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-security-web","jar","5.7.1.SP1.RELEASE","compile"),
                tuple("org.springframework.security","spring-security-taglibs","jar","5.6.0","compile"),
                tuple("org.springframework.security","spring-security-acl","jar","5.6.0","compile"),
                tuple("org.springframework.security","spring-security-web","jar","5.6.0","compile"),
                tuple("org.terasoluna.gfw","terasoluna-gfw-recommended-web-dependencies","pom","5.7.1.SP1.RELEASE","compile"),
                tuple("org.apache.tiles","tiles-core","jar","3.0.8","compile"),
                tuple("org.apache.tiles","tiles-api","jar","3.0.8","compile"),
                tuple("org.apache.tiles","tiles-request-api","jar","1.0.7","compile"),
                tuple("commons-digester","commons-digester","jar","2.0","compile"),
                tuple("org.slf4j","jcl-over-slf4j","jar","1.7.32","compile"),
                tuple("org.apache.tiles","tiles-jsp","jar","3.0.8","compile"),
                tuple("org.apache.tiles","tiles-servlet","jar","3.0.8","compile"),
                tuple("org.apache.tiles","tiles-request-servlet","jar","1.0.7","compile"),
                tuple("org.apache.tiles","tiles-template","jar","3.0.8","compile"),
                tuple("org.apache.tiles","tiles-autotag-core-runtime","jar","1.2","compile"),
                tuple("org.apache.tiles","tiles-request-jsp","jar","1.0.7","compile"),
                tuple("org.apache.tomcat","tomcat-jsp-api","jar","9.0.55","provided"),
                tuple("org.apache.tomcat","tomcat-el-api","jar","9.0.55","provided"),
                tuple("org.apache.tomcat","tomcat-servlet-api","jar","9.0.55","provided"),
                tuple("org.apache.taglibs","taglibs-standard-jstlel","jar","1.2.5","runtime"),
                tuple("org.apache.taglibs","taglibs-standard-spec","jar","1.2.5","runtime"),
                tuple("org.apache.taglibs","taglibs-standard-impl","jar","1.2.5","runtime"),
                tuple("junit","junit","jar","4.13.2","test"),
                tuple("org.hamcrest","hamcrest-core","jar","2.2","test"),
                tuple("org.hamcrest","hamcrest","jar","2.2","test"),
                tuple("org.mockito","mockito-core","jar","4.0.0","test"),
                tuple("net.bytebuddy","byte-buddy","jar","1.11.22","test"),
                tuple("net.bytebuddy","byte-buddy-agent","jar","1.11.22","test"),
                tuple("org.objenesis","objenesis","jar","3.2","compile"),
                tuple("org.springframework","spring-test","jar","5.3.18","test"),
                tuple("org.springframework","spring-core","jar","5.3.18","compile"),
                tuple("org.springframework","spring-jcl","jar","5.3.18","compile"),
                tuple("org.apache.tomcat.embed","tomcat-embed-el","jar","9.0.55","test"),
                tuple("com.example.todo","todo-env","jar","1.0.0-SNAPSHOT","compile"),
                tuple("com.h2database","h2","jar","1.4.200","runtime"))
    }
}