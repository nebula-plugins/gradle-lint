package com.netflix.nebula.lint.rule.dependency


import nebula.test.IntegrationTestKitSpec
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.DefaultResolvedDependency
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Issue
import spock.lang.Subject
import spock.lang.Unroll

import static com.netflix.nebula.lint.rule.dependency.DependencyClassVisitorSpec.gav

@Subject(DependencyService)
class DependencyServiceSpec extends IntegrationTestKitSpec {
    Project project

    def setup() {
        project = ProjectBuilder.builder().withName('dependency-service').withProjectDir(projectDir).build()
        project.with {
            apply plugin: 'java'
            repositories { mavenCentral() }
        }
    }

    def 'check if configuration is resolved'() {
        setup:
        project.with {
            apply plugin: 'war'
            dependencies {
                implementation 'com.google.guava:guava:19.0'
                providedCompile 'commons-lang:commons-lang:latest.release'
            }
        }

        when:
        def service = DependencyService.forProject(project)
        project.configurations.compileClasspath.resolve()

        then:
        service.isResolved('implementation')
        service.isResolved('providedCompile')
    }

    def 'transitive dependencies with a cycle'() {
        setup:
        def service = DependencyService.forProject(project)

        def resolvedDependency = { String dep ->
            def (group, name, version) = dep.split(':')
            gav(group, name, version)
        }

        when:
        DefaultResolvedDependency a1 = resolvedDependency('a:a:1')
        DefaultResolvedDependency b1 = resolvedDependency('b:b:1')
        a1.addChild(b1)
        b1.addChild(a1)

        def transitives = service.transitiveDependencies(a1)

        then:
        transitives == [b1] as Set
    }
    
    @Unroll
    def 'find unused dependencies'() {
        when:
        buildFile.text = """\
            import com.netflix.nebula.lint.rule.dependency.*

            plugins {
                id 'nebula.lint'
                id 'java'
            }

            repositories { mavenCentral() }
            dependencies {
                implementation 'com.google.inject.extensions:guice-servlet:3.0' // used directly
                implementation 'javax.servlet:servlet-api:2.5' // used indirectly through guice-servlet
                implementation 'commons-lang:commons-lang:2.6' // unused
                testImplementation 'junit:junit:4.11'
                testImplementation 'commons-lang:commons-lang:2.6' // unused
            }

            // a task to generate an unused dependency report for each configuration
            project.configurations.collect { it.name }.each { conf ->
                task "\${conf}Unused"(dependsOn: compileTestJava) {
                    doLast {
                        new File(projectDir, "\${conf}Unused.txt").text = DependencyService.forProject(project)
                        .unusedDependencies(conf)
                        .join('\\n')
                    }
                  
                }
            }
            """.stripMargin()

        writeJavaSourceFile('''\
            import com.google.inject.servlet.*;
            public abstract class Main extends GuiceServletContextListener { }
        ''')

        writeUnitTest()

        then:
        runTasks('implementationUnused')
        new File(projectDir, 'implementationUnused.txt').readLines() == ['commons-lang:commons-lang']

        then:
        runTasks('testImplementationUnused')
        new File(projectDir, 'testImplementationUnused.txt').readLines() == ['commons-lang:commons-lang']
    }

    @Unroll
    def 'find undeclared dependencies'() {
        when:
        buildFile.text = """\
            import com.netflix.nebula.lint.rule.dependency.*

            plugins {
                id 'nebula.lint'
                id 'java'
            }

            repositories { mavenCentral() }
            dependencies {
                implementation 'io.springfox:springfox-core:2.0.2'
            }

            // a task to generate an undeclared dependency report for each configuration
            project.configurations.collect { it.name }.each { conf ->
                task "\${conf}Undeclared"(dependsOn: compileTestJava) {
                    doLast {
                         new File(projectDir, "\${conf}Undeclared.txt").text = DependencyService.forProject(project)
                        .undeclaredDependencies(conf)
                        .join('\\n')
                    }
                }
            }
            """.stripMargin()

        writeJavaSourceFile('''\
            import com.google.common.collect.*;
            public class Main { Multimap m = HashMultimap.create(); }
        ''')

        then:
        runTasks('compileClasspathUndeclared')
        new File(projectDir, 'compileClasspathUndeclared.txt').readLines() == ['com.google.guava:guava:18.0']
    }

    def 'first level dependencies in conf'() {
        when:
        project.with {
            dependencies {
                compile 'com.google.guava:guava:18.0'
                testCompile 'junit:junit:latest.release'
            }
        }

        def deps = DependencyService.forProject(project).firstLevelDependenciesInConf(project.configurations.testCompile, 'testCompile')

        project.configurations.compile.incoming.afterResolve {
            project.configurations.compile.incoming.resolutionResult.root.dependencies
        }

        then:
        deps.size() == 1
        deps[0].module.toString() == 'junit:junit'
        deps[0].version != 'latest.release' // the version has been resolved to a fixed version
    }

