package com.netflix.nebula.lint.rule.dependency

import com.netflix.nebula.lint.TestKitSpecification
import spock.lang.Subject
import spock.lang.Unroll

@Unroll
@Subject(DeprecatedDependencyConfigurationRule)
class DeprecatedDependencyConfigurationRuleSpec extends TestKitSpecification {

    def 'Replaces deprecated configurations - #configuration for #replacementConfiguration'() {
        buildFile << """
            plugins {
                id 'nebula.lint'
                id 'java'
            }

            gradleLint.rules = ['deprecated-dependency-configuration']

            repositories {
                mavenCentral()
            }
            
            dependencies {
                $configuration 'com.google.guava:guava:19.0'
            }
        """

        when:
        def result = runTasksSuccessfully('autoLintGradle', '--warning-mode=none')

        then:
        result.output.contains("warning   deprecated-dependency-configurationConfiguration $configuration has been deprecated and should be replaced with $replacementConfiguration (no auto-fix available)")

        where:
        configuration | replacementConfiguration
        "compile"     | "implementation"
        "testCompile" | "testImplementation"
        "runtime"     | "runtimeOnly"
    }

    def 'Replaces deprecated configurations - multi project - project dependency - #configuration for #replacementConfiguration'() {
        def sub1 = addSubproject('sub1', """
            repositories {
                mavenCentral()
            }
            
            dependencies {
                $configuration 'com.google.guava:guava:19.0'
                $configuration project(':sub2')
            }

            def x = "test"
            """.stripIndent())

        createJavaSourceFile(sub1, 'public class Sub1 {}')

        def sub2 = addSubproject('sub2', """            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                $configuration 'com.google.guava:guava:19.0'
            }
            """.stripIndent())

        createJavaSourceFile(sub2, 'public class Sub2 {}')

        buildFile << """
            plugins {
                id 'nebula.lint'
            }
            
            subprojects {
                apply plugin: 'java'
                apply plugin: 'nebula.lint'
            }
              
            allprojects {
                  gradleLint.rules = ['deprecated-dependency-configuration']
            }

        """

        when:
        def result = runTasksSuccessfully('autoLintGradle', '--warning-mode=none')

        then:
        result.output.contains("warning   deprecated-dependency-configurationConfiguration $configuration has been deprecated and should be replaced with $replacementConfiguration (no auto-fix available)")

        where:
        configuration | replacementConfiguration
        "compile"     | "implementation"
        "testCompile" | "testImplementation"
        "runtime"     | "runtimeOnly"
    }


    def 'Replaces deprecated configuration - latest release - #configuration for #replacementConfiguration'() {
        buildFile << """
            plugins {
                id 'nebula.lint'
                id 'java'
            }

            gradleLint.rules = ['deprecated-dependency-configuration']

            repositories {
                mavenCentral()
            }
            
            dependencies {
                $configuration 'org.apache.tomcat:tomcat-catalina:latest.release'
            }
        """

        when:
        def result = runTasksSuccessfully('autoLintGradle', '--warning-mode=none')

        then:
        result.output.contains("warning   deprecated-dependency-configurationConfiguration $configuration has been deprecated and should be replaced with $replacementConfiguration (no auto-fix available)")

        where:
        configuration | replacementConfiguration
        "compile"     | "implementation"
        "testCompile" | "testImplementation"
        "runtime"     | "runtimeOnly"
    }

    def 'Replaces deprecated configuration - dynamic version  - #configuration for #replacementConfiguration'() {
        buildFile << """
            plugins {
                id 'nebula.lint'
                id 'java'
            }

            gradleLint.rules = ['deprecated-dependency-configuration']

            repositories {
                mavenCentral()
            }
            
            dependencies {
                $configuration 'org.apache.tomcat:tomcat-catalina:7.+'
            }
        """

        when:
        def result = runTasksSuccessfully('autoLintGradle', '--warning-mode=none')

        then:
        result.output.contains("warning   deprecated-dependency-configurationConfiguration $configuration has been deprecated and should be replaced with $replacementConfiguration (no auto-fix available)")

        where:
        configuration | replacementConfiguration
        "compile"     | "implementation"
        "testCompile" | "testImplementation"
        "runtime"     | "runtimeOnly"
    }

    def 'Replaces deprecated dconfiguration - with excludes - #configuration for #replacementConfiguration'() {
        buildFile << """
            plugins {
                id 'nebula.lint'
                id 'java'
            }

            gradleLint.rules = ['deprecated-dependency-configuration']

            repositories {
                mavenCentral()
            }
            
            dependencies {
                $configuration('org.apache.tomcat:tomcat-catalina:latest.release') {
                    exclude module: "spring-boot-starter-tomcat"
                }
            }
        """

        when:
        def result = runTasksSuccessfully('autoLintGradle', '--warning-mode=none')

        then:
        result.output.contains("warning   deprecated-dependency-configurationConfiguration $configuration has been deprecated and should be replaced with $replacementConfiguration (no auto-fix available)")

        where:
        configuration | replacementConfiguration
        "compile"     | "implementation"
        "testCompile" | "testImplementation"
        "runtime"     | "runtimeOnly"
    }
}
