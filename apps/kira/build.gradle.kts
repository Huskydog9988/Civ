plugins {
    kotlin("jvm") version "1.7.10"
    application
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    implementation("org.json:json:20160810")
    implementation("org.apache.logging.log4j:log4j-api:2.18.0")
    implementation("org.apache.logging.log4j:log4j-core:2.18.0")
    implementation("net.dv8tion:JDA:4.4.0_350")
    implementation("com.zaxxer:HikariCP:2.4.6")
    implementation("org.postgresql:postgresql:42.2.5")
    implementation("com.rabbitmq:amqp-client:5.6.0")
    implementation("org.jsoup:jsoup:1.14.2")
    implementation("org.java-websocket:Java-WebSocket:1.5.0")
    implementation("org.apache.commons:commons-collections4:4.4")
}

application {
    mainClass.set("com.github.maxopoly.kira.KiraMain")
}
