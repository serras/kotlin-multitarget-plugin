package com.serranofp.kotlinmultitarget

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.UseSiteTargetsList
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun KtAnnotationEntry.getPotentialTargets(): List<AnnotationUseSiteTarget>? {
    val context = this.analyze(BodyResolveMode.PARTIAL)
    val parent = this.parent.parent as? KtAnnotated ?: return null
    val annotationTargetList = AnnotationChecker.getActualTargetList(parent, null, context)
    val annotationTargets = annotationTargetList.defaultTargets + annotationTargetList.canBeSubstituted + annotationTargetList.onlyWithUseSiteTarget
    val elementTargets = this.potentialTargets(context) ?: return null

    return parent.getImplicitUseSiteTargetList().filter {
        KotlinTarget.USE_SITE_MAPPING[it] in annotationTargets &&
                KotlinTarget.USE_SITE_MAPPING[it] in elementTargets
    }
}

private fun KtAnnotationEntry.potentialTargets(context: BindingContext): Set<KotlinTarget>? {
    val typeReference = typeReference ?: return null
    val type = context[BindingContext.TYPE, typeReference] ?: return null
    val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    return AnnotationChecker.applicableTargetSet(descriptor)
}

val AnnotationUseSiteTarget.description: String
    get() = name.lowercase().split('_').joinToString(separator = " ")

// copied from the compiler sources
private fun KtAnnotated?.getImplicitUseSiteTargetList(): List<AnnotationUseSiteTarget> = when (this) {
    is KtParameter ->
        if (ownerFunction is KtPrimaryConstructor) UseSiteTargetsList.T_CONSTRUCTOR_PARAMETER else emptyList()
    is KtProperty ->
        if (!isLocal) UseSiteTargetsList.T_PROPERTY else emptyList()
    is KtPropertyAccessor ->
        if (isGetter) listOf(AnnotationUseSiteTarget.PROPERTY_GETTER) else listOf(AnnotationUseSiteTarget.PROPERTY_SETTER)
    else ->
        emptyList()
}