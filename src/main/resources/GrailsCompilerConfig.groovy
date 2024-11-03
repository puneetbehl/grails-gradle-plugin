import org.codehaus.groovy.control.customizers.ImportCustomizer

def importCustomizer = new ImportCustomizer()
importCustomizer.addStarImports('java.time')

configuration.addCompilationCustomizers(importCustomizer)