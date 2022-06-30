package org.utbot.cli.util

import kotlinx.coroutines.runBlocking
import org.utbot.framework.plugin.api.util.jcdb
import org.utbot.jcdb.api.ClasspathSet
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import org.utbot.jcdb.api.CompilationDatabase

private fun String.toUrl(): File = File(this)

 fun createClassLoader(classPath: String? = "", absoluteFileNameWithClasses: String? = null): ClasspathSet {
    val urlSet = mutableSetOf<File>()
    classPath?.run {
        urlSet.addAll(this.split(File.pathSeparatorChar).map { it.toUrl() }.toMutableSet())
    }
    absoluteFileNameWithClasses?.run {
        urlSet.addAll(File(absoluteFileNameWithClasses).readLines().map { it.toUrl() }.toMutableSet())
    }
    return runBlocking {

    }jcdb.classpathSet(urlSet.toList())
}