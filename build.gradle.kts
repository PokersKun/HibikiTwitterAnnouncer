plugins {
    val kotlinVersion = "1.4.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.8.2"
}

group = "org.example"
version = "1.1.2"

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    mavenCentral()
}

dependencies {
    val fuel_version = "2.3.1"
    implementation("com.alibaba:fastjson:1.2.78")
    implementation("com.cloudconvert:cloudconvert-java:1.1.0")
    implementation("com.github.kittinunf.fuel:fuel:$fuel_version")
    implementation("com.github.kittinunf.fuel:fuel-gson:$fuel_version")
    implementation("com.google.code.gson:gson:2.8.9")
}