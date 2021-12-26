package hu.robnn.evaluator.models

class MethodCallResult(val output: String? = null,
                       val error: String? = null,
                       val result: Any? = null,
                       val runningTime: Double? = null)