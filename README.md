Grails Gradle Plugins
========

Latest API Docs: https://grails.github.io/grails-gradle-plugin/latest/api/

Below are the plugins that are provided by the grails-gradle-plugin dependency.

```
buildscript {
    dependencies {
        classpath "org.grails:grails-gradle-plugin:$grailsVersion"
    }
}
```

grails-core
---------
_Todo_: Add the docs

grails-doc
---------
_Todo_: Add the docs

grails-gsp
---------
* Configure GSP Compiling Task

grails-plugin
---------
* Configure Ast Sources
* Configure Project Name And Version AST Metadata
* Configure Plugin Resources
* Configure Plugin Jar Task
* Configure Sources Jar Task

grails-profile
---------
_Todo_: Add the docs

grails-profile-publish
---------
_Todo_: Add the docs

grails-publish
---------
A Gradle plugin to simplify publishing to either an Artifactory or Maven Central endpoint for snapshots & to Maven Central for releases.  

Example Usage:

    grailsPublish {
        websiteUrl = 'http://foo.com/myplugin'
        license {
            name = 'Apache-2.0'
        }
        issueTrackerUrl = 'http://github.com/myname/myplugin/issues'
        vcsUrl = 'http://github.com/myname/myplugin'
        title = "My plugin title"
        desc = "My plugin description"
        developers = [johndoe:"John Doe"]
    }

or

    grailsPublish {
        githubSlug = 'foo/bar'
        license {
            name = 'Apache-2.0'
        }
        title = "My plugin title"
        desc = "My plugin description"
        developers = [johndoe:"John Doe"]
    }

By default this plugin will publish to the specified Artifactory instance for snapshots, and Maven Central for releases.  To change the snapshot publish behavior, you can set the `snapshotRepoType` to `RepositoryType.CENTRAL`.

The credentials and connection url must be specified as a project property or an environment variable.

Artifactory Environment Variables are:

    ARTIFACTORY_USERNAME
    ARTIFACTORY_PASSWORD
    ARTIFACTORY_URL

Sonatype Environment Variables are:

    SONATYPE_NEXUS_URL
    SONATYPE_SNAPSHOT_URL
    SONATYPE_USERNAME
    SONATYPE_PASSWORD
    SONATYPE_STAGING_PROFILE_ID

grails-web
---------
* Adds web specific extensions


Typical Project Type Gradle Plugin Includes
========
Below are typical Gradle plugin applies that certain types of projects should expect.  These should be automatically added of you when using `grails create-app` and `grails create-plugin` commands.  However, if you wish to enhance or change the scope of your plugin or project you may have to change (add or remove) a grails gradle plugin.

Create App
----

<h4>Grails Web Project</h4>
-----
A project created with a typical `grails create-app --profile=web`

```
apply plugin: "org.grails.grails-web"
apply plugin: "org.grails.grails-gsp"
```

<h4>Grails Web API Project</h4>
----
A project created with a typical `grails create-app --profile=web-api`

```
apply plugin: "org.grails.grails-web"
```

<h4>Grails Web Micro Project</h4>

A project created with a typical `grails create-app --profile=web-micro`

There is no plugins used here as this project type creates a stand alone runnable groovy application and no `build.gradle` file.


Create Plugin
---

<h4>Grails Plugin Web Project</h4>
A project created with a typical `grails create-plugin --profile=web-plugin`

```
apply plugin: "org.grails.grails-plugin"
apply plugin: "org.grails.grails-gsp"
```

<h4>Grails Plugin Web API Project</h4>
A project created with a typical `grails create-plugin --profile=web-api`. _Note: No org.grails.grails-plugin include_

```
apply plugin: "org.grails.grails-web"
```


<h4>Grails Plugin Web Plugin Project</h4>
A project created with a typical `grails create-plugin --profile=plugin`.

```
apply plugin: "org.grails.grails-plugin"
```

<h4>Grails Plugin Web Micro Project</h4>

A project created with a typical `grails create-plugin --profile=web-micro`

There is no plugins used here as this project type creates a stand alone runnable groovy application and no `build.gradle`` file.
