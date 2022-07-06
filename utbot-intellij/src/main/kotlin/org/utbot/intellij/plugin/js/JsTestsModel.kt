package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.util.classMembers.MemberInfo
import java.lang.reflect.Member

data class JsTestsModel(
    val project: Project,
    val srcModule: Module,
    val testModule: Module,
    val fileMethods: Set<MemberInfo>?,
    val focusedMethod: Set<JSFunction>?,
) {
    var testSourceRoot: VirtualFile? = null
    // TODO testPackageName
    // TODO testFramework
}
