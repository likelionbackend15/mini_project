dependencies {
    implementation("junit:junit:4.13.1")
    implementation("junit:junit:4.13.1")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
}
plugins { java }
tasks.test { useJUnitPlatform() }
