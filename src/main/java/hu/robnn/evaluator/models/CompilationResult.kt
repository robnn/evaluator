package hu.robnn.evaluator.models

class CompilationResult(var compilerResultCode: Int? = null,
                        var compilerResultOutput: String? = null,
                        var compilerResultError: String? = null,
                        var packageName: String? = null,
                        var loadedClasses: List<Class<*>> = mutableListOf())