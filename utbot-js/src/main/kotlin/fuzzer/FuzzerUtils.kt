package fuzzer

import com.oracle.js.parser.ir.IdentNode
import javafx.scene.shape.Path
import org.utbot.framework.plugin.api.JsClassId
import java.io.File
import java.nio.file.Paths

object FuzzerUtils {
    fun createMapOfTypedParams(): Map<IdentNode, JsClassId>? {
        val fileName = "utbot-js/src/main/js/runTernTakeTypes.js"
        val allProgram = ""
        // TODO: Change this to tern call via cmd
        val nodeBuilder = ProcessBuilder("cmd.exe", "/c", "node -e \"$allProgram\"")
        val nodeProcess = nodeBuilder.start()
        val nodeBufferedReader = nodeProcess.inputStream.bufferedReader()
        val errBufferReader = nodeProcess.errorStream.bufferedReader().readText()
        val stringJson = nodeBufferedReader.readText()
        nodeBufferedReader.close()
        println(stringJson)
        return null
    }
}