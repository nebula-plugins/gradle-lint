# Gradle Lint Plugin

[![Join the chat at https://gitter.im/nebula-plugins/gradle-lint-plugin](https://badges.gitter.im/nebula-plugins/gradle-lint-plugin.svg)](https://gitter.im/nebula-plugins/gradle-lint-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/nebula-plugins/gradle-lint-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/gradle-lint-plugin)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/gradle-lint-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-lint-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Purpose

The Gradle Lint plugin is a pluggable and configurable linter tool for identifying and reporting on patterns of misuse or deprecations in Gradle scripts.  It is inspired by the excellent ESLint tool for Javascript and by the formatting in NPM's [eslint-friendly-formatter](https://www.npmjs.com/package/eslint-friendly-formatter) package.

It assists a centralized build tools team in gently introducing and maintaining a standard build script style across their organization.

## Usage

To apply this plugin:

    plugins {
      id 'nebula.lint' version '0.1.0'
    }

Alternatively:

    buildscript {
      repositories { jcenter() }
      dependencies {
        classpath 'com.netflix.nebula:gradle-lint-plugin:latest.release'
      }
    }

    apply plugin: 'nebula.lint'

Define which rules you would like to lint against:

    lint.rules = ['dependency-parentheses', 'dependency-tuple', ...]

For an enterprise build, we recommend defining the lint rules in a `init.gradle` script or in a gradle script that is included via the Gradle `apply from` mechanism.

For multimodule projects, we recommend applying the plugin in an allprojects block:

    allprojects {
      apply plugin: 'nebula.lint'
      lint.rules = ['dependency-parentheses', 'dependency-tuple', ...]
    }

## Tasks

Run `./gradlew lint` to execute the linting process.

![gradle-lint output](docs/images/lint-output.png)

Run `./gradlew autoCorrectLint` to apply automatically fix your build scripts!

## License

Copyright 2014-2015 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
