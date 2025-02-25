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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.google.common.collect.Sets
import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.runner.JavaMainMethodProvider
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreJavaFileManager
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.java.JavaParserDefinition
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.*
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileContextProvider
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.JavaClassSupersImpl
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.compiled.ClsCustomNavigationPolicy
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.psi.util.JavaClassSupers
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.JavaElementFinder
import org.jetbrains.kotlin.asJava.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.jvm.config.JVMConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.kotlinSourceRoots
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.parsing.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.CliDeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*

public class KotlinCoreEnvironment private constructor(
        parentDisposable: Disposable, 
        applicationEnvironment: JavaCoreApplicationEnvironment, 
        configuration: CompilerConfiguration
) {

    private val projectEnvironment: JavaCoreProjectEnvironment = object : KotlinCoreProjectEnvironment(parentDisposable, applicationEnvironment) {
        override fun preregisterServices() {
            registerProjectExtensionPoints(Extensions.getArea(getProject()))
        }
    }
    private val sourceFiles = ArrayList<KtFile>()
    private val javaRoots = ArrayList<JavaRoot>()

    public val configuration: CompilerConfiguration = configuration.copy().let {
        it.setReadOnly(true)
        it
    }

    init {
        val project = projectEnvironment.getProject()
        project.registerService(javaClass<DeclarationProviderFactoryService>(), CliDeclarationProviderFactoryService(sourceFiles))
        project.registerService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl())

        registerProjectServicesForCLI(projectEnvironment)
        registerProjectServices(projectEnvironment)

        fillClasspath(configuration)
        val fileManager = ServiceManager.getService(project, javaClass<CoreJavaFileManager>())
        val index = JvmDependenciesIndex(javaRoots)
        (fileManager as KotlinCliJavaFileManagerImpl).initIndex(index)

        sourceFiles.addAll(CompileEnvironmentUtil.getKtFiles(project, getSourceRootsCheckingForDuplicates(), {
            message ->
            report(ERROR, message)
        }))
        sourceFiles.sortedWith(object : Comparator<KtFile> {
            override fun compare(o1: KtFile, o2: KtFile): Int {
                return o1.getVirtualFile().getPath().compareTo(o2.getVirtualFile().getPath(), ignoreCase = true)
            }
        })

        KotlinScriptDefinitionProvider.getInstance(project).addScriptDefinitions(configuration.getList(CommonConfigurationKeys.SCRIPT_DEFINITIONS_KEY))

        project.registerService(javaClass<JvmVirtualFileFinderFactory>(), JvmCliVirtualFileFinderFactory(index))

        ExternalDeclarationsProvider.registerExtensionPoint(project)
        ExpressionCodegenExtension.registerExtensionPoint(project)
        ClassBuilderInterceptorExtension.registerExtensionPoint(project)
        AnalysisCompletedHandlerExtension.registerExtensionPoint(project)
        StorageComponentContainerContributor.registerExtensionPoint(project)

        for (registrar in configuration.getList(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)) {
            registrar.registerProjectComponents(project, configuration)
        }
    }

    private val applicationEnvironment: CoreApplicationEnvironment
        get() = projectEnvironment.getEnvironment()

    public val application: MockApplication
        get() = applicationEnvironment.getApplication()

    public val project: Project
        get() = projectEnvironment.getProject()

    public val sourceLinesOfCode: Int by lazy { countLinesOfCode(sourceFiles) }

    public fun countLinesOfCode(sourceFiles: List<KtFile>): Int  =
            sourceFiles.sumBy {
                val text = it.getText()
                StringUtil.getLineBreakCount(it.getText()) + (if (StringUtil.endsWithLineBreak(text)) 0 else 1)
            }

    private fun fillClasspath(configuration: CompilerConfiguration) {
        for (root in configuration.getList(CommonConfigurationKeys.CONTENT_ROOTS)) {
            val javaRoot = root as? JvmContentRoot ?: continue
            val virtualFile = contentRootToVirtualFile(javaRoot) ?: continue

            projectEnvironment.addSourcesToClasspath(virtualFile)

            val prefixPackageFqName = (javaRoot as? JavaSourceRoot)?.packagePrefix?.let {
                if (isValidJavaFqName(it)) {
                    FqName(it)
                }
                else {
                    report(WARNING, "Invalid package prefix name is ignored: $it")
                    null
                }
            }

            val rootType = when (javaRoot) {
                is JavaSourceRoot -> JavaRoot.RootType.SOURCE
                is JvmClasspathRoot -> JavaRoot.RootType.BINARY
                else -> throw IllegalStateException()
            }

            javaRoots.add(JavaRoot(virtualFile, rootType, prefixPackageFqName))
        }
    }

    fun contentRootToVirtualFile(root: JvmContentRoot): VirtualFile? {
        when (root) {
            is JvmClasspathRoot -> {
                return if (root.file.isFile()) findJarRoot(root) else findLocalDirectory(root)
            }
            is JavaSourceRoot -> {
                return if (root.file.isDirectory()) findLocalDirectory(root) else null
            }
            else -> throw IllegalStateException("Unexpected root: $root")
        }
    }

    private fun findLocalDirectory(root: JvmContentRoot): VirtualFile? {
        val path = root.file
        val localFile = applicationEnvironment.getLocalFileSystem().findFileByPath(path.getAbsolutePath())
        if (localFile == null) {
            report(WARNING, "Classpath entry points to a non-existent location: $path")
            return null
        }
        return localFile
    }

    private fun findJarRoot(root: JvmClasspathRoot): VirtualFile? {
        val path = root.file
        val jarFile = applicationEnvironment.getJarFileSystem().findFileByPath("${path}!/")
        if (jarFile == null) {
            report(WARNING, "Classpath entry points to a file that is not a JAR archive: $path")
            return null
        }
        return jarFile
    }

    private fun getSourceRootsCheckingForDuplicates(): Collection<String> {
        val uniqueSourceRoots = Sets.newLinkedHashSet<String>()

        configuration.kotlinSourceRoots.forEach { path ->
            if (!uniqueSourceRoots.add(path)) {
                report(WARNING, "Duplicate source root: $path")
            }
        }

        return uniqueSourceRoots
    }

    public fun getSourceFiles(): List<KtFile> = sourceFiles

    private fun report(severity: CompilerMessageSeverity, message: String) {
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                               ?: throw CompileEnvironmentException(message)
        messageCollector.report(severity, message, CompilerMessageLocation.NO_LOCATION)
    }

    companion object {

        private val APPLICATION_LOCK = Object()
        private var ourApplicationEnvironment: JavaCoreApplicationEnvironment? = null
        private var ourProjectCount = 0

        @JvmStatic
        public fun createForProduction(
                parentDisposable: Disposable, configuration: CompilerConfiguration, configFilePaths: List<String>
        ): KotlinCoreEnvironment {
            val appEnv = getOrCreateApplicationEnvironmentForProduction(configuration, configFilePaths)
            // Disposing of the environment is unsafe in production then parallel builds are enabled, but turning it off universally
            // breaks a lot of tests, therefore it is disabled for production and enabled for tests
            if (System.getProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY) == null || appEnv.application.isUnitTestMode) {
                // JPS may run many instances of the compiler in parallel (there's an option for compiling independent modules in parallel in IntelliJ)
                // All projects share the same ApplicationEnvironment, and when the last project is disposed, the ApplicationEnvironment is disposed as well
                Disposer.register(parentDisposable, object : Disposable {
                    override fun dispose() {
                        synchronized (APPLICATION_LOCK) {
                            if (--ourProjectCount <= 0) {
                                disposeApplicationEnvironment()
                            }
                        }
                    }
                })
            }
            val environment = KotlinCoreEnvironment(parentDisposable, appEnv, configuration)

            synchronized (APPLICATION_LOCK) {
                ourProjectCount++
            }
            return environment
        }

        @TestOnly
        @JvmStatic
        public fun createForTests(
                parentDisposable: Disposable, configuration: CompilerConfiguration, extensionConfigs: List<String>
        ): KotlinCoreEnvironment {
            // Tests are supposed to create a single project and dispose it right after use
            return KotlinCoreEnvironment(parentDisposable, createApplicationEnvironment(parentDisposable, configuration, extensionConfigs), configuration)
        }

        private fun getOrCreateApplicationEnvironmentForProduction(configuration: CompilerConfiguration, configFilePaths: List<String>): JavaCoreApplicationEnvironment {
            synchronized (APPLICATION_LOCK) {
                if (ourApplicationEnvironment != null)
                    return ourApplicationEnvironment!!

                val parentDisposable = Disposer.newDisposable()
                ourApplicationEnvironment = createApplicationEnvironment(parentDisposable, configuration, configFilePaths)
                ourProjectCount = 0
                Disposer.register(parentDisposable, object : Disposable {
                    override fun dispose() {
                        synchronized (APPLICATION_LOCK) {
                            ourApplicationEnvironment = null
                        }
                    }
                })
                return ourApplicationEnvironment!!
            }
        }

        public fun disposeApplicationEnvironment() {
            synchronized (APPLICATION_LOCK) {
                if (ourApplicationEnvironment == null) return
                val environment = ourApplicationEnvironment
                ourApplicationEnvironment = null
                Disposer.dispose(environment!!.getParentDisposable())
            }
        }

        private fun createApplicationEnvironment(parentDisposable: Disposable, configuration: CompilerConfiguration, configFilePaths: List<String>): JavaCoreApplicationEnvironment {

            Extensions.cleanRootArea(parentDisposable)
            registerAppExtensionPoints()
            val applicationEnvironment = JavaCoreApplicationEnvironment(parentDisposable)

            for (configPath in configFilePaths) {
                registerApplicationExtensionPointsAndExtensionsFrom(configuration, configPath)
            }

            registerApplicationServicesForCLI(applicationEnvironment)
            registerApplicationServices(applicationEnvironment)

            return applicationEnvironment
        }

        private fun registerAppExtensionPoints() {
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), BinaryFileStubBuilders.EP_NAME, javaClass<FileTypeExtensionPoint<Any>>())
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), FileContextProvider.EP_NAME, javaClass<FileContextProvider>())
            //
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), MetaDataContributor.EP_NAME, javaClass<MetaDataContributor>())
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), PsiAugmentProvider.EP_NAME, javaClass<PsiAugmentProvider>())
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), JavaMainMethodProvider.EP_NAME, javaClass<JavaMainMethodProvider>())
            //
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ContainerProvider.EP_NAME, javaClass<ContainerProvider>())
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClsCustomNavigationPolicy.EP_NAME, javaClass<ClsCustomNavigationPolicy>())
            CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), ClassFileDecompilers.EP_NAME, javaClass<ClassFileDecompilers.Decompiler>())
        }

        private fun registerApplicationExtensionPointsAndExtensionsFrom(configuration: CompilerConfiguration, configFilePath: String) {
            val locator = configuration.get(JVMConfigurationKeys.COMPILER_JAR_LOCATOR)
            var pluginRoot = if (locator == null) PathUtil.getPathUtilJar() else locator.getCompilerJar()

            val app = ApplicationManager.getApplication()
            val parentFile = pluginRoot.getParentFile()

            if (pluginRoot.isDirectory() && app != null && app.isUnitTestMode()
                && FileUtil.toCanonicalPath(parentFile.getPath()).endsWith("out/production")) {
                // hack for load extensions when compiler run directly from out directory(e.g. in tests)
                val srcDir = parentFile.getParentFile().getParentFile()
                pluginRoot = File(srcDir, "idea/src")
            }

            CoreApplicationEnvironment.registerExtensionPointAndExtensions(pluginRoot, configFilePath, Extensions.getRootArea())
        }

        private fun registerApplicationServicesForCLI(applicationEnvironment: JavaCoreApplicationEnvironment) {
            // ability to get text from annotations xml files
            applicationEnvironment.registerFileType(PlainTextFileType.INSTANCE, "xml")
            applicationEnvironment.registerParserDefinition(JavaParserDefinition())
        }

        // made public for Upsource
        @JvmStatic
        public fun registerApplicationServices(applicationEnvironment: JavaCoreApplicationEnvironment) {
            with(applicationEnvironment) {
                registerFileType(KotlinFileType.INSTANCE, "kt")
                registerFileType(KotlinFileType.INSTANCE, KotlinParserDefinition.STD_SCRIPT_SUFFIX)
                registerParserDefinition(KotlinParserDefinition())
                getApplication().registerService(javaClass<KotlinBinaryClassCache>(), KotlinBinaryClassCache())
                getApplication().registerService(javaClass<JavaClassSupers>(), javaClass<JavaClassSupersImpl>())
            }
        }

        private fun registerProjectExtensionPoints(area: ExtensionsArea) {
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiTreeChangePreprocessor.EP_NAME, javaClass<PsiTreeChangePreprocessor>())
            CoreApplicationEnvironment.registerExtensionPoint(area, PsiElementFinder.EP_NAME, javaClass<PsiElementFinder>())
        }

        // made public for Upsource
        @JvmStatic
        public fun registerProjectServices(projectEnvironment: JavaCoreProjectEnvironment) {
            with (projectEnvironment.getProject()) {
                registerService(javaClass<KotlinScriptDefinitionProvider>(), KotlinScriptDefinitionProvider())
                registerService(javaClass<KotlinJavaPsiFacade>(), KotlinJavaPsiFacade(this))
                registerService(javaClass<KtLightClassForFacade.FacadeStubCache>(), KtLightClassForFacade.FacadeStubCache(this))
            }
        }

        private fun registerProjectServicesForCLI(projectEnvironment: JavaCoreProjectEnvironment) {
            with (projectEnvironment.getProject()) {
                registerService(javaClass<CoreJavaFileManager>(), ServiceManager.getService(this, javaClass<JavaFileManager>()) as CoreJavaFileManager)

                val cliLightClassGenerationSupport = CliLightClassGenerationSupport(this)
                registerService(javaClass<LightClassGenerationSupport>(), cliLightClassGenerationSupport)
                registerService(javaClass<CliLightClassGenerationSupport>(), cliLightClassGenerationSupport)
                registerService(javaClass<CodeAnalyzerInitializer>(), cliLightClassGenerationSupport)

                val area = Extensions.getArea(this)

                area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(JavaElementFinder(this, cliLightClassGenerationSupport))
                area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(
                        PsiElementFinderImpl(this, ServiceManager.getService(this, javaClass<JavaFileManager>())))
            }
        }
    }
}
