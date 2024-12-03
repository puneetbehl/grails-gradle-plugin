/*
 * Copyright 2024 original authors
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
package org.grails.gradle.plugin.web.gsp

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Abstract Gradle task for compiling templates, using GroovyPageCompilerForkTask
 * This Task is a Forked Java Task that is configurable with fork options provided
 * by {@link GspCompileOptions}
 *
 * @author David Estes
 * @since 4.0
 */
@CompileStatic
abstract class GroovyPageForkCompileTask extends AbstractCompile {

    @Input
    @Optional
    String packageName

    @Internal
    File srcDir

    @LocalState
    String tmpDirPath

    /**
     * @deprecated Use {@link #tmpDirPath} instead.
     */
    @Deprecated
    @Optional
    @InputDirectory
    File tmpDir

    @Input
    @Optional
    String serverpath

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }

    @Nested
    GspCompileOptions compileOptions = getObjectFactory().newInstance(GspCompileOptions.class)

    @Override
    @PathSensitive(PathSensitivity.RELATIVE)
    FileTree getSource() {
        return super.getSource()
    }

    @Override
    void setSource(Object source) {
        try {
            srcDir = project.file(source)
            if(srcDir.exists() && !srcDir.isDirectory()) {
                throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
            }
            super.setSource(source)
        } catch (e) {
            throw new IllegalArgumentException("The source for GSP compilation must be a single directory, but was $source")
        }
    }

    @TaskAction
    void execute() {
        compile()
    }

    protected void compile() {

        if(packageName == null) {
            packageName = project.name
            if(!packageName) {
                packageName = project.projectDir.canonicalFile.name
            }
        }

        ExecResult result = project.javaexec(
                new Action<JavaExecSpec>() {
                    @Override
                    @CompileDynamic
                    void execute(JavaExecSpec javaExecSpec) {
                        javaExecSpec.mainClass.set(getCompilerName())
                        javaExecSpec.setClasspath(getClasspath())

                        def jvmArgs = compileOptions.forkOptions.jvmArgs
                        if(jvmArgs) {
                            javaExecSpec.jvmArgs(jvmArgs)
                        }
                        javaExecSpec.setMaxHeapSize( compileOptions.forkOptions.memoryMaximumSize )
                        javaExecSpec.setMinHeapSize( compileOptions.forkOptions.memoryInitialSize )

                        //This is the OLD Style and seems kinda silly to be hard coded this way. but restores functionality
                        //for now
                        def configFiles = [
                            project.file("grails-app/conf/application.yml").canonicalPath,
                            project.file("grails-app/conf/application.groovy").canonicalPath
                        ].join(',')

                        Path path = Paths.get(tmpDirPath)
                        File tmp = tmpDir
                        if (Files.exists(path)) {
                            tmp = path.toFile()
                        } else {
                            tmp = Files.createDirectories(path).toFile()
                        }
                        def arguments = [
                            srcDir.canonicalPath,
                            destinationDir.canonicalPath,
                            tmp.canonicalPath,
                            targetCompatibility,
                            packageName,
                            serverpath,
                            configFiles,
                            compileOptions.encoding
                        ]

                        prepareArguments(arguments)
                        javaExecSpec.args(arguments)
                    }

                }
        )
        result.assertNormalExitValue()

    }

    void prepareArguments(List<String> arguments) {
        // no-op
    }

    @Input
    protected String getCompilerName() {
        "org.grails.web.pages.GroovyPageCompilerForkTask"
    }

    @Input
    String getFileExtension() {
        "gsp"
    }
}
