package com.etsviewer.core.answers

object AnswerExport {
    fun filename(title: String) = title.replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_").trim().ifBlank { "answers" } + ".txt"
    fun text(document: AnswerDocument): String = buildString {
        append("=".repeat(50)).append('\n').append(document.title).append('\n').append("=".repeat(50)).append("\n\n")
        for (section in document.sections) {
            append("\n【").append(section.name).append("】\n").append("-".repeat(30)).append('\n')
            if (section.originalText.isNotBlank()) append("\n原文:\n").append(section.originalText).append('\n')
            if (section.explanation.isNotBlank()) append("\n解析:\n").append(section.explanation).append('\n')
            for (q in section.questions) {
                when {
                    q.options.isNotEmpty() -> {
                        append("\n").append(q.number).append(". ").append(q.text).append('\n')
                        q.options.forEach { append("  ").append(if (it.label in q.answers) "★" else " ").append(' ').append(it.label).append(". ").append(it.text).append('\n') }
                        append("  答案: ").append(q.answers.joinToString(" / ")).append('\n')
                    }
                    section.structureType == "collector.fill" -> append("\n").append(q.number).append(". 答案: ").append(q.answers.joinToString(" / ")).append('\n')
                    q.topic != null -> {
                        append("\n主题: ").append(q.topic).append('\n')
                        if (q.originalText.isNotEmpty()) append("\n原文:\n").append(q.originalText).append('\n')
                        if (q.answers.isNotEmpty()) append("\n参考答案:\n").append(q.answers.joinToString("\n")).append('\n')
                    }
                    section.structureType == "collector.read" -> append("\n").append(q.number).append(". 模仿朗读原文:\n").append(q.originalText).append('\n')
                    section.structureType == "collector.word" -> append("\n").append(q.number).append(". 词句朗读原文:\n").append(q.originalText).append('\n')
                    else -> {
                        append("\n").append(q.number).append(". ").append(q.text).append('\n')
                        if (q.answers.isNotEmpty()) append("参考答案:\n").append(q.answers.joinToString("\n")).append('\n')
                    }
                }
                if (q.translation.isNotBlank()) append("译文:\n").append(q.translation).append('\n')
                if (q.explanation.isNotBlank()) append("解析:\n").append(q.explanation).append('\n')
            }
        }
        if (document.failures.isNotEmpty()) append("\n未读取的资源:\n").append(document.failures.joinToString("\n")).append('\n')
    }
}
