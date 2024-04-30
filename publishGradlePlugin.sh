#!/usr/bin/env bash

function publishGradlePlugin() {
  ./gradlew :robolectric-extension-gradle-plugin:publishToMavenLocal --configure-on-demand
  local -r robolectricGradlePluginVersion=$(cat < gradle.properties | grep tech.apter.junit5.robolectric.extension.version | cut -d'=' -f2)

  if [[ "$robolectricGradlePluginVersion" != *SNAPSHOT ]]; then
    ./gradlew :robolectric-extension-gradle-plugin:publishPlugins \
      --configure-on-demand \
      -D"gradle.publish.key=${JUNIT5_ROBOLECTRIC_EXTENSION_GRADLE_PLUGIN_PORTAL_KEY}" \
      -D"gradle.publish.secret=${JUNIT5_ROBOLECTRIC_EXTENSION_GRADLE_PLUGIN_PORTAL_SECRET}"
  fi
}

publishGradlePlugin
