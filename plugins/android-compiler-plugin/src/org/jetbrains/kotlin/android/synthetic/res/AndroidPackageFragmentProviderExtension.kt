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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.descriptors.*
import org.jetbrains.kotlin.android.synthetic.forEachUntilLast
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.storage.StorageManager

abstract class AndroidPackageFragmentProviderExtension : PackageFragmentProviderExtension {
    protected abstract fun getLayoutXmlFileManager(project: Project, moduleInfo: ModuleInfo?): AndroidLayoutXmlFileManager?

    @Volatile
    private var cachedPackageFragmentProvider: PackageFragmentProvider? = null

    @Volatile
    private var cachedModuleData: AndroidModuleData? = null

    @Synchronized
    private fun getCachedFragmentProvider(actualModuleData: AndroidModuleData): PackageFragmentProvider? {
        return if (actualModuleData === cachedModuleData) cachedPackageFragmentProvider else null
    }

    override fun getPackageFragmentProvider(
            project: Project,
            module: ModuleDescriptor,
            storageManager: StorageManager,
            trace: BindingTrace,
            moduleInfo: ModuleInfo?
    ): PackageFragmentProvider? {
        val layoutXmlFileManager = getLayoutXmlFileManager(project, moduleInfo) ?: return null

        val moduleData = layoutXmlFileManager.getModuleData()
        val cachedPackageFragmentProvider = getCachedFragmentProvider(moduleData)
        if (cachedPackageFragmentProvider != null) return cachedPackageFragmentProvider

        val lazyContext = LazySyntheticElementResolveContext(module, storageManager)

        val packageDescriptors = arrayListOf<PackageFragmentDescriptor>()

        // Packages with synthetic properties
        for (variantData in moduleData) {
            for ((layoutName, layouts) in variantData) {
                fun createPackageFragment(fqName: String, forView: Boolean, isDeprecated: Boolean = false): PackageFragmentDescriptor {
                    val resources = layoutXmlFileManager.extractResources(layouts, module)
                    val packageData = AndroidSyntheticPackageData(moduleData, forView, isDeprecated, resources)
                    return AndroidSyntheticPackageFragmentDescriptor(module, FqName(fqName), packageData, lazyContext, storageManager)
                }

                val packageFqName = AndroidConst.SYNTHETIC_PACKAGE + '.' + variantData.variant.name + '.' + layoutName

                packageDescriptors += createPackageFragment(packageFqName, false)
                packageDescriptors += createPackageFragment(packageFqName + ".view", true)

                if (variantData.variant.isMainVariant && !moduleData.any { it.variant.name == layoutName }) {
                    val deprecatedPackageFqName = AndroidConst.SYNTHETIC_PACKAGE + '.' + layoutName
                    packageDescriptors += createPackageFragment(deprecatedPackageFqName, false, isDeprecated = true)
                    packageDescriptors += createPackageFragment(deprecatedPackageFqName + ".view", true, isDeprecated = true)
                }
            }
        }

        // Empty middle packages
        AndroidConst.SYNTHETIC_SUBPACKAGES.forEachUntilLast { s ->
            packageDescriptors += PredefinedPackageFragmentDescriptor(s, module, storageManager)
        }
        for (variantData in moduleData) {
            val fqName = AndroidConst.SYNTHETIC_PACKAGE + '.' + variantData.variant.name
            packageDescriptors += PredefinedPackageFragmentDescriptor(fqName, module, storageManager)
        }

        // Package with clearFindViewByIdCache()
        AndroidConst.SYNTHETIC_SUBPACKAGES.last().let { s ->
            packageDescriptors += PredefinedPackageFragmentDescriptor(s, module, storageManager) { descriptor ->
                lazyContext().getWidgetReceivers(false).map { genClearCacheFunction(descriptor, it) }
            }
        }

        return AndroidSyntheticPackageFragmentProvider(packageDescriptors)
    }
}

class AndroidSyntheticPackageFragmentProvider(val packageFragments: Collection<PackageFragmentDescriptor>) : PackageFragmentProvider {
    override fun getPackageFragments(fqName: FqName) = packageFragments.filter { it.fqName == fqName }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) =
            packageFragments.asSequence()
                    .map { it.fqName }
                    .filter { !it.isRoot && it.parent() == fqName }
                    .toList()
}
