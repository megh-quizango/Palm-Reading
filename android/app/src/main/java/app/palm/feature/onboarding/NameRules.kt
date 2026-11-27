package app.palm.feature.onboarding

const val NAME_MAX = 30

enum class NameCheck { OK, EMPTY, TOO_SHORT, TOO_LONG, BAD_CHARS }

private const val ZWNJ = 0x200C
private const val ZWJ = 0x200D

private fun isMark(cp: Int) = when (Character.getType(cp).toByte()) {
    Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
    else -> false
}

/**
 * First-name rule (D-09): letters, marks after a letter or mark (Devanagari matras, anusvara, virama),
 * ZWJ/ZWNJ inside a word, and a single space, hyphen or apostrophe between letters. At least 2 letters.
 */
fun checkName(raw: String): NameCheck {
    val name = raw.trim()
    if (name.isEmpty()) return NameCheck.EMPTY
    if (name.length > NAME_MAX) return NameCheck.TOO_LONG
    val cps = name.codePoints().toArray()
    var letters = 0
    for ((i, cp) in cps.withIndex()) {
        val prevWordy = i > 0 && (Character.isLetter(cps[i - 1]) || isMark(cps[i - 1]))
        val nextWordy = i < cps.size - 1 && (Character.isLetter(cps[i + 1]) || isMark(cps[i + 1]))
        val ok = when {
            Character.isLetter(cp) -> { letters++; true }
            isMark(cp) -> prevWordy
            cp == ZWJ || cp == ZWNJ -> prevWordy && nextWordy
            cp == ' '.code || cp == '-'.code || cp == '\''.code -> prevWordy && nextWordy
            else -> false
        }
        if (!ok) return NameCheck.BAD_CHARS
    }
    return if (letters < 2) NameCheck.TOO_SHORT else NameCheck.OK
}
