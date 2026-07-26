package com.example.shiyu.data

import com.example.shiyu.data.entity.ArticleEntity
import com.example.shiyu.data.entity.SentenceEntity
import com.example.shiyu.data.entity.VocabularyEntity
import java.util.UUID

object InitialData {
    val sampleArticles = listOf(
        ArticleEntity(
            id = "art-1",
            title = "The Art of Reading Swiftly & Deeply",
            content = """Reading is not merely an act of decoding symbols on a page; it is a cognitive dialogue between the author and the reader. When you immerse yourself in a well-crafted narrative, your brain activates neural pathways similar to those triggered when experiencing the events in real life.

To master deep reading, one must cultivate deliberate focus. Modern digital distractions often fragment our attention span, reducing our ability to synthesize complex ideas. By dedicating distraction-free intervals to reading, you build mental endurance and enhance linguistic intuition.

Furthermore, acquiring vocabulary in authentic context is far more effective than rote memorization. Encounters with unfamiliar words embedded in meaningful sentences allow your mind to map nuances, connotations, and structural relationships effortlessly.""",
            author = "Language Learning Journal",
            category = "Essay",
            description = "Explore the cognitive benefits of deep reading and contextual vocabulary acquisition.",
            wordCount = 142,
            createdAt = System.currentTimeMillis() - 86400000L * 3,
            mindmapMarkdown = """# Deep Reading
## Benefits
- Activates neural pathways
- Enhances linguistic intuition
- Builds mental endurance
## Strategies
- Distraction-free intervals
- Contextual vocabulary learning"""
        ),
        ArticleEntity(
            id = "art-2",
            title = "The Little Prince - Chapter 1",
            content = """Once when I was six years old I saw a magnificent picture in a book, called True Stories from Nature, about the primeval forest. It was a picture of a boa constrictor in the act of swallowing an animal. Here is a copy of the drawing.

In the book it said: "Boa constrictors swallow their prey whole, without chewing it. After that they are not able to move, and they sleep through the six months that they need for digestion."

I pondered deeply, then, over the adventures of the jungle. And after some work with a colored pencil I succeeded in making my first drawing. My drawing Number One. It looked something like this...""",
            author = "Antoine de Saint-Exupéry",
            category = "Fiction",
            description = "The timeless classic story about childhood imagination and perspective.",
            wordCount = 125,
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            mindmapMarkdown = """# The Little Prince Ch.1
## Key Concepts
- Primeval forest illustration
- Boa constrictor swallowing prey
- Drawing Number One vs Adult Interpretation"""
        )
    )

    val sampleVocabulary = emptyList<VocabularyEntity>()

    val sampleSentences = emptyList<SentenceEntity>()
}
