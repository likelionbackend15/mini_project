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
        implementation("org.slf4j:slf4j-api:2.0.12")
        runtimeOnly("ch.qos.logback:logback-classic:1.4.12")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
