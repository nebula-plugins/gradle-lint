#!/bin/bash
# This script will build the project.

SWITCHES="--info --stacktrace"

GRADLE_VERSION=$(./gradlew -version | grep Gradle | cut -d ' ' -f 2)

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo -e "Build Pull Request #$TRAVIS_PULL_REQUEST => Branch [$TRAVIS_BRANCH]"
  ./gradlew build $SWITCHES
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" == "" ]; then
  echo -e 'Build Branch with Snapshot => Branch ['$TRAVIS_BRANCH']'
  ./gradlew build $SWITCHES
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then
  echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
  case "$TRAVIS_TAG" in
  *-rc\.*)
    ./gradlew -Prelease.travisci=true -Psonatype.username=$sonatypeUsername -Psonatype.password=$sonatypePassword -Pbintray.user=$bintrayUser -Pbintray.apiKey=$bintrayKey  -Prelease.useLastTag=true candidate $SWITCHES --refresh-dependencies
    ;;
  *)
    ./gradlew -Prelease.travisci=true -Dgradle.publish.key=$gradlePluginPublishKey -Dgradle.publish.secret=$gradlePluginPublishSecret -Psonatype.username=$sonatypeUsername -Psonatype.password=$sonatypePassword -Pbintray.user=$bintrayUser -Pbintray.apiKey=$bintrayKey  -Prelease.useLastTag=true final $SWITCHES --refresh-dependencies
    ;;
  esac
else
  echo -e 'WARN: Should not be here => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']  Pull Request ['$TRAVIS_PULL_REQUEST']'
  ./gradlew build $SWITCHES
fi

EXIT=$?

rm -f "$HOME/.gradle/caches"
rm -rf "$HOME/.gradle/cachesn"

exit $EXIT
