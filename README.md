# services-loader initialization optimization 
优化service-loader动态加载耗时问题，支持AGP 8.0+，适配版本较高

## service-compiler 
java、kt的注解编译器，支持APT + Javapoet实现，依赖注解声明@AutoService

## service-kt-compiler 
kt的注解编译器，支持KSP实现,依赖注解声明@AutoService
### 配置信息
```
id("com.google.devtools.ksp") version "1.9.21-1.0.16"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")
    // KSP API
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.21-1.0.16")
    // 代码生成工具
    implementation("com.squareup:kotlinpoet-ksp:1.16.0")
    implementation("com.squareup:kotlinpoet:1.16.0")
}

```

## service-loader-plugin
service-loader插件，services注册器

## service-register
services管理器，业务方使用api定义

## 使用方式
```
> root build.gradle

    dependencies {
        classpath("com.github.raingift:service-loader-plugin:1.0.0")
    }

> sub build.gradle

   plugins {
      id("io.github.raingitft.ServiceLoaderPlugin")
   }

    dependencies {
        implementation(libs.auto.service.annotations)
        kapt("com.github.raingift:service-compiler:1.0.0")
        implementation("com.github.raingift:service-register:1.0.0")
    }

```