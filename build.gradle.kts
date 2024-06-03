plugins {
    kotlin("js") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation(npm("jszip", "3.10.1"))
    implementation(npm("localforage", "1.10.0"))
    implementation(npm("vis-network", "9.1.2"))
    implementation(npm("vis-data", "7.1.4"))
    testImplementation(kotlin("test"))
}

kotlin {
    js {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
            }
            testTask {
                useMocha()
            }
        }
    }
}