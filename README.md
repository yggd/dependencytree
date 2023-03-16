## Obtain a list of dependent libraries from Maven's POM file.

It is primarily intended to be used within test cases.

### Install

* Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.yggd</groupId>
        <artifactId>dependencytree</artifactId>
        <version>1.0.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

* Gradle

```groovy
dependencies {
    test 'org.yggd:dependencytree:1.0.0'
}
```

### Usage

#### Get a tree of dependent libraries.

* Java

```java
DependencyReader dependencyReader = new DependencyReader();
Set<EffectiveDependency> dependencies = dependencyReader.transitDependency(new File("projectpath/pom.xml"));
```

* Kotlin

```kotlin
val dependencyReader = DependencyReader(profile)
val dependencies = dependencyReader.transitDependency(File("projectpath/pom.xml"))
```

#### Get a flatten list of dependent libraries.

* Java

```java
DependencyReader dependencyReader = new DependencyReader();
Set<EffectiveDependency> flatten = dependencyReader.flatten(new File("projectpath/pom.xml"));
```

* Kotlin

```kotlin
val dependencyReader = DependencyReader()
var flatten = dependencyReader.flatten(File("projectpath/pom.xml"))
```

### MISC

For more detailed usage, please refer to the test case: `DependencyReaderTest`.

#### The following profiles can be specified by `-P` in the `mvn` command and the JDK at runtime.
- Java version: If not specified, the version of Java in use (system property: `java.specification.version`) is used.
- Profile ID: String that can be specified for `id` in the profile.

* Java

```java
ActiveProfile.Builder builder = new ActiveProfile.Builder();
ActiveProfile profile = builder
        .jdk("1.8")
        .profile("profileA")
        .profile("profileB")
        .build();
DependencyReader dependencyReader = DependencyReader(profile);
Set<EffectiveDependency> dependencies = dependencyReader.transitDependency(new File("projectpath/pom.xml"));
```

* Kotlin

```kotlin
val profile = ActiveProfile.Builder()
      .jdk("1.8")
      .profile("profileA")
      .profile("profileB")
      .build()
val dependencyReader = DependencyReader(profile)
val dependencies = dependencyReader.transitDependency(File("projectpath/pom.xml"))
```

#### Load a multi-modularized POM. (Specify the ROOT-POM file with `<modules>`)

* Java

```java
DependencyReader dependencyReader = new DependencyReader();
// key string is module name.
Map<String, Set<EffectiveDependency>> dependencyMap = dependencyReader.flattenMultiModule(File("multi-moduled-projectpath/pom.xml"));
```

* Kotlin

```kotlin
val dependencyReader = DependencyReader()
val modules = dependencyReader.flattenMultiModule(File("multi-moduled-projectpath/pom.xml"))
```

### License

* Apache License, Version 2.0
  - https://www.apache.org/licenses/LICENSE-2.0
* This production uses the following TERASOLUNA Framework archetypes as test data.
  - https://github.com/terasolunaorg/terasoluna-gfw-web-multi-blank
  - https://github.com/terasoluna-batch/v5-sample
