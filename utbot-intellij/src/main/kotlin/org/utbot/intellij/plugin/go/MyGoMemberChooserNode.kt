package org.utbot.intellij.plugin.go

import com.goide.GoIcons
import com.goide.intentions.generate.constructor.GoMemberChooserNode
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject
import com.intellij.psi.util.PsiTreeUtil

class MyGoMemberChooserNode(functionOrMethod: GoFunctionOrMethodDeclaration) : GoMemberChooserNode(functionOrMethod) {

    override fun getParentNodeDelegate(): MemberChooserObject {
        val parentFile = PsiTreeUtil.getParentOfType(
            this.psiElement,
            GoFile::class.java
        ) ?: throw IllegalStateException("Function or method must have GoFile as a parent")

        return PsiElementMemberChooserObject(parentFile, parentFile.name, GoIcons.ICON)
    }
}