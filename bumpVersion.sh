#!/usr/bin/env bash

function incrementVersion() {
  local version="$1"
  if [ -z "$2" ]; then
    local rgx='^((?:[0-9]+\.)*)([0-9]+)($)'
  else
    local rgx='^((?:[0-9]+\.){'$(($2-1))'})([0-9]+)(\.|$)'
    for (( p=$(grep -o "\."<<<".$version"|wc -l); p<$2; p++)); do
       version+=.0
    done;
  fi
  local -r val=$(echo -e "$version" | perl -pe 's/^.*'$rgx'.*$/$2/')
  echo "$version" | perl -pe s/$rgx.*$'/${1}'$(printf "%0${#val}s" $((val+1)))/
}

function bumpVersion() {
  local -r baseDir=$(dirname "$0")
  local -r propertiesFile="${baseDir}/gradle.properties"
  local -r currentVersion=$(cat < "$propertiesFile" | grep tech.apter.junit5.robolectric.extension.version | cut -d'=' -f2)
  if [[ "$1" == "" ]]; then
    local -r versionSuffix="-SNAPSHOT"
    local -r newVersion=$(incrementVersion "$(echo "$currentVersion" | cut -d'-' -f1)")"$versionSuffix"
  else
    local -r newVersion="$1"
  fi
  sed -ie "s/^tech\.apter\.junit5\.robolectric\.extension\.version=.*/tech.apter.junit5.robolectric.extension.version=${newVersion}/" "$propertiesFile"
  rm -rf "${propertiesFile}e"
  echo "$newVersion"
}

bumpVersion "$@"
