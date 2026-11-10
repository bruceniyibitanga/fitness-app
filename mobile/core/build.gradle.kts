/*
 * :core holds the training rules both apps share: load per rep, estimated
 * one-rep max, pain guidance and skill paths. It is plain Kotlin with no
 * Android or UI code, so it is fast to test and can later be compiled for
 * iOS and watchOS without changes.
 *
 * Today it has one target, the JVM, which Android apps consume directly.
 * The iOS and watchOS targets are added when that work starts (see ADR 0005).
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    // Every public declaration must state its visibility and type on purpose,
    // so nothing becomes part of the shared API by accident.
    explicitApi()

    jvm {
        compilerOptions {
            // Android builds use Java 17 bytecode, so the core matches it.
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
