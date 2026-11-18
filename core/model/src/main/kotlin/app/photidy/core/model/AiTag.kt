package app.photidy.core.model

import kotlinx.serialization.Serializable

/** Coarse labels produced by the optional on-device model. */
@Serializable
enum class AiTag {
    FOOD,
    PET,
    DOCUMENT,
    NATURE,
    LANDMARK,
}

/** Downloadable model sizes. Larger tiers trade speed for better recognition. */
@Serializable
enum class AiModelTier {
    LIGHT,
    BALANCED,
    ACCURATE,
}

/**
 * Maps ImageNet-style label strings onto a handful of friendly categories. Pure and
 * deterministic so the mapping can be unit tested without the model or Android.
 */
object AiTagging {

    private val words: Map<AiTag, List<String>> = mapOf(
        AiTag.FOOD to listOf(
            "pizza", "hamburger", "hotdog", "sandwich", "bagel", "pretzel", "burrito",
            "guacamole", "salad", "ice cream", "icecream", "banana", "orange", "lemon",
            "pineapple", "strawberry", "fig", "pomegranate", "broccoli", "cauliflower",
            "zucchini", "cucumber", "artichoke", "bell pepper", "cabbage",
            "spaghetti squash", "acorn squash", "butternut squash", "coffee mug",
            "coffeepot", "espresso", "cup", "plate", "wine bottle", "beer bottle",
            "beer glass", "cocktail shaker", "carbonara", "meat loaf", "potpie",
            "trifle", "cheeseburger", "french loaf", "dough", "chocolate sauce",
            "consomme", "hot pot",
        ),
        AiTag.PET to listOf(
            "dog", "cat", "tabby", "hamster", "guinea pig", "rabbit", "parrot", "macaw",
            "cockatoo", "lorikeet", "canary", "goldfish", "kitten", "puppy",
        ),
        AiTag.DOCUMENT to listOf(
            "web site", "menu", "envelope", "notebook", "binder", "book jacket",
            "comic book", "laptop", "desktop computer", "monitor", "screen", "keyboard",
            "mouse", "printer", "photocopier", "filing cabinet", "crossword", "packet",
        ),
        AiTag.NATURE to listOf(
            "seashore", "lakeside", "lakeshore", "promontory", "sandbar", "cliff",
            "coral reef", "geyser", "volcano", "alp", "valley", "daisy", "sunflower",
            "rapeseed", "coral fungus", "earthstar", "stinkhorn", "agaric", "bolete",
            "gyromitra", "hen-of-the-woods", "yellow lady's slipper", "dragonfly",
            "butterfly", "monarch", "bee",
        ),
        AiTag.LANDMARK to listOf(
            "castle", "palace", "monastery", "church", "mosque", "stupa", "lighthouse",
            "suspension bridge", "steel arch bridge", "viaduct", "dock", "fountain",
            "obelisk", "triumphal arch", "bell cote", "boathouse", "greenhouse", "barn",
            "cinema", "bookshop", "grocery store",
        ),
    )

    private data class Rule(val tag: AiTag, val regex: Regex)

    private val rules: List<Rule> = words.flatMap { (tag, keywords) ->
        keywords.map { keyword ->
            Rule(tag, Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE))
        }
    }

    /** Returns every category that matches any of [labels], in enum order. */
    fun tagsFor(labels: List<String>): List<AiTag> {
        if (labels.isEmpty()) return emptyList()
        val text = labels.joinToString(" ")
        return rules.asSequence()
            .filter { it.regex.containsMatchIn(text) }
            .map { it.tag }
            .distinct()
            .sortedBy { it.ordinal }
            .toList()
    }
}
