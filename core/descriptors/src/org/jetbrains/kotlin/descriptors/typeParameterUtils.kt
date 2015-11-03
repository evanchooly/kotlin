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

package org.jetbrains.kotlin.descriptors

fun ClassDescriptor.computeConstructorTypeParameters(): List<TypeParameterDescriptor> {
    val declaredParameters = declaredTypeParameters

    if (!isInner) return declaredParameters
    val containingTypeConstructor = (containingDeclaration as? ClassDescriptor)?.typeConstructor ?: return emptyList()

    val additional = containingTypeConstructor.parameters.map { it.copyForInnerDeclaration(this, declaredParameters.size) }
    if (additional.isEmpty()) return declaredParameters

    return declaredParameters + additional
}

private fun TypeParameterDescriptor.copyForInnerDeclaration(
        declarationDescriptor: DeclarationDescriptor,
        declaredTypeParametersCount: Int
) = CopyTypeParameterDescriptor(this, declarationDescriptor, declaredTypeParametersCount)

private class CopyTypeParameterDescriptor(
        private val originalDescriptor: TypeParameterDescriptor,
        private val declarationDescriptor: DeclarationDescriptor,
        private val declaredTypeParametersCount: Int
) : TypeParameterDescriptor by originalDescriptor {
    override fun isCopyFromOuterDeclaration() = true
    override fun getOriginal() = originalDescriptor.original
    override fun getContainingDeclaration() = declarationDescriptor
    override fun getIndex() = declaredTypeParametersCount + originalDescriptor.index
    override fun toString() = originalDescriptor.toString() + "[inner-copy]"
}
