/*
 * Copyright 2015-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.gradle.plugin.publishing

import grails.util.GrailsNameUtils
import groovy.namespace.QName
import io.github.gradlenexus.publishplugin.InitializeNexusStagingRepository
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.PluginManager
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

import static com.bmuschko.gradle.nexus.NexusPlugin.getSIGNING_KEY_ID
import static com.bmuschko.gradle.nexus.NexusPlugin.getSIGNING_PASSWORD
import static com.bmuschko.gradle.nexus.NexusPlugin.getSIGNING_KEYRING

/**
 * A plugin to ease publishing Grails related artifacts
 *
 * @author Graeme Rocher
 * @author James Daugherty
 * @since 3.1
 */
class GrailsPublishGradlePlugin implements Plugin<Project> {

    public static String NEXUS_PUBLISH_PLUGIN_ID = 'io.github.gradle-nexus.publish-plugin'
    public static String MAVEN_PUBLISH_PLUGIN_ID = 'maven-publish'
    public static String SIGNING_PLUGIN_ID = 'signing'
    public static String ENVIRONMENT_VARIABLE_BASED_RELEASE = 'GRAILS_PUBLISH_RELEASE'

    String getErrorMessage(String missingSetting) {
        return """No '$missingSetting' was specified. Please provide a valid publishing configuration. Example:

grailsPublish {
    websiteUrl = 'https://example.com/myplugin'
    license {
        name = 'Apache-2.0'
    }
    issueTrackerUrl = 'https://github.com/myname/myplugin/issues'
    vcsUrl = 'https://github.com/myname/myplugin'
    title = 'My plugin title'
    desc = 'My plugin description'
    developers = [johndoe: 'John Doe']
}

or

grailsPublish {
    githubSlug = 'foo/bar'
    license {
        name = 'Apache-2.0'
    }
    title = 'My plugin title'
    desc = 'My plugin description'
    developers = [johndoe: 'John Doe']
}

By default snapshotPublishType is set to MAVEN_PUBLISH and releasePublishType is set to NEXUS_PUBLISH.

The credentials and connection url must be specified as a project property or an environment variable:

`MAVEN_PUBLISH` Environment Variables are:
    MAVEN_PUBLISH_USERNAME
    MAVEN_PUBLISH_PASSWORD
    MAVEN_PUBLISH_URL

`NEXUS_PUBLISH` Environment Variables are:
    NEXUS_PUBLISH_USERNAME
    NEXUS_PUBLISH_PASSWORD
    NEXUS_PUBLISH_URL
    NEXUS_PUBLISH_SNAPSHOT_URL
    NEXUS_PUBLISH_STAGING_PROFILE_ID

When using `NEXUS_PUBLISH`, the property `signing.secretKeyRingFile` must be set to the path of the GPG keyring file.

Note: if project properties are used, the properties must be defined prior to applying this plugin.
"""
    }

