package hu.robnn.evaluator.java

import hu.robnn.evaluator.Evaluator
import hu.robnn.evaluator.ProgrammingLanguage
import hu.robnn.evaluator.models.CompilationResult
import hu.robnn.evaluator.models.EvaluationResult
import hu.robnn.evaluator.models.MethodCallResult
import hu.robnn.evaluator.models.ThinCompilationResult
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.time.StopWatch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider


class JavaEvaluator: Evaluator {

    override fun evaluate(classes: Map<String, String>, classNameToCall: String, methodToCall: String, methodParamTypes:
    List<String>, params: List<Any>): EvaluationResult {
        val packageId = RandomStringUtils.random(8, true, false)
        val messages = mutableListOf<String?>()
        val loadedClasses = mutableMapOf<String, Class<*>?>()

        val compilationResult = compileAndLoadClasses(packageId, classes)
        if (compilationResult.compilerResultCode != 0) {
            messages.add(compilationResult.compilerResultError)
        } else {
            messages.add(compilationResult.compilerResultOutput)
        }
        compilationResult.loadedClasses.forEach { loadedClasses[it.name.split(".")[1]] = it }
        var method: Method? = null
        val constructedClasses = loadedClasses.map {
            if (it.key == classNameToCall) {
                method = it.value?.getMethod(methodToCall, *methodParamTypes.map { paramType ->  Class.forName(paramType) }.toTypedArray())
                method?.isAccessible = true
            }
            val declaredConstructor = it.value?.getDeclaredConstructor()
            declaredConstructor?.isAccessible = true
            it.key to declaredConstructor?.newInstance()
        }.toMap()

        val invoke = methodInvokeDecorator(method, constructedClasses[classNameToCall], params)
        cleanup(compilationResult.packageName)
        return EvaluationResult(ThinCompilationResult(compilationResult.compilerResultCode,
                compilationResult.compilerResultOutput, compilationResult.compilerResultError), invoke)
    }

    private fun cleanup(packageName: String?) {
        val root = File("./tmp")
        if (packageName != null) {
            val packageFolder = File(root, packageName)
            deleteDirectoryStream(packageFolder.toPath())
            packageFolder.delete()
        }
    }

    @Throws(IOException::class)
    private fun deleteDirectoryStream(path: Path?) {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map { obj: Path -> obj.toFile() }
                .forEach { obj: File -> obj.delete() }
    }

    private fun methodInvokeDecorator(method: Method?, `class`: Any?, params: List<Any>): MethodCallResult {
        val out = ByteArrayOutputStream()
        val outputStream = PrintStream(out)
        val err = ByteArrayOutputStream()
        val errorStream = PrintStream(err)
        val originalOut = System.out
        val originalErr = System.err
        System.setOut(outputStream)
        System.setErr(errorStream)

        val watch = StopWatch.create()
        watch.start()

        val result = method?.invoke(`class`, *params.toTypedArray())

        watch.stop()

        System.setErr(originalErr)
        System.setOut(originalOut)

        out.close()
        err.close()
        errorStream.close()
        outputStream.close()

        return MethodCallResult(String(out.toByteArray(), Charset.defaultCharset()),
                String(err.toByteArray(), Charset.defaultCharset()), result, watch.nanoTime.toDouble() / 1000000)
    }

    private fun compileAndLoadClasses(generatedPackageId: String, classes: Map<String, String>): CompilationResult {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        val compiler = ToolProvider.getSystemJavaCompiler()
        val root = File("./tmp")
        val sourceFiles = classes.map {
            val file = File(root, "$generatedPackageId/${it.key}.java")
            file.parentFile.mkdirs()
            val withPackage = "package $generatedPackageId; ${it.value}"
            Files.write(file.toPath(), withPackage.encodeToByteArray())
            file
        }
        val compilerResult = compiler.run(null, outputStream, errorStream, *sourceFiles.map { it.path }.toTypedArray())
        val classLoader = URLClassLoader.newInstance(arrayOf(root.toURI().toURL()))
        val loadedClasses = classes.map { Class.forName("$generatedPackageId.${it.key}", true, classLoader) }
        return CompilationResult(compilerResult, outputStream.toString(), errorStream.toString(), generatedPackageId,
                loadedClasses)
    }

    override fun getLanguage() = ProgrammingLanguage.JAVA
}