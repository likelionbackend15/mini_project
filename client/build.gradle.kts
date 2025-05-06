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
    /** macOS 창이 안 뜨는 현상 방지 */
    applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")   // ★ 이 옵션이 run 태스크에 자동 전파됨
}

sourceSets {
    main {
        resources.srcDirs("src/main/resources")
    }
}

tasks.withType<JavaExec>().configureEach {
    val fxLib = "/Users/songchiho/Library/Java/JavaVirtualMachines/javafx-sdk-21.0.6/lib"
    /*
    *                 위의 fx주소 꼭 각자 환경에 맞게 수정하시고 gradle reload 하세요!!!!!!!
    *
    *
    * */
    jvmArgs = listOf(
        "--module-path", fxLib,
        "--add-modules", "javafx.controls,javafx.fxml,javafx.media"
    )
}