    @Override
    void apply(Project project) {
        project.rootProject.logger.lifecycle("Applying Grails Publish Gradle Plugin for `${project.name}`...");

        final ExtensionContainer extensionContainer = project.extensions
        final TaskContainer taskContainer = project.tasks
        final GrailsPublishExtension gpe = extensionContainer.create('grailsPublish', GrailsPublishExtension)

        final String mavenPublishUsername = project.findProperty('mavenPublishUsername') ?: System.getenv('MAVEN_PUBLISH_USERNAME') ?: ''
        final String mavenPublishPassword = project.findProperty('mavenPublishPassword') ?: System.getenv('MAVEN_PUBLISH_PASSWORD') ?: ''
        // the maven publish url can technically be a directory so do not force to String type
        final def mavenPublishUrl = project.findProperty('mavenPublishUrl') ?: System.getenv('MAVEN_PUBLISH_URL') ?: ''

        final String nexusPublishUrl = project.findProperty('nexusPublishUrl') ?: System.getenv('NEXUS_PUBLISH_URL') ?: ''
        final String nexusPublishSnapshotUrl = project.findProperty('nexusPublishSnapshotUrl') ?: System.getenv('NEXUS_PUBLISH_SNAPSHOT_URL') ?: ''
        final String nexusPublishUsername = project.findProperty('nexusPublishUsername') ?: System.getenv('NEXUS_PUBLISH_USERNAME') ?: ''
        final String nexusPublishPassword = project.findProperty('nexusPublishPassword') ?: System.getenv('NEXUS_PUBLISH_PASSWORD') ?: ''
        final String nexusPublishStagingProfileId = project.findProperty('nexusPublishStagingProfileId') ?: System.getenv('NEXUS_PUBLISH_STAGING_PROFILE_ID') ?: ''

        final ExtraPropertiesExtension extraPropertiesExtension = extensionContainer.findByType(ExtraPropertiesExtension)

        extraPropertiesExtension.setProperty(SIGNING_KEY_ID, project.findProperty(SIGNING_KEY_ID) ?: System.getenv('SIGNING_KEY'))
        extraPropertiesExtension.setProperty(SIGNING_PASSWORD, project.findProperty(SIGNING_PASSWORD) ?: System.getenv('SIGNING_PASSPHRASE'))
        extraPropertiesExtension.setProperty(SIGNING_KEYRING, project.findProperty(SIGNING_KEYRING) ?: System.getenv('SIGNING_KEYRING'))

        PublishType snapshotPublishType = gpe.snapshotPublishType
        PublishType releasePublishType = gpe.releasePublishType

        boolean isSnapshot, isRelease
        if (System.getenv(ENVIRONMENT_VARIABLE_BASED_RELEASE) != null) {
            // Detect release state based on environment variables instead of versions
            isRelease = Boolean.parseBoolean(System.getenv(ENVIRONMENT_VARIABLE_BASED_RELEASE))
            isSnapshot = !isRelease

            project.rootProject.logger.lifecycle("Environment Variable `$ENVIRONMENT_VARIABLE_BASED_RELEASE` detected - using variable instead of project version.")
        } else {
            String detectedVersion = (project.version == Project.DEFAULT_VERSION ? (project.findProperty('projectVersion') ?: Project.DEFAULT_VERSION) : project.version) as String
            if (detectedVersion == Project.DEFAULT_VERSION) {
                throw new IllegalStateException("Project ${project.name} has an unspecified version (neither `version` or the property `projectVersion` is defined). Release state cannot be determined.")
            }
            if (project.version == Project.DEFAULT_VERSION) {
                project.rootProject.logger.warn("Project ${project.name} does not have a version defined. Using the gradle property `projectVersion` to assume version is ${detectedVersion}.")
            }
            project.rootProject.logger.info("Version $detectedVersion detected for project ${project.name}")

            isSnapshot = detectedVersion.endsWith('SNAPSHOT')
            isRelease = !isSnapshot
        }

        if (isSnapshot) {
            project.rootProject.logger.info("Project ${project.name} will be a snapshot.")
        }
        if (isRelease) {
            project.rootProject.logger.info("Project ${project.name} will be a release.")
        }

        boolean useMavenPublish = (isSnapshot && snapshotPublishType == PublishType.MAVEN_PUBLISH) || (isRelease && releasePublishType == PublishType.MAVEN_PUBLISH)
        if (useMavenPublish) {
            project.rootProject.logger.info("Maven Publish is enabled for project ${project.name}")
        }
        boolean useNexusPublish = (isSnapshot && snapshotPublishType == PublishType.NEXUS_PUBLISH) || (isRelease && releasePublishType == PublishType.NEXUS_PUBLISH)
        if (useNexusPublish) {
            project.rootProject.logger.info("Nexus Publish is enabled for project ${project.name}")
        }

        // Required for the pom always
        final PluginManager projectPluginManager = project.pluginManager
        projectPluginManager.apply(MavenPublishPlugin)

        if (isRelease || useNexusPublish) {
            if (project.pluginManager.hasPlugin(SIGNING_PLUGIN_ID)) {
                project.logger.debug("Signing Plugin already applied to project ${project.name}")
            } else {
                projectPluginManager.apply(SigningPlugin)
            }

            project.tasks.withType(Sign).configureEach { Sign task ->
                task.onlyIf { isRelease }
            }
        }

        if (useNexusPublish) {
            // The nexus plugin is special since it must always be applied to the root project.
            // Handle when multiple subprojects exist and grailsPublish is defined in each one instead of at the root.
            final PluginManager rootProjectPluginManager = project.rootProject.pluginManager
            boolean hasNexusPublishApplied = rootProjectPluginManager.hasPlugin(NEXUS_PUBLISH_PLUGIN_ID)
            if (hasNexusPublishApplied) {
                project.rootProject.logger.debug("Nexus Publish Plugin already applied to root project")
            } else {
                rootProjectPluginManager.apply(NexusPublishPlugin)
            }

            if (isRelease) {
                project.rootProject.tasks.withType(InitializeNexusStagingRepository).configureEach { InitializeNexusStagingRepository task ->
                    task.shouldRunAfter = project.tasks.withType(Sign)
                }
            }

            if (!hasNexusPublishApplied) {
                project.rootProject.nexusPublishing {
                    repositories {
                        sonatype {
                            if (nexusPublishUrl) {
                                nexusUrl = project.uri(nexusPublishUrl)
                            }
                            if (nexusPublishSnapshotUrl) {
                                snapshotRepositoryUrl = project.uri(nexusPublishSnapshotUrl)
                            }
                            username = nexusPublishUsername
                            password = nexusPublishPassword
                            stagingProfileId = nexusPublishStagingProfileId
                        }
                    }
                }
            }
        }

        project.afterEvaluate {
            validateProjectPublishable(project as Project)
            project.publishing {
                if (useMavenPublish) {
                    addMavenPublishValidations(project as Project, mavenPublishUrl)
                    System.setProperty('org.gradle.internal.publish.checksums.insecure', true as String)
                    repositories {
                        maven {
                            credentials {
                                username = mavenPublishUsername
                                password = mavenPublishPassword
                            }
                            url = mavenPublishUrl
                        }
                    }
                }

                publications {
                    maven(MavenPublication) {
                        artifactId gpe.artifactId ?: project.name
                        groupId gpe.groupId ?: project.group

                        doAddArtefact(project, delegate)
                        def sourcesJar = taskContainer.findByName('sourcesJar')
                        if (sourcesJar != null) {
                            artifact sourcesJar
                        }
                        def javadocJar = taskContainer.findByName('javadocJar')
                        if (javadocJar != null) {
                            artifact javadocJar
                        }
                        def extraArtefact = getDefaultExtraArtifact(project)
                        if (extraArtefact) {
                            artifact extraArtefact
                        }

                        pom.withXml {
                            Node pomNode = asNode()

                            // Prevent multiple dependencyManagement nodes
                            if (pomNode.dependencyManagement) {
                                pomNode.dependencyManagement[0].replaceNode {}
                            }

                            if (gpe != null) {
                                pomNode.children().last() + {
                                    def title = gpe.title ?: project.name
                                    delegate.name title
                                    delegate.description gpe.desc ?: title

                                    def websiteUrl = gpe.websiteUrl ?: gpe.githubSlug ? "https://github.com/$gpe.githubSlug" : ''
                                    if (!websiteUrl) {
                                        throw new RuntimeException(getErrorMessage('websiteUrl'))
                                    }
                                    delegate.url websiteUrl

                                    def license = gpe.license
                                    if (license != null) {
                                        def concreteLicense = GrailsPublishExtension.License.LICENSES.get(license.name)
                                        if (concreteLicense != null) {
                                            delegate.licenses {
                                                delegate.license {
                                                    delegate.name concreteLicense.name
                                                    delegate.url concreteLicense.url
                                                    delegate.distribution concreteLicense.distribution
                                                }
                                            }
                                        } else if (license.name && license.url) {
                                            delegate.licenses {
                                                delegate.license {
                                                    delegate.name license.name
                                                    delegate.url license.url
                                                    delegate.distribution license.distribution
                                                }
                                            }
                                        }
                                    } else {
                                        throw new RuntimeException(getErrorMessage('license'))
                                    }

                                    if (gpe.githubSlug) {
                                        delegate.scm {
                                            delegate.url "https://github.com/$gpe.githubSlug"
                                            delegate.connection "scm:git@github.com:${gpe.githubSlug}.git"
                                            delegate.developerConnection "scm:git@github.com:${gpe.githubSlug}.git"
                                        }
                                        delegate.issueManagement {
                                            delegate.system 'Github Issues'
                                            delegate.url "https://github.com/$gpe.githubSlug/issues"
                                        }
                                    } else {
                                        if (gpe.vcsUrl) {
                                            delegate.scm {
                                                delegate.url gpe.vcsUrl
                                                delegate.connection "scm:$gpe.vcsUrl"
                                                delegate.developerConnection "scm:$gpe.vcsUrl"
                                            }
                                        } else {
                                            throw new RuntimeException(getErrorMessage('vcsUrl'))
                                        }

                                        if (gpe.issueTrackerUrl) {
                                            delegate.issueManagement {
                                                delegate.system 'Issue Tracker'
                                                delegate.url gpe.issueTrackerUrl
                                            }
                                        } else {
                                            throw new RuntimeException(getErrorMessage('issueTrackerUrl'))
                                        }
                                    }

                                    if (gpe.developers) {
                                        delegate.developers {
                                            for (entry in gpe.developers.entrySet()) {
                                                delegate.developer {
                                                    delegate.id entry.key
                                                    delegate.name entry.value
                                                }
                                            }
                                        }
                                    } else {
                                        throw new RuntimeException(getErrorMessage('developers'))
                                    }
                                }

                            }

                            // fix dependencies without a version
                            def mavenPomNamespace = 'http://maven.apache.org/POM/4.0.0'
                            def dependenciesQName = new QName(mavenPomNamespace, 'dependencies')
                            def dependencyQName = new QName(mavenPomNamespace, 'dependency')
                            def versionQName = new QName(mavenPomNamespace, 'version')
                            def groupIdQName = new QName(mavenPomNamespace, 'groupId')
                            def artifactIdQName = new QName(mavenPomNamespace, 'artifactId')
                            def dependencyNodes = ((pomNode.getAt(dependenciesQName) as NodeList).first() as Node).getAt(dependencyQName)
                            dependencyNodes.findAll { dependencyNode ->
                                def versionNodes = (dependencyNode as Node).getAt(versionQName)
                                return versionNodes.size() == 0 || (versionNodes.first() as Node).text().isEmpty()
                            }.each { dependencyNode ->
                                def groupId = ((dependencyNode as Node).getAt(groupIdQName).first() as Node).text()
                                def artifactId = ((dependencyNode as Node).getAt(artifactIdQName).first() as Node).text()
                                def resolvedArtifacts = project.configurations.compileClasspath.resolvedConfiguration.resolvedArtifacts +
                                                        project.configurations.runtimeClasspath.resolvedConfiguration.resolvedArtifacts
                                def managedVersion = resolvedArtifacts.find {
                                    it.moduleVersion.id.group == groupId &&
                                    it.moduleVersion.id.name == artifactId
                                }?.moduleVersion?.id?.version
                                if (!managedVersion) {
                                    throw new RuntimeException("No version found for dependency $groupId:$artifactId.")
                                }
                                def versionNode = (dependencyNode as Node).getAt(versionQName)
                                if (versionNode) {
                                    (versionNode.first() as Node).value = managedVersion
                                } else {
                                    (dependencyNode as Node).appendNode('version', managedVersion)
                                }
                            }
                        }
                    }
                }
            }

            if (isRelease) {
                extensionContainer.configure(SigningExtension, {
                    it.required = isRelease
                    it.sign project.publishing.publications.maven
                })
            }

            def installTask = taskContainer.findByName('install')
            def publishToSonatypeTask = taskContainer.findByName('publishToSonatype')
            def closeAndReleaseSonatypeStagingRepositoryTask = taskContainer.findByName('closeAndReleaseSonatypeStagingRepository')
            def publishToMavenLocal = taskContainer.findByName('publishToMavenLocal')
            if (publishToSonatypeTask != null && taskContainer.findByName("publish${GrailsNameUtils.getClassName(defaultClassifier)}") == null) {
                taskContainer.register("publish${GrailsNameUtils.getClassName(defaultClassifier)}", { Task task ->
                    task.dependsOn([publishToSonatypeTask, closeAndReleaseSonatypeStagingRepositoryTask])
                    task.setGroup('publishing')
                })
            }
            if (installTask == null) {
                taskContainer.register('install', { Task task ->
                    task.dependsOn(publishToMavenLocal)
                    task.setGroup('publishing')
                })
            }
        }
    }