    def 'find the nearest source set to a configuration'() {
        when:
        project.with {
            apply plugin: 'war' // to define the providedCompile conf
            configurations {
                deeper
                deep { extendsFrom deeper }
            }
            configurations.compile { extendsFrom configurations.deep }
        }

        def service = DependencyService.forProject(project)

        then:
        service.sourceSetByConf('compile')?.name == 'main'
        service.sourceSetByConf('providedCompile')?.name == 'main'
        service.sourceSetByConf('deeper')?.name == 'main'
    }

    @Issue('39')
    def 'compile sourceSet is not mixed up with integTest class output'() {
        setup:
        buildFile.text = """
            import com.netflix.nebula.lint.rule.dependency.*

            plugins {
                id 'java'
                id 'nebula.integtest' version '7.0.7'
                id 'nebula.lint'
            }
            
            task compileClasspathSourceSetOutput {
                doLast {
                  println('@@' + DependencyService.forProject(project).sourceSetByConf('compileClasspath').java.outputDir)
                }
            }
            
            task integTestSourceSetOutput {
                doLast {
                   println('@@' + DependencyService.forProject(project).sourceSetByConf('integTestCompileClasspath').java.outputDir)
                }
            }
        """

        when:
        def results = runTasks('compileClasspathSourceSetOutput')
        def dir = results.output.readLines().find { it.startsWith('@@')}.substring(2)

        then:
        dir.split('/')[-1] == 'main'

        when:
        results = runTasks('integTestSourceSetOutput')
        dir = results.output.readLines().find { it.startsWith('@@')}.substring(2)

        then:
        dir.split('/')[-1] == 'integTest'        
    }

    def 'identify parent source sets'() {
        expect:
        DependencyService.forProject(project).parentSourceSetConfigurations('compile')*.name == ['testCompile']
    }

    @Unroll
    def 'read jar contents of project dependencies'() {
        setup:
        def core = addSubproject('core')
        def web = addSubproject('web')

        buildFile << """
            plugins {
                id 'nebula.lint'
            }

            subprojects {
                apply plugin: 'java'
            }
        """

        new File(web, 'build.gradle').text = """
            import com.netflix.nebula.lint.rule.dependency.*

            dependencies {
                implementation project(':core')
            }

            task coreContents {
                doLast {
                    new File(projectDir, "coreContents.txt").text = DependencyService.forProject(project)
                    .jarContents(configurations.compileClasspath.resolvedConfiguration.firstLevelModuleDependencies[0].module.id.module)
                    .classes
                    .join('\\n')
                }
            }
        """

        writeJavaSourceFile('public class A {}', core)

        when:
        runTasks(*tasks)
        def coreContents = new File(web, 'coreContents.txt')

        then:
        coreContents.readLines() == contents

        where:
        tasks                                   | contents
        ['web:coreContents']                    | []
        ['web:assemble', 'web:coreContents']    | ['A']
    }

    def 'resolved configurations returns lists of configurations that are resolvable and resolved'() {
        given:
        definePluginOutsideOfPluginBlock = true
        def dependency = 'commons-lang:commons-lang:latest.release'

        buildFile << """
            apply plugin: 'java-library'

            repositories { jcenter() }

            dependencies {
                compile '${dependency}'
                compileOnly '${dependency}'
                testCompile '${dependency}'
                testCompileOnly '${dependency}'

                implementation '${dependency}'
                testImplementation '${dependency}'

                apiElements '${dependency}'

                runtimeElements '${dependency}'

                runtime '${dependency}'
                testRuntime '${dependency}'

                runtimeOnly '${dependency}'
                testRuntimeOnly '${dependency}'
            }
            
            import com.netflix.nebula.lint.rule.dependency.*
            
            task resolvableAndResolvedConfigurations {
                doLast {
                    new File(projectDir, "resolvableAndResolvedConfigurations.txt").text = DependencyService.forProject(project)
                    .resolvableAndResolvedConfigurations()
                    .join('\\n')
                }
            }
            """.stripIndent()

        writeJavaSourceFile('public class Main {}')
        writeUnitTest('public class TestMain {}')

        def dependencyService = DependencyService.forProject(project)
        dependencyService.resolvableConfigurations().each { resolvableConf ->
            resolvableConf.resolve()
        }

        when:
        def resolvedConfigurations = dependencyService.resolvableAndResolvedConfigurations()

        then:
        def configurationNames = resolvedConfigurations.collect { conf -> conf.getName() }

        configurationNames.size() > 0

        // sample the configurations
        configurationNames.contains('compileClasspath')
        configurationNames.contains('testCompileClasspath')

        !configurationNames.contains('compile')
        !configurationNames.contains('testCompile')

        !configurationNames.contains('implementation')
        !configurationNames.contains('testImplementation')

        !configurationNames.contains('compileOnly')
        !configurationNames.contains('testCompileOnly')

        !configurationNames.contains('apiElements')
        !configurationNames.contains('runtimeElements')

        configurationNames.contains('runtimeClasspath')
        configurationNames.contains('testRuntimeClasspath')

        !configurationNames.contains('runtime')
        !configurationNames.contains('testRuntime')

        !configurationNames.contains('runtimeOnly')
        !configurationNames.contains('testRuntimeOnly')

        // and more!
    }
}