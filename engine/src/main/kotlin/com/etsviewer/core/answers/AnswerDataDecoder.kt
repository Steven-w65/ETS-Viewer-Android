package com.etsviewer.core.answers

import com.etsviewer.core.answers.parsing.*
import com.etsviewer.core.service.*
import kotlinx.serialization.json.*

/** Stable API used by the resource loader; question structure rules live in registered parsers. */
class AnswerDataDecoder(private val registry: QuestionParserRegistry = QuestionParserRegistry()) {
    fun clean(text: String): String = TextNormalizer.clean(text)

    fun decode(text: String, groupName: String): AnswerSection {
        val root = try { Json.parseToJsonElement(text).jsonObject } catch (_: Exception) { throw ProtocolFailure("Invalid content.json") }
        val type = root.string("structure_type")
        val entry = registry.resolve(type)
        val info = root["info"] as? JsonObject ?: throw ProtocolFailure("content.json is missing info for $type")
        val parsed = entry.parser.parse(info, ParseContext(type))
        if (parsed.questions.isEmpty() || parsed.questions.any { q ->
            q.text.isBlank() && q.topic.isNullOrBlank() && q.originalText.isBlank() &&
                q.answers.all(String::isBlank) && q.options.all { it.text.isBlank() && it.media.isEmpty() } &&
                q.media.isEmpty() && !(parsed.allowsSectionMediaOnly && parsed.media.isNotEmpty())
        }) throw ProtocolFailure("No readable question content for $type")
        val ownTitle = listOf("section_title", "section_name").firstNotNullOfOrNull { key ->
            (root[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?.let(::clean)?.takeIf { it.isNotBlank() }
        }?.let { SectionTitle(it, SectionTitleSource.CONTENT_METADATA) }
        return AnswerSection(groupName.ifBlank { entry.displayName }, type, entry.displayName, parsed.questions, parsed.media,
            originalText = parsed.originalText, explanation = parsed.explanation, sectionTitle = ownTitle,
            nameOrigin = SectionNameOrigin.RESOURCE_COMPATIBILITY, presentation = entry.presentation)
    }

    companion object {
        val types: Map<String, String> = QuestionParserRegistry().typeNames
    }
}
