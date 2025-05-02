plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

dependencies {
    implementation(project(":common"))
    implementation("org.openjfx:javafx-controls:21.0.6")
    implementation("org.openjfx:javafx-fxml:21.0.6")
    implementation("org.openjfx:javafx-media:21.0.6")
}

javafx {
    version = "21.0.6"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml",
        "javafx.media"
    )
}

application {
    mainClass.set("com.studybuddy.client.MainApp")
}

/*  ➤ macOS 첫-스레드 옵션 추가  */
tasks.withType<JavaExec>().configureEach {
    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }
}
