#!/usr/bin/env bash

function commitPropertiesFile() {
  local -r newVersion="$1"
  local -r propertiesFile="${baseDir}/gradle.properties"
  git add "$propertiesFile"
  git commit -m "Bump version to $newVersion"
}

function tagRelease() {
  local -r baseDir=$(dirname "$0")
  local -r newVersion="$1"
  local -r propertiesFile="${baseDir}/gradle.properties"

  "$baseDir"/bumpVersion.sh "$newVersion"
  commitPropertiesFile "$newVersion"

  git tag -a "$newVersion" -m "Release $newVersion"

  local -r newSnapshotVersion=$("$baseDir"/bumpVersion.sh)
  commitPropertiesFile "$newSnapshotVersion"

  git push origin main
  git push origin "$newVersion"
}

tagRelease "$@"
