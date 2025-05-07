// 공통 DTO·유틸
plugins { `java-library` }

dependencies {
    // Jackson core
    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    // ✅ Java 8 Date/Time 지원 모듈 (LocalDateTime ↔ ISO-8601)
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
}


