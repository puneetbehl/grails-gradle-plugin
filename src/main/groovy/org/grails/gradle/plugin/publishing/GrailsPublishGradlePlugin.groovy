/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//file:noinspection DuplicatedCode
package org.grails.gradle.plugin.publishing

import grails.util.GrailsNameUtils
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
 * @since 3.1
 */
class GrailsPublishGradlePlugin implements Plugin<Project> {

    String getErrorMessage(String missingSetting) {
        return """No '$missingSetting' was specified. Please provide a valid publishing configuration. Example:

grailsPublish {
    websiteUrl = 'http://foo.com/myplugin'
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

The credentials and connection url must be specified as a project property or an environment variable:

`MAVEN_PUBLISH` Environment Variables are:
    ARTIFACTORY_USERNAME
    ARTIFACTORY_PASSWORD
    ARTIFACTORY_URL

`NEXUS_PUBLISH` Environment Variables are:
    SONATYPE_NEXUS_URL
    SONATYPE_SNAPSHOT_URL
    SONATYPE_USERNAME
    SONATYPE_PASSWORD
    SONATYPE_STAGING_PROFILE_ID

"""
    }

    @Override
    void apply(Project project) {
        final ExtensionContainer extensionContainer = project.extensions
        final TaskContainer taskContainer = project.tasks
        final GrailsPublishExtension gpe = extensionContainer.create('grailsPublish', GrailsPublishExtension)

        final String artifactoryUsername = project.findProperty('artifactoryPublishUsername') ?: System.getenv('ARTIFACTORY_USERNAME') ?: ''
        final String artifactoryPassword = project.findProperty('artifactoryPublishPassword') ?: System.getenv('ARTIFACTORY_PASSWORD') ?: ''
        final String artifactoryPublishUrl = project.findProperty('artifactoryPublishUrl') ?: System.getenv('ARTIFACTORY_URL') ?: ''
        final String sonatypeNexusUrl = project.findProperty('sonatypeNexusUrl') ?: System.getenv('SONATYPE_NEXUS_URL') ?: ''
        final String sonatypeSnapshotUrl = project.findProperty('sonatypeSnapshotUrl') ?: System.getenv('SONATYPE_SNAPSHOT_URL') ?: ''
        final String sonatypeUsername = project.findProperty('sonatypeUsername') ?: System.getenv('SONATYPE_USERNAME') ?: ''
        final String sonatypePassword = project.findProperty('sonatypePassword') ?: System.getenv('SONATYPE_PASSWORD') ?: ''
        final String sonatypeStagingProfileId = project.findProperty('sonatypeStagingProfileId') ?: System.getenv('SONATYPE_STAGING_PROFILE_ID') ?: ''

        final ExtraPropertiesExtension extraPropertiesExtension = extensionContainer.findByType(ExtraPropertiesExtension)

        extraPropertiesExtension.setProperty(SIGNING_KEY_ID, project.hasProperty(SIGNING_KEY_ID) ? project[SIGNING_KEY_ID] : System.getenv('SIGNING_KEY') ?: null)
        extraPropertiesExtension.setProperty(SIGNING_PASSWORD, project.hasProperty(SIGNING_PASSWORD) ? project[SIGNING_PASSWORD] : System.getenv('SIGNING_PASSPHRASE') ?: null)
        extraPropertiesExtension.setProperty(SIGNING_KEYRING, project.hasProperty(SIGNING_KEYRING) ? project[SIGNING_KEYRING] : System.getenv('SIGNING_KEYRING') ?: null)

        project.afterEvaluate {
            RepositoryTarget snapshotType = gpe.snapshotRepoType
            boolean isSnapshot = project.version.endsWith('SNAPSHOT')
            boolean isRelease = !isSnapshot
            final PluginManager projectPluginManager = project.getPluginManager()
            final PluginManager rootProjectPluginManager = project.rootProject.getPluginManager()
            projectPluginManager.apply(MavenPublishPlugin)

            project.publishing {
                if (isSnapshot && snapshotType == RepositoryTarget.MAVEN_PUBLISH) {
                    System.setProperty('org.gradle.internal.publish.checksums.insecure', true as String)
                    repositories {
                        maven {
                            credentials {
                                username = artifactoryUsername
                                password = artifactoryPassword
                            }

                            if (!artifactoryPublishUrl) {
                                throw new RuntimeException('Could not locate a project property of `artifactoryPublishUrl` or an environment variable of `ARTIFACTORY_URL` for the snapshot url')
                            }
                            url = artifactoryPublishUrl
                        }
                    }
                }

                publications {
                    maven(MavenPublication) {
                        artifactId project.name

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
                                            delegate.system "Github Issues"
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
                                                delegate.system "Issue Tracker"
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
                            // resolve versions via global dependency management
                            // see https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/8 for more complete solutions
                            final versions = project.dependencyManagement.dependencyManagementContainer.globalDependencyManagement.versions
                            pomNode.dependencies.dependency.findAll {
                                it.version.text().isEmpty()
                            }.each {
                                it.appendNode('version', versions["${it.groupId.text()}:${it.artifactId.text()}"])
                            }
                        }
                    }
                }
            }

            if (isRelease || (isSnapshot && snapshotType == RepositoryTarget.NEXUS_PUBLISH)) {
                rootProjectPluginManager.apply(NexusPublishPlugin)
                projectPluginManager.apply(SigningPlugin)

                extensionContainer.configure(SigningExtension, {
                    it.required = isRelease
                    it.sign project.publishing.publications.maven
                })

                project.rootProject.tasks.withType(InitializeNexusStagingRepository).configureEach { InitializeNexusStagingRepository task ->
                    task.shouldRunAfter = project.tasks.withType(Sign)
                }

                project.tasks.withType(Sign) {
                    onlyIf { isRelease }
                }

                project.rootProject.nexusPublishing {
                    repositories {
                        sonatype {
                            if (sonatypeNexusUrl) {
                                nexusUrl = project.uri(sonatypeNexusUrl)
                            }
                            if (sonatypeSnapshotUrl) {
                                snapshotRepositoryUrl = project.uri(sonatypeSnapshotUrl)
                            }
                            username = sonatypeUsername
                            password = sonatypePassword
                            stagingProfileId = sonatypeStagingProfileId
                        }
                    }
                }
            }

            def installTask = taskContainer.findByName("install")
            def publishToSonatypeTask = taskContainer.findByName('publishToSonatype')
            def closeAndReleaseSonatypeStagingRepositoryTask = taskContainer.findByName('closeAndReleaseSonatypeStagingRepository')
            def publishToMavenLocal = taskContainer.findByName("publishToMavenLocal")
            if (publishToSonatypeTask != null && taskContainer.findByName("publish${GrailsNameUtils.getClassName(defaultClassifier)}") == null) {
                taskContainer.register("publish${GrailsNameUtils.getClassName(defaultClassifier)}", { Task task ->
                    task.dependsOn([publishToSonatypeTask, closeAndReleaseSonatypeStagingRepositoryTask])
                    task.setGroup("publishing")
                })
            }
            if (installTask == null) {
                taskContainer.register("install", { Task task ->
                    task.dependsOn(publishToMavenLocal)
                    task.setGroup("publishing")
                })
            }
        }
    }

    protected void doAddArtefact(Project project, MavenPublication publication) {
        publication.from project.components.java
    }

    protected Map<String, String> getDefaultExtraArtifact(Project project) {
        String pluginXml = "${project.sourceSets.main.groovy.getClassesDirectory().get().getAsFile()}/META-INF/grails-plugin.xml".toString()
        new File(pluginXml).exists()? [
            source    : pluginXml,
            classifier: getDefaultClassifier(),
            extension : 'xml'
        ] : null
    }

    protected String getDefaultClassifier() {
        "plugin"
    }
}

