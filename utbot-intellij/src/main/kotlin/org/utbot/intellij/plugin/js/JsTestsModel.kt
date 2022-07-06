package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

data class JsTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
    val selectedMethods: Set<JSMemberInfo>?,
) {
    var testSourceRoot: VirtualFile? = null
    // TODO testPackageName
    // TODO testFramework
}
