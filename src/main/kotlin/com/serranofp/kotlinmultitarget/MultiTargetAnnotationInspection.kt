package com.serranofp.kotlinmultitarget

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.AddAnnotationUseSiteTargetIntention
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.annotationEntryVisitor
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.UseSiteTargetsList
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MultiTargetAnnotationInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        annotationEntryVisitor visitor@{ entry ->
            if (entry.useSiteTarget != null) return@visitor

            val context = entry.analyze(BodyResolveMode.PARTIAL)
            val parent = entry.parent.parent as? KtAnnotated ?: return@visitor
            val annotationTargetList = AnnotationChecker.getActualTargetList(parent, null, context)
            val annotationTargets = annotationTargetList.defaultTargets + annotationTargetList.canBeSubstituted + annotationTargetList.onlyWithUseSiteTarget
            val elementTargets = entry.potentialTargets(context) ?: return@visitor

            val potentialTargets = parent.getImplicitUseSiteTargetList().filter {
                KotlinTarget.USE_SITE_MAPPING[it] in annotationTargets &&
                  KotlinTarget.USE_SITE_MAPPING[it] in elementTargets
            }
            if (potentialTargets.size <= 1) return@visitor

            val firstTarget = potentialTargets.first()
            val applicableToProperty = AnnotationUseSiteTarget.PROPERTY in potentialTargets
            val applicableToField = AnnotationUseSiteTarget.FIELD in potentialTargets

            if (firstTarget == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER && (applicableToProperty || applicableToField)) {
                val explanation = when {
                    applicableToProperty && !applicableToField -> "property"
                    !applicableToProperty && applicableToField -> "field"
                    else -> "property or field"
                }
                holder.registerProblem(
                    entry,
                    "Annotation applied to constructor parameter, not to $explanation",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    AddExplicitTarget(entry)
                )
            } else {
                holder.registerProblem(
                    entry,
                    "Potentially ambiguous use-site target, defaulted to ${firstTarget.description}",
                    ProblemHighlightType.WEAK_WARNING,
                    AddExplicitTarget(entry)
                )
            }
        }

    private fun KtAnnotationEntry.potentialTargets(context: BindingContext): Set<KotlinTarget>? {
        val typeReference = typeReference ?: return null
        val type = context[BindingContext.TYPE, typeReference] ?: return null
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
        return AnnotationChecker.applicableTargetSet(descriptor)
    }
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

class AddExplicitTarget(
    annotation: KtAnnotationEntry
) : LocalQuickFixAndIntentionActionOnPsiElement(annotation) {
    private val addUseSiteIntent = AddAnnotationUseSiteTargetIntention()

    override fun getFamilyName(): String = addUseSiteIntent.familyName
    override fun getText(): String = "Add explicit use-site target"

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) = addUseSiteIntent.applyTo(startElement as KtAnnotationEntry, editor)

}

/*
class AddExplicitTargetOld(private val isDefault: Boolean, private val target: AnnotationUseSiteTarget) : LocalQuickFix {
    override fun getName(): String =
        if (isDefault) "Make default use site target '${target.description}' explicit"
        else "Set use site target to '${target.description}'"

    override fun getFamilyName(): String = "Make use site target explicit"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? KtAnnotationEntry ?: return
        val factory = KtPsiFactory(project)
        val newElementText = "@${target.renderName}:${element.text.drop(1)}"
        val newElement = factory.createAnnotationEntry(newElementText)
        element.replace(newElement)
    }
}
*/
