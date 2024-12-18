package org.grails.gradle.test

import org.gradle.testkit.runner.GradleRunner
import spock.lang.PendingFeature

class GrailsPublishPluginSpec extends GradleSpecification {
    def "gradle config works when not publishing - snapshot - maven publish - legacy-apply - multi-project-no-subproject-build-gradle-publish-all"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-no-subproject-build-gradle-publish-all')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - snapshot - maven publish - legacy-apply - multi-project-no-subproject-build-gradle-publish-per-project"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-no-subproject-build-gradle-publish-per-project')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - snapshot - maven publish - legacy-apply - multi-project-parent-child-setup-per-project-parent-published"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-parent-child-setup-per-project-parent-published')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - snapshot - maven publish - legacy-apply - multi-project-parent-child-setup-per-project-child-published"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-parent-child-setup-per-project-child-published')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - snapshot - maven publish - legacy-apply - multi-project-with-subproject-gradle"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-with-subproject-gradle')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - snapshot - maven publish - plugins-block - multi-project-parent-child-setup-per-project-parent-published"() {
        given:
        GradleRunner runner = setupProject('plugins-block', 'multi-project-parent-child-setup-per-project-parent-published')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - snapshot - maven publish - plugins-block - multi-project-parent-child-setup-per-project-child-published"() {
        given:
        GradleRunner runner = setupProject('plugins-block', 'multi-project-parent-child-setup-per-project-child-published')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - snapshot - maven publish - plugins-block - multi-project-with-subproject-gradle"() {
        given:
        GradleRunner runner = setupProject('plugins-block', 'multi-project-with-subproject-gradle')

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }


    def "gradle config works when not publishing - milestone - maven publish - legacy-apply - multi-project-no-subproject-build-gradle-publish-all"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-no-subproject-build-gradle-publish-all')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - milestone - maven publish - legacy-apply - multi-project-no-subproject-build-gradle-publish-per-project"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-no-subproject-build-gradle-publish-per-project')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - milestone - maven publish - legacy-apply - multi-project-parent-child-setup-per-project-parent-published"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-parent-child-setup-per-project-parent-published')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    @PendingFeature(reason = "Failed to apply plugin class 'io.github.gradlenexus.publishplugin.NexusPublishPlugin'")
    def "gradle config works when not publishing - milestone - maven publish - legacy-apply - multi-project-parent-child-setup-per-project-child-published"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-parent-child-setup-per-project-child-published')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    @PendingFeature(reason = "Failed to apply plugin class 'io.github.gradlenexus.publishplugin.NexusPublishPlugin'")
    def "gradle config works when not publishing - milestone - maven publish - legacy-apply - multi-project-with-subproject-gradle"() {
        given:
        GradleRunner runner = setupProject('legacy-apply', 'multi-project-with-subproject-gradle')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    def "gradle config works when not publishing - milestone - maven publish - plugins-block - multi-project-parent-child-setup-per-project-parent-published"() {
        given:
        GradleRunner runner = setupProject('plugins-block', 'multi-project-parent-child-setup-per-project-parent-published')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    @PendingFeature(reason = "Failed to apply plugin class 'io.github.gradlenexus.publishplugin.NexusPublishPlugin'")
    def "gradle config works when not publishing - milestone - maven publish - plugins-block - multi-project-parent-child-setup-per-project-child-published"() {
        given:
        GradleRunner runner = setupProject('plugins-block', 'multi-project-parent-child-setup-per-project-child-published')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }

    @PendingFeature(reason = "Failed to apply plugin class 'io.github.gradlenexus.publishplugin.NexusPublishPlugin'")
    def "gradle config works when not publishing - milestone - maven publish - plugins-block - multi-project-with-subproject-gradle"() {
        given:
        GradleRunner runner = setupProject('plugins-block', 'multi-project-with-subproject-gradle')

        setGradleProperty(
                "projectVersion",
                "0.0.1-M1",
                runner
        )

        when:
        def result = executeTask("assemble", runner)

        then:
        assertTaskSuccess("assemble", result)
        assertBuildSuccess(result, ["compileJava", "processResources"])
    }
}
