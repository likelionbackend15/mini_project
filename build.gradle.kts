plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
        implementation("mysql:mysql-connector-java:8.1.0")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