    protected void addMavenPublishValidations(Project project, def mavenPublishUrl) {
        project.plugins.withId(MAVEN_PUBLISH_PLUGIN_ID) {
            TaskProvider<? extends Task> publishTask = project.tasks.named("publish")

            TaskProvider validateBeforePublish = project.tasks.register("requireMavenPublishUrl") {
                if (!mavenPublishUrl) {
                    throw new RuntimeException('Could not locate a project property of `mavenPublishUrl` or an environment variable of `MAVEN_PUBLISH_URL`. A URL is required for maven publishing.')
                }
            }

            publishTask.configure {
                it.dependsOn validateBeforePublish
            }
        }
    }

    protected void doAddArtefact(Project project, MavenPublication publication) {
        publication.from project.components.java
    }

    protected Map<String, String> getDefaultExtraArtifact(Project project) {
        String pluginXml = "${project.sourceSets.main.groovy.getClassesDirectory().get().getAsFile()}/META-INF/grails-plugin.xml".toString()
        new File(pluginXml).exists() ? [
                source    : pluginXml,
                classifier: getDefaultClassifier(),
                extension : 'xml'
        ] : null
    }

    protected String getDefaultClassifier() {
        'plugin'
    }

    protected validateProjectPublishable(Project project) {
        if (!project.components) {
            throw new RuntimeException("Cannot apply Grails Publish Plugin. Project ${project.name} does not have any components to publish.")
        }
    }
}

