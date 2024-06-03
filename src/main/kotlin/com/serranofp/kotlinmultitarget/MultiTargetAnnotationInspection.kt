package com.serranofp.kotlinmultitarget

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.AddAnnotationUseSiteTargetIntention
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.annotationEntryVisitor

class MultiTargetAnnotationInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        annotationEntryVisitor visitor@{ entry ->
            if (entry.useSiteTarget != null) return@visitor
            val potentialTargets = entry.getPotentialTargets()?.takeIf { it.size > 1 } ?: return@visitor
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