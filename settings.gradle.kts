plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "mini_project"

// 우리가 실제로 사용할 모듈들만 include
include(
    "common",
    "server",
    "client",
    "test"
)
