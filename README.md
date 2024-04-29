# JUnit5 Robolectric Extension (Experimental)

This is an experimental project that aims to bridge the gap between JUnit 5 and Robolectric,
providing a way to run your
Android unit tests using the modern JUnit 5 framework while leveraging Robolectric's
in-memory environment.

## Key features

* **JUnit 5 Compatibility:** Run your tests with the latest JUnit 5 features and syntax.

## Current Limitations

* **Parallel Execution:** Parallel test execution is not yet supported. We're actively working on
  addressing this limitation in future releases.
* **Configuration:**
  * Robolectric `@Config`'s sdk parameter annotation can only be set on most outer test class.
  * `@ResourcesMode`, `@LooperMode`, `GraphicsMode` annotations can only be set on most outer test class.
* **Experimental Status:** This extension is still under development, and its API might change in
  future versions.

## Installation

1. Add the Maven Central repository to your project's `build.gradle`:

<details open>
<summary>Kotlin</summary>

```kotlin
repositories {
    mavenCentral()
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
repositories {
    mavenCentral()
}
```

</details>

2. Enable JUnit Platform Launcher Interceptors and add the dependency to your app or library
   module's `build.gradle`:

<details open>
<summary>Kotlin</summary>

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.useJUnitPlatform()
                test.jvmArgs(
                    listOf(
                        "-Djunit.platform.launcher.interceptors.enabled=true",
                        "--add-exports", "java.base/jdk.internal.loader=ALL-UNNAMED",
                        "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED",
                    )
                )
            }
        }
    }
}

dependencies {
    testImplementation("tech.apter.junit.jupiter:robolectric-extension:<latest.release>")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:<latest.release>") // JUnit 5 Jupiter Engine
    // Optional dependencies if you want to use different version than used by the extension
    testImplementation("org.junit.jupiter:junit-jupiter-api:<latest.release>") // Latest JUnit 5 Jupiter API
    testImplementation("org.robolectric:robolectric:<latest.release>") // Latest Robolectric version
}
```

</details>

<details>
<summary>Groovy</summary>

```groovy
android {
    testOptions {
        unitTests {
            includeAndroidResources = true
            all {
                useJUnitPlatform()
                jvmArgs(
                    '-Djunit.platform.launcher.interceptors.enabled=true',
                    '--add-exports', 'java.base/jdk.internal.loader=ALL-UNNAMED',
                    '--add-opens', 'java.base/jdk.internal.loader=ALL-UNNAMED',
                )
            }
        }
    }
}

dependencies {
    testImplementation 'tech.apter.junit.jupiter:robolectric-extension:0.2.0'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:<latest.release>'
    // Latest JUnit 5 Jupiter Engine
    // Optional dependencies if you want to use different versions than used by the extension
    testImplementation 'org.junit.jupiter:junit-jupiter-api:<latest.release>' // Latest JUnit 5 Jupiter API
    testImplementation 'org.robolectric:robolectric:<latest.release>' // Latest Robolectric version
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
   test methods. You
   could also use `org.jetbrains.kotlin:kotlin-test-junit5` package if you want to.

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

## Important Notes

* Ensure `isIncludeAndroidResources` is set to true in your testOptions configuration to access
  Android resources in your tests.
* JUnit Platform Launcher Interceptors must be
  enabled (`junit.platform.launcher.interceptors.enabled=true`), otherwise
  test instances will not be created by Robolectric's classloader.
* Parallel execution is currently not supported. Run tests sequentially for now.

