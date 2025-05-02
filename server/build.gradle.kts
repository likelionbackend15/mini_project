plugins { java }

dependencies {
    // 공용(common) 모듈을 서버(server) 모듈에서 사용하겠다
    implementation(project(":common"))

    // + JDBC 드라이버, Jackson 등 다른 라이브러리
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("org.mindrot:jbcrypt:0.4")   // bcrypt 해시 검증
}
