package org.utbot.intellij.plugin.go

import com.goide.psi.GoFunctionOrMethodDeclaration

class MyGoFunctionsOrMethodsChooserNode(val functionOrMethod: GoFunctionOrMethodDeclaration) {

    var isSelected = false
        private set

}