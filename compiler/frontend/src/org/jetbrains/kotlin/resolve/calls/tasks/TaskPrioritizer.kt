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

package org.jetbrains.kotlin.resolve.calls.tasks

import com.google.common.collect.Lists
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getUnaryPlusOrMinusOperatorFunctionName
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isConventionCall
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized
import org.jetbrains.kotlin.resolve.calls.callUtil.createLookupLocation
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollector
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollectors
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.filtered
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.hasLowPriorityInOverloadResolution
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.CastClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.resolve.validation.InfixValidator
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.util.OperatorNameConventions

public class TaskPrioritizer(
        private val storageManager: StorageManager,
        private val smartCastManager: SmartCastManager,
        private val dynamicCallableDescriptors: DynamicCallableDescriptors
) {

    public fun <D : CallableDescriptor, F : D> computePrioritizedTasks(
            context: BasicCallResolutionContext,
            name: Name,
            tracing: TracingStrategy,
            callableDescriptorCollectors: CallableDescriptorCollectors<D>
    ): List<ResolutionTask<D, F>> {
        val explicitReceiver = context.call.getExplicitReceiver()
        val result = ResolutionTaskHolder<D, F>(storageManager, context, PriorityProviderImpl<D>(context), tracing)
        val taskPrioritizerContext = TaskPrioritizerContext(name, result, context, context.scope, callableDescriptorCollectors)

        if (explicitReceiver is QualifierReceiver) {
            val qualifierReceiver: QualifierReceiver = explicitReceiver
            val receiverScope = LexicalScope.empty(qualifierReceiver.getNestedClassesAndPackageMembersScope().memberScopeAsImportingScope(),
                                                   qualifierReceiver.descriptor)
            doComputeTasks(NO_RECEIVER, taskPrioritizerContext.replaceScope(receiverScope))
            computeTasksForClassObjectReceiver(qualifierReceiver, taskPrioritizerContext)
        }
        else {
            doComputeTasks(explicitReceiver, taskPrioritizerContext)

            // Temporary fix for code migration (unaryPlus()/unaryMinus())
            val unaryConventionName = getUnaryPlusOrMinusOperatorFunctionName(context.call)
            if (unaryConventionName != null) {
                val deprecatedName = if (name == OperatorNameConventions.UNARY_PLUS)
                    OperatorNameConventions.PLUS
                else
                    OperatorNameConventions.MINUS

                val additionalContext = TaskPrioritizerContext(deprecatedName, result, context, context.scope, callableDescriptorCollectors)
                doComputeTasks(explicitReceiver, additionalContext)
            }
        }

        return result.getTasks()
    }

    private fun <D : CallableDescriptor, F : D> computeTasksForClassObjectReceiver(
            qualifierReceiver: QualifierReceiver,
            taskPrioritizerContext: TaskPrioritizerContext<D, F>
    ) {
        val classObjectReceiver = qualifierReceiver.getClassObjectReceiver()
        if (!classObjectReceiver.exists()) {
            return
        }
        val classifierDescriptor = qualifierReceiver.classifier
        doComputeTasks(classObjectReceiver, taskPrioritizerContext.filterCollectors {
            when {
                classifierDescriptor is ClassDescriptor && classifierDescriptor.getCompanionObjectDescriptor() != null -> {
                    // nested classes and objects should not be accessible via short reference to companion object
                    it !is ConstructorDescriptor && it !is FakeCallableDescriptorForObject
                }
                classifierDescriptor != null && DescriptorUtils.isEnumEntry(classifierDescriptor) -> {
                    // objects nested in enum should not be accessible via enum entries reference
                    it !is FakeCallableDescriptorForObject
                }
                else -> true
            }
        })
    }

    private fun <D : CallableDescriptor, F : D> doComputeTasks(receiver: ReceiverValue, c: TaskPrioritizerContext<D, F>) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val receiverWithTypes = ReceiverWithTypes(receiver, c.context)

        val resolveInvoke = c.context.call.getDispatchReceiver().exists()
        if (resolveInvoke) {
            addCandidatesForInvoke(receiverWithTypes, c)
            return
        }
        val implicitReceivers = c.scope.getImplicitReceiversHierarchy().map { it.value }
        if (receiver.exists()) {
            addCandidatesForExplicitReceiver(receiverWithTypes, implicitReceivers, c, isExplicit = true)
            addMembers(receiverWithTypes, c, staticMembers = true, isExplicit = true)
            return
        }
        addCandidatesForNoReceiver(implicitReceivers, c)
    }

    private inner class ReceiverWithTypes(
            val value: ReceiverValue,
            private val context: ResolutionContext<*>
    ) {
        val types: Collection<KotlinType> by lazy { smartCastManager.getSmartCastVariants(value, context) }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForExplicitReceiver(
            explicitReceiver: ReceiverWithTypes,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean
    ) {
        val explicitReceiverTypeIsDynamic = explicitReceiver.value.type.isDynamic()

        fun addMembersAndExtensionsWithFilter(filter: (CallableDescriptor) -> Boolean) {
            // If the explicit receiver is erroneous, an error function is returned by getMembersByName().
            // An error function should not be sorted out by our custom filters to prevent looking for extension functions.
            if (explicitReceiver.types.any { it.isError }) return

            addMembers(explicitReceiver, c, staticMembers = false, isExplicit = isExplicit, filter = filter)
            if (!explicitReceiverTypeIsDynamic) {
                addExtensionCandidates(explicitReceiver, implicitReceivers, c, isExplicit, filter)
            }
        }

        // Members and extensions with 'operator' and 'infix' modifiers have higher priority
        if (isConventionCall(c.context.call)) {
            addMembersAndExtensionsWithFilter { d: CallableDescriptor -> d is FunctionDescriptor && d.isOperator }
        }
        if (InfixValidator.isInfixCall(c.context.call.calleeExpression)) {
            addMembersAndExtensionsWithFilter { d: CallableDescriptor -> d is FunctionDescriptor && d.isInfix }
        }

        addMembers(explicitReceiver, c, staticMembers = false, isExplicit = isExplicit)

        if (explicitReceiverTypeIsDynamic) {
            addCandidatesForDynamicReceiver(explicitReceiver, implicitReceivers, c, isExplicit)
        }
        else {
            addExtensionCandidates(explicitReceiver, implicitReceivers, c, isExplicit)
        }
    }

    private fun <D : CallableDescriptor, F : D> addExtensionCandidates(
            explicitReceiver: ReceiverWithTypes,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean,
            filter: ((CallableDescriptor) -> Boolean)? = null
    ) {
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {
            //member extensions
            for (implicitReceiver in implicitReceivers) {
                addMemberExtensionCandidates(
                        implicitReceiver,
                        explicitReceiver,
                        callableDescriptorCollector,
                        c,
                        createKind(EXTENSION_RECEIVER, isExplicit)
                )
            }
            //extensions
            c.result.addCandidates {
                val extensions = callableDescriptorCollector.getExtensionsByName(
                        c.scope, c.name, explicitReceiver.types, createLookupLocation(c))
                val filteredExtensions = if (filter == null) extensions else extensions.filter(filter)

                convertWithImpliedThis(
                        c.scope,
                        explicitReceiver.value,
                        filteredExtensions,
                        createKind(EXTENSION_RECEIVER, isExplicit),
                        c.context.call
                )
            }
        }
    }

    private fun <D : CallableDescriptor, F : D> addMembers(
            explicitReceiver: ReceiverWithTypes,
            c: TaskPrioritizerContext<D, F>,
            staticMembers: Boolean,
            isExplicit: Boolean,
            filter: ((CallableDescriptor) -> Boolean)? = null
    ) {
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {
            c.result.addCandidates {
                val members = Lists.newArrayList<ResolutionCandidate<D>>()
                for (type in explicitReceiver.types) {
                    val membersForThisVariant = if (staticMembers) {
                        callableDescriptorCollector.getStaticMembersByName(type, c.name, createLookupLocation(c))
                    }
                    else {
                        callableDescriptorCollector.getMembersByName(type, c.name, createLookupLocation(c))
                    }
                    val filteredMembers = if (filter == null) membersForThisVariant else membersForThisVariant.filter(filter)

                    val dispatchReceiver =
                            if (explicitReceiver.value is ClassReceiver && type != explicitReceiver.value.type) {
                                CastClassReceiver(explicitReceiver.value.declarationDescriptor, type)
                            }
                            else {
                                explicitReceiver.value
                            }
                    convertWithReceivers(
                            filteredMembers,
                            dispatchReceiver,
                            NO_RECEIVER,
                            members,
                            createKind(DISPATCH_RECEIVER, isExplicit),
                            c.context.call
                    )
                }
                members
            }
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForDynamicReceiver(
            explicitReceiver: ReceiverWithTypes,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean
    ) {
        val onlyDynamicReceivers = c.replaceCollectors(c.callableDescriptorCollectors.onlyDynamicReceivers<D>())
        addExtensionCandidates(explicitReceiver, implicitReceivers, onlyDynamicReceivers, isExplicit)

        c.result.addCandidates {
            val dynamicScope = dynamicCallableDescriptors.createDynamicDescriptorScope(c.context.call, c.scope.ownerDescriptor)

            val dynamicDescriptors = c.callableDescriptorCollectors.flatMap {
                it.getNonExtensionsByName(dynamicScope.memberScopeAsImportingScope(), c.name, createLookupLocation(c))
            }

            convertWithReceivers(dynamicDescriptors, explicitReceiver.value, NO_RECEIVER, createKind(DISPATCH_RECEIVER, isExplicit), c.context.call)
        }
    }

    private fun createKind(kind: ExplicitReceiverKind, isExplicit: Boolean): ExplicitReceiverKind {
        if (isExplicit) return kind
        return ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
    }

    private fun <D : CallableDescriptor, F : D> addMemberExtensionCandidates(
            dispatchReceiver: ReceiverValue,
            receiverParameter: ReceiverWithTypes,
            callableDescriptorCollector: CallableDescriptorCollector<D>,
            c: TaskPrioritizerContext<D, F>,
            receiverKind: ExplicitReceiverKind
    ) {
        c.result.addCandidates {
            val memberExtensions =
                    callableDescriptorCollector.getExtensionsByName(dispatchReceiver.type.memberScope.memberScopeAsImportingScope(), c.name, receiverParameter.types, createLookupLocation(c))
            convertWithReceivers(memberExtensions, dispatchReceiver, receiverParameter.value, receiverKind, c.context.call)
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForNoReceiver(
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>
    ) {
        val lookupLocation = createLookupLocation(c)

        //locals
        c.callableDescriptorCollectors.forEach {
            c.result.addCandidates {
                convertWithImpliedThisAndNoReceiver(
                        c.scope,
                        it.getLocalNonExtensionsByName(c.scope, c.name, lookupLocation),
                        c.context.call
                )
            }
        }

        val implicitReceiversWithTypes = implicitReceivers.map { ReceiverWithTypes(it, c.context) }

        //try all implicit receivers as explicit
        for (implicitReceiver in implicitReceiversWithTypes) {
            addCandidatesForExplicitReceiver(implicitReceiver, implicitReceivers, c, isExplicit = false)
        }

        // static members hack
        c.callableDescriptorCollectors.forEach {
            c.result.addCandidates {
                val descriptors = it.getStaticInheritanceByName(c.scope, c.name, lookupLocation)
                convertWithImpliedThisAndNoReceiver(c.scope, descriptors, c.context.call)
            }
        }

        //nonlocals
        c.callableDescriptorCollectors.forEach {
            c.result.addCandidates {
                val descriptors = it.getNonExtensionsByName(c.scope, c.name, lookupLocation)
                        .filter { c.scope is ImportingScope || !ExpressionTypingUtils.isLocal(c.scope.ownerDescriptor, it) }
                convertWithImpliedThisAndNoReceiver(c.scope, descriptors, c.context.call)
            }
        }

        //static (only for better error reporting)
        for (implicitReceiver in implicitReceiversWithTypes) {
            addMembers(implicitReceiver, c, staticMembers = true, isExplicit = false)
        }
    }

    private fun createLookupLocation(c: TaskPrioritizerContext<*, *>) = c.context.call.createLookupLocation()

    private fun <D : CallableDescriptor, F : D> addCandidatesForInvoke(explicitReceiver: ReceiverWithTypes, c: TaskPrioritizerContext<D, F>) {
        val implicitReceivers = c.scope.getImplicitReceiversHierarchy().map { it.value }

        // For 'a.foo()' where foo has function type,
        // a is explicitReceiver, foo is variableReceiver.
        val variableReceiver = c.context.call.getDispatchReceiver()
        assert(variableReceiver.exists()) { "'Invoke' call hasn't got variable receiver" }

        // For invocation a.foo() explicit receiver 'a'
        // can be a receiver for 'foo' variable
        // or for 'invoke' function.

        // (1) a.foo + foo.invoke()
        if (!explicitReceiver.value.exists()) {
            addCandidatesForExplicitReceiver(ReceiverWithTypes(variableReceiver, c.context), implicitReceivers, c, isExplicit = true)
        }

        // (2) foo + a.invoke()

        // 'invoke' is member extension to explicit receiver while variable receiver is 'this object'
        //trait A
        //trait Foo { fun A.invoke() }

        if (explicitReceiver.value.exists()) {
            //a.foo()
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, explicitReceiver, c, BOTH_RECEIVERS)
            return
        }
        // with (a) { foo() }
        for (implicitReceiver in implicitReceivers) {
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, ReceiverWithTypes(implicitReceiver, c.context), c, DISPATCH_RECEIVER)
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(
            dispatchReceiver: ReceiverValue,
            receiverParameter: ReceiverWithTypes,
            c: TaskPrioritizerContext<D, F>,
            receiverKind: ExplicitReceiverKind
    ) {
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {
            addMemberExtensionCandidates(dispatchReceiver, receiverParameter, callableDescriptorCollector, c, receiverKind)
        }
    }

    private fun <D : CallableDescriptor> convertWithReceivers(
            descriptors: Collection<D>,
            dispatchReceiver: ReceiverValue,
            extensionReceiver: ReceiverValue,
            explicitReceiverKind: ExplicitReceiverKind,
            call: Call
    ): Collection<ResolutionCandidate<D>> {
        val result = Lists.newArrayList<ResolutionCandidate<D>>()
        convertWithReceivers(descriptors, dispatchReceiver, extensionReceiver, result, explicitReceiverKind, call)
        return result
    }

    private fun <D : CallableDescriptor> convertWithReceivers(
            descriptors: Collection<D>,
            dispatchReceiver: ReceiverValue,
            extensionReceiver: ReceiverValue,
            result: MutableCollection<ResolutionCandidate<D>>,
            explicitReceiverKind: ExplicitReceiverKind,
            call: Call
    ) {
        for (descriptor in descriptors) {
            val candidate = ResolutionCandidate.create<D>(call, descriptor)
            candidate.setDispatchReceiver(dispatchReceiver)
            candidate.setExtensionReceiver(extensionReceiver)
            candidate.setExplicitReceiverKind(explicitReceiverKind)
            result.add(candidate)
        }
    }

    public fun <D : CallableDescriptor> convertWithImpliedThisAndNoReceiver(
            scope: LexicalScope,
            descriptors: Collection<D>,
            call: Call
    ): Collection<ResolutionCandidate<D>> {
        return convertWithImpliedThis(scope, NO_RECEIVER, descriptors, NO_EXPLICIT_RECEIVER, call)
    }

    public fun <D : CallableDescriptor> convertWithImpliedThis(
            scope: LexicalScope,
            receiverParameter: ReceiverValue,
            descriptors: Collection<D>,
            receiverKind: ExplicitReceiverKind,
            call: Call
    ): Collection<ResolutionCandidate<D>> {
        val result = Lists.newArrayList<ResolutionCandidate<D>>()
        for (descriptor in descriptors) {
            val candidate = ResolutionCandidate.create<D>(call, descriptor)
            candidate.setExtensionReceiver(receiverParameter)
            candidate.setExplicitReceiverKind(receiverKind)
            if (setImpliedThis(scope, candidate)) {
                result.add(candidate)
            }
        }
        return result
    }

    private fun <D : CallableDescriptor> setImpliedThis(
            scope: LexicalScope,
            candidate: ResolutionCandidate<D>
    ): Boolean {
        val dispatchReceiver = candidate.getDescriptor().getDispatchReceiverParameter()
        if (dispatchReceiver == null) return true
        val receivers = scope.getImplicitReceiversHierarchy()
        for (receiver in receivers) {
            if (KotlinTypeChecker.DEFAULT.isSubtypeOf(receiver.getType(), dispatchReceiver.getType())) {
                // TODO : Smartcasts & nullability
                candidate.setDispatchReceiver(dispatchReceiver.getValue())
                return true
            }
        }
        return false
    }

    public fun <D : CallableDescriptor, F : D> computePrioritizedTasksFromCandidates(
            context: BasicCallResolutionContext,
            candidates: Collection<ResolutionCandidate<D>>,
            tracing: TracingStrategy
    ): List<ResolutionTask<D, F>> {
        val result = ResolutionTaskHolder<D, F>(storageManager, context, PriorityProviderImpl<D>(context), tracing)
        result.addCandidates {
            candidates
        }
        return result.getTasks()
    }

    private class PriorityProviderImpl<D : CallableDescriptor>(private val context: BasicCallResolutionContext) :
            ResolutionTaskHolder.PriorityProvider<ResolutionCandidate<D>> {

        override fun getPriority(candidate: ResolutionCandidate<D>)
                = if (hasImplicitDynamicReceiver(candidate)) 0
                  else (if (!isVisible(candidate) || hasLowPriority(candidate)) 0 else 2) + (if (isSynthesized(candidate)) 0 else 1)

        override fun getMaxPriority() = 3

        private fun isVisible(candidate: ResolutionCandidate<D>?): Boolean {
            if (candidate == null) return false
            val candidateDescriptor = candidate.getDescriptor()
            if (ErrorUtils.isError(candidateDescriptor)) return true
            val receiverValue = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidate.getDispatchReceiver(), context.trace.getBindingContext())
            return Visibilities.isVisible(receiverValue, candidateDescriptor, context.scope.ownerDescriptor)
        }

        private fun hasLowPriority(candidate: ResolutionCandidate<D>?): Boolean {
            if (candidate == null) return false
            return candidate.descriptor.hasLowPriorityInOverloadResolution()
        }

        private fun isSynthesized(candidate: ResolutionCandidate<D>): Boolean {
            val descriptor = candidate.getDescriptor()
            return descriptor is CallableMemberDescriptor && isOrOverridesSynthesized(descriptor)
        }

        fun hasImplicitDynamicReceiver(candidate: ResolutionCandidate<D>): Boolean {
            return (!candidate.getExplicitReceiverKind().isDispatchReceiver() || !candidate.getCall().getExplicitReceiver().exists())
                   && candidate.getDescriptor().isDynamic()
        }
    }

    private class TaskPrioritizerContext<D : CallableDescriptor, F : D>(
            val name: Name,
            val result: ResolutionTaskHolder<D, F>,
            val context: BasicCallResolutionContext,
            val scope: LexicalScope,
            val callableDescriptorCollectors: CallableDescriptorCollectors<D>
    ) {
        fun replaceScope(newScope: LexicalScope): TaskPrioritizerContext<D, F> {
            return TaskPrioritizerContext(name, result, context, newScope, callableDescriptorCollectors)
        }

        fun replaceCollectors(newCollectors: CallableDescriptorCollectors<D>): TaskPrioritizerContext<D, F> {
            return TaskPrioritizerContext(name, result, context, scope, newCollectors)
        }

        fun filterCollectors(filter: (D) -> Boolean): TaskPrioritizerContext<D, F> {
            return TaskPrioritizerContext(name, result, context, scope, callableDescriptorCollectors.filtered(filter))
        }
    }
}
