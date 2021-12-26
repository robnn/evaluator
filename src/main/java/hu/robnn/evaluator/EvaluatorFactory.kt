package hu.robnn.evaluator

import org.reflections.Reflections

object EvaluatorFactory {
    fun getAvailableProgrammingLanguages(): List<ProgrammingLanguage> {
        val reflections = Reflections("hu.robnn.evaluator")
        return reflections.getSubTypesOf(Evaluator::class.java).map {
            val newInstance = it.getDeclaredConstructor().newInstance()
            val method = it.getMethod("getLanguage")
            method.invoke(newInstance) as ProgrammingLanguage
        }
    }

    fun getEvaluatorForLanguage(language: ProgrammingLanguage): Evaluator {
        val reflections = Reflections("hu.robnn.evaluator")
        val evaluator = reflections.getSubTypesOf(Evaluator::class.java).firstOrNull {
            val newInstance = it.getDeclaredConstructor().newInstance()
            val method = it.getMethod("getLanguage")
            language == method.invoke(newInstance) as ProgrammingLanguage
        } ?: throw UnsupportedOperationException("Not supported language: $language")
        return evaluator.getDeclaredConstructor().newInstance()
    }
}