package utils

import org.utbot.framework.plugin.api.JsClassId

data class MethodTypes(
    val parameters: List<JsClassId>,
    val returnType: JsClassId,
)