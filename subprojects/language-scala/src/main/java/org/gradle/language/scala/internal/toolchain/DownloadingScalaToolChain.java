/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.language.scala.internal.toolchain;

import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.tasks.compile.daemon.CompilerDaemonManager;
import org.gradle.language.scala.ScalaPlatform;
import org.gradle.platform.base.internal.toolchain.ToolProvider;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DownloadingScalaToolChain implements ScalaToolChainInternal {
    private ProjectFinder projectFinder;
    private CompilerDaemonManager compilerDaemonManager;
    private final ConfigurationContainer configurationContainer;
    private final DependencyHandler dependencyHandler;
    private final JavaVersion javaVersion;

    public DownloadingScalaToolChain(ProjectFinder projectFinder, CompilerDaemonManager compilerDaemonManager, ConfigurationContainer configurationContainer, DependencyHandler dependencyHandler) {
        this.projectFinder = projectFinder;
        this.compilerDaemonManager = compilerDaemonManager;
        this.configurationContainer = configurationContainer;
        this.dependencyHandler = dependencyHandler;
        this.javaVersion = JavaVersion.current();
    }

    public String getName() {
        return String.format("Scala Toolchain");
    }

    public String getDisplayName() {
        return String.format("Scala Toolchain (JDK %s (%s))", javaVersion.getMajorVersion(), javaVersion);
    }

    public ToolProvider select(ScalaPlatform targetPlatform) {
        try {
            Configuration scalaClasspath = resolveDependency(String.format("org.scala-lang:scala-compiler:%s", targetPlatform.getScalaVersion()));
            Configuration zincClasspath = resolveDependency(String.format("com.typesafe.zinc:zinc:%s", DefaultScalaToolProvider.DEFAULT_ZINC_VERSION));
            Set<File> resolvedScalaClasspath = scalaClasspath.resolve();
            Set<File> resolvedZincClasspath = zincClasspath.resolve();

            // Unfortunately, the Zinc artifacts *always* pull in scala library, reflect and compiler 2.11.7 in
            // `resolvedZincClasspath`. This is a problem, because we may be using 2.10.5.
            // The correct versions of the library and compiler are in `resolvedScalaClasspath`.
            resolvedZincClasspath = removeScalaComponents(resolvedZincClasspath);
            resolvedZincClasspath.addAll(resolvedScalaClasspath);

            return new DefaultScalaToolProvider(projectFinder, compilerDaemonManager, resolvedScalaClasspath, resolvedZincClasspath);

        } catch(ResolveException resolveException) {
            return new NotFoundScalaToolProvider(resolveException);
        }
    }

    private static Set<File> removeScalaComponents(Set<File> jars) {
        Set<File> nonScalaComponents = new HashSet<File>();
        for (File jar: jars) {
            if (!isScalaComponent(jar)) {
                nonScalaComponents.add(jar);
            }
        }
        return nonScalaComponents;
    }

    private static boolean isScalaComponent(File jar) {
        String jarName = jar.getName();

        return jarName.contains("scala-library") || jarName.contains("scala-reflect") || jarName.contains("scala-compiler");
    }

    private Configuration resolveDependency(Object dependencyNotation) {
        Dependency dependency = dependencyHandler.create(dependencyNotation);
        return configurationContainer.detachedConfiguration(dependency);
    }
}
