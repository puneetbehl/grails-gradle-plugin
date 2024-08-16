package org.grails.gradle.plugin.web.gsp

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.compile.AbstractOptions
import org.gradle.api.tasks.compile.GroovyForkOptions
import javax.inject.Inject;

/**
 * Presents the Compile Options used by the {@llink GroovyPageForkCompileTask}
 *
 * @author David Estes
 * @since 4.0
 */
abstract class GspCompileOptions extends AbstractOptions {
    private static final long serialVersionUID = 0L;

    @Input
    String encoding = "UTF-8"

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }

    @Nested
    GroovyForkOptions forkOptions = getObjectFactory().newInstance(GroovyForkOptions.class)
}