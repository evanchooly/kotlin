/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.modules;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.kotlin.config.IncrementalCompilation;
import org.jetbrains.kotlin.jps.build.JvmSourceRoot;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.intellij.openapi.util.io.FileUtil.toSystemIndependentName;
import static com.intellij.openapi.util.text.StringUtil.escapeXml;
import static org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser.*;

public class KotlinModuleXmlBuilder {
    private final StringBuilder xml = new StringBuilder();
    private final Printer p = new Printer(xml);
    private boolean done = false;

    public KotlinModuleXmlBuilder() {
        openTag(p, MODULES);
    }

    public KotlinModuleXmlBuilder addModule(
            String moduleName,
            String outputDir,
            List<File> sourceFiles,
            List<JvmSourceRoot> javaSourceRoots,
            Collection<File> classpathRoots,
            JavaModuleBuildTargetType targetType,
            Set<File> directoriesToFilterOut
    ) {
        assert !done : "Already done";

        if (targetType.isTests()) {
            p.println("<!-- Module script for tests -->");
        }
        else {
            p.println("<!-- Module script for production -->");
        }

        p.println("<", MODULE, " ",
                  NAME, "=\"", escapeXml(moduleName), "\" ",
                  TYPE, "=\"", escapeXml(targetType.getTypeId()), "\" ",
                  OUTPUT_DIR, "=\"", getEscapedPath(new File(outputDir)), "\">"
        );
        p.pushIndent();

        for (File sourceFile : sourceFiles) {
            p.println("<", SOURCES, " ", PATH, "=\"", getEscapedPath(sourceFile), "\"/>");
        }

        processJavaSourceRoots(javaSourceRoots);
        processClasspath(classpathRoots, directoriesToFilterOut);

        closeTag(p, MODULE);
        return this;
    }

    private void processClasspath(
            @NotNull Collection<File> files,
            @NotNull Set<File> directoriesToFilterOut
    ) {
        p.println("<!-- Classpath -->");
        for (File file : files) {
            boolean isOutput = directoriesToFilterOut.contains(file) && !IncrementalCompilation.isEnabled();
            if (isOutput) {
                // For IDEA's make (incremental compilation) purposes, output directories of the current module and its dependencies
                // appear on the class path, so we are at risk of seeing the results of the previous build, i.e. if some class was
                // removed in the sources, it may still be there in binaries. Thus, we delete these entries from the classpath.
                p.println("<!-- Output directory, commented out -->");
                p.println("<!-- ");
                p.pushIndent();
            }

            p.println("<", CLASSPATH, " ", PATH, "=\"", getEscapedPath(file), "\"/>");

            if (isOutput) {
                p.popIndent();
                p.println("-->");
            }
        }
    }

    private void processJavaSourceRoots(@NotNull List<JvmSourceRoot> roots) {
        p.println("<!-- Java source roots -->");
        for (JvmSourceRoot root : roots) {
            p.print("<");
            p.printWithNoIndent(JAVA_SOURCE_ROOTS, " ", PATH, "=\"", getEscapedPath(root.getFile()), "\"");

            if (root.getPackagePrefix() != null) {
                p.printWithNoIndent(" ", JAVA_SOURCE_PACKAGE_PREFIX, "=\"", root.getPackagePrefix(), "\"");
            }

            p.printWithNoIndent("/>");
            p.println();
        }
    }

    public CharSequence asText() {
        if (!done) {
            closeTag(p, MODULES);
            done = true;
        }
        return xml;
    }

    private static void openTag(Printer p, String tag) {
        p.println("<" + tag + ">");
        p.pushIndent();
    }

    private static void closeTag(Printer p, String tag) {
        p.popIndent();
        p.println("</" + tag + ">");
    }

    private static String getEscapedPath(File sourceFile) {
        return escapeXml(toSystemIndependentName(sourceFile.getPath()));
    }
}
