package org.gradle.api.plugins.scala

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.collections.SimpleFileCollection

class ZincClasspathExtension {
    public def FileCollection zincClasspath = new SimpleFileCollection()
}
