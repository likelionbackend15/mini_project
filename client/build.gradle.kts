plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

dependencies {
    implementation(project(":common"))
    implementation("org.openjfx:javafx-controls:20.0.2")
    implementation("org.openjfx:javafx-fxml:20.0.2")
    implementation("org.openjfx:javafx-media:20.0.2")
}

javafx {
    version = "20.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("com.yourname.miniproject.client.MainApp")
}
