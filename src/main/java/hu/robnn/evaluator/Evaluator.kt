package hu.robnn.evaluator

import hu.robnn.evaluator.models.EvaluationResult

interface Evaluator {
    fun getLanguage(): ProgrammingLanguage
    fun evaluate(classes: Map<String, String>, classNameToCall: String, methodToCall: String, methodParamTypes: List<String>, params: List<Any>): EvaluationResult
}