plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.12.0"
    id("checkstyle")
}

group = "com.zhongan"
version = "2.1.0"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.1.4")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

dependencies {
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.10.0")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
    compileOnly("com.puppycrawl.tools:checkstyle:10.9.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("233.*")

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

    checkstyle {
        configFile = rootProject.file("checkstyle.xml")
        maxWarnings = 0
    }
}
