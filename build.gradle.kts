plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.12.0"
    id("checkstyle")
}

group = "com.zhongan"
version = "3.0.1"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.1.4")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("com.intellij.java", "org.jetbrains.idea.maven", "Git4Idea"))
}

dependencies {
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.10.0")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("io.github.bonede:tree-sitter:0.22.6")
    implementation("io.github.bonede:tree-sitter-java:0.21.0a")
    implementation("io.github.bonede:tree-sitter-python:0.21.0a")
    implementation("io.github.bonede:tree-sitter-go:0.21.0a")
    implementation("com.knuddels:jtokkit:1.0.0")
    implementation("org.apache.maven.shared:maven-shared-utils:3.4.2")
    compileOnly("com.puppycrawl.tools:checkstyle:10.9.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.encoding = "UTF-8"
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("252.*")

        pluginDescription.set(provider { file("description.html").readText() })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        systemProperty("devpilot.env", "test")
    }

    checkstyle {
        configFile = rootProject.file("checkstyle.xml")
        maxWarnings = 0
    }
}
