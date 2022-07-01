package org.utbot.framework.plugin.api

import kotlinx.coroutines.runBlocking
import org.utbot.jcdb.api.*
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.FieldId

open class JCDBClassId(private val jcdbClassId: ClassId) {

    fun findField(name: String): FieldId? {
        return runBlocking {
            jcdbClassId.findFieldOrNull(name)
        }
    }

    val name get() = jcdbClassId.name
    val simpleName get() = jcdbClassId.simpleName

    open val isPublic: Boolean
        get() = runBlocking { jcdbClassId.isPublic() }

    open val isProtected: Boolean
        get() = runBlocking { jcdbClassId.isProtected() }

    open val isPrivate: Boolean
        get() = runBlocking { jcdbClassId.isPrivate() }

    open val isFinal: Boolean
        get() = runBlocking { jcdbClassId.isFinal() }

    open val isStatic: Boolean
        get() = runBlocking { jcdbClassId.isStatic() }

    open val isAbstract: Boolean
        get() = runBlocking { jcdbClassId.isAbstract() }

    open val isAnonymous: Boolean
        get() = runBlocking { jcdbClassId.isAnonymous() }

    open val isLocalClass: Boolean
        get() = runBlocking { jcdbClassId.isLocal() }

    open val isInner: Boolean
        get() = isNested && !isStatic

    open val isNested: Boolean
        get() = runBlocking { jcdbClassId.outerClass() != null }

    open val isSynthetic: Boolean
        get() = runBlocking { jcdbClassId.isSynthetic() }

    open val isMemberClass: Boolean
        get() = runBlocking { jcdbClassId.isMemberClass() }

    open val superclass: JCDBClassId?
        get() = runBlocking {
            jcdbClassId.superclass()?.let {
                JCDBClassId(it)
            }
        }

    val methods: List<org.utbot.jcdb.api.MethodId> get() {
        return runBlocking {
            jcdbClassId.methods()
        }
    }

    val interfaces: List<JCDBClassId> get() {
        return runBlocking {
            jcdbClassId.interfaces().map { JCDBClassId(it) }
        }
    }


}