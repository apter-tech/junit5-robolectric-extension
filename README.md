# JUnit5 Robolectric Extension (Experimental)

[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/apter-tech/junit5-robolectric-extension/build.yml?branch=main)](https://github.com/apter-tech/junit5-robolectric-extension/actions/workflows/build.yml?query=branch%3Amain)
[![Maven Central Version](https://img.shields.io/maven-central/v/tech.apter.junit5.jupiter/robolectric-extension)](https://central.sonatype.com/search?q=tech.apter.junit5.jupiter)
[![Gradle Plugin Portal Version](https://img.shields.io/gradle-plugin-portal/v/tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin)](https://plugins.gradle.org/plugin/tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

This is an experimental project that aims to bridge the gap between JUnit 5 and Robolectric,
providing a way to run your Android unit tests using the modern JUnit 5 framework while leveraging
Robolectric's in-memory environment.

## Key features

* **JUnit 5 Compatibility:** Run your tests with the latest JUnit 5 features and syntax.

## Current Limitations

* **Parallel Execution:** Parallel test execution only supported with classes.
* **Configuration:**
    * Robolectric `@Config`'s sdk parameter annotation can only be set on outermost test class.
    * `@ResourcesMode`, `@LooperMode`, `GraphicsMode` annotations can only be set on outermost test
      class.
* **Experimental Status:** This extension is still under development, and its API might change in
  future versions.

## Installation

1. Add the Gradle Plugin Portal and Maven Central and Google's Maven repository to your project's
   `settings.gradle` file:

<details open>
<summary>Kotlin</summary>

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}
```

</details>

2. Enable JUnit Platform Launcher Interceptors and add the dependency to your app or library
   module's `build.gradle`:

<details open>
<summary>Kotlin</summary>

```kotlin
plugins {
    id("tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin") version ("<latest.release>")
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
plugins {
    id 'tech.apter.junit5.jupiter.robolectric-extension-gradle-plugin' version '<latest.release>'
}
```

</details>

## Basic usage

1. Annotate your test class with `@ExtendWith`. This extension will manage the Robolectric
   environment for your tests:

<details open>
<summary>Kotlin</summary>

```kotlin
@ExtendWith(RobolectricExtension::class)
```

</details>

<details>
<summary>Java</summary>

```java
@ExtendWith(RobolectricExtension.class)
```

</details>

2. Utilize the standard JUnit 5 annotations (`@Test`, `@BeforeEach`, `@AfterEach`, etc.) within your
   test methods. You could also use `org.jetbrains.kotlin:kotlin-test-junit5` package if you want
   to.

<details open>
<summary>Kotlin</summary>

```kotlin

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(application = RobolectricExtensionSelfTest.MyTestApplication::class)
class RobolectricExtensionSelfTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun shouldInitializeAndBindApplicationAndCallOnCreate() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        assertInstanceOf(MyTestApplication::class.java, application)
        assertTrue((application as MyTestApplication).onCreateWasCalled)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setUpClass() {
        }

        @AfterAll
        @JvmStatic
        fun tearDownClass() {
        }
    }

    class MyTestApplication : Application() {
        internal var onCreateWasCalled = false

        override fun onCreate() {
            this.onCreateWasCalled = true
        }
    }
}

```

</details>

<details>
<summary>Java</summary>

```java

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.robolectric.annotation.Config;

import tech.apter.junit.jupiter.robolectric.RobolectricExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(RobolectricExtension.class)
@Config(application = RobolectricExtensionSelfTest.MyTestApplication::class)
public class RobolectricExtensionSelfTest {

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void shouldInitializeAndBindApplicationAndCallOnCreate() {
        final Application application = ApplicationProvider.getApplicationContext();
        assertInstanceOf(MyTestApplication.class, application);
        assertTrue(((MyTestApplication) application).onCreateWasCalled);
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    static class MyTestApplication extends Application {
        public boolean onCreateWasCalled = false;

        @Override
        public void onCreate() {
            this.onCreateWasCalled = true;
        }
    }
}

```

</details>
