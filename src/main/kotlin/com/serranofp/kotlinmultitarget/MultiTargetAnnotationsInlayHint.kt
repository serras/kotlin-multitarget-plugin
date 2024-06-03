package com.serranofp.kotlinmultitarget

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class MultiTargetAnnotationsInlayHint : InlayHintsProvider {
    companion object {
        const val PROVIDER_ID : String = "kotlin.multi.target"
    }

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector = Collector()

    private class Collector : SharedBypassCollector {
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is KtAnnotationEntry) return
            val potentialTargets = element.getPotentialTargets()?.takeIf { it.size > 1 } ?: return

            sink.addPresentation(InlineInlayPosition(element.startOffset + 1, true), hasBackground = true) {
                text("${potentialTargets.first().renderName}:")
            }
        }

    }
}