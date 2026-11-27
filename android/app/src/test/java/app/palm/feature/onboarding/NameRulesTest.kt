package app.palm.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

/** Research Pitfall 4 table: Devanagari matras are marks, not letters. */
class NameRulesTest {
    @Test fun latinName() = assertEquals(NameCheck.OK, checkName("Asha"))

    @Test fun oneLetterIsTooShort() = assertEquals(NameCheck.TOO_SHORT, checkName("A"))

    @Test fun devanagariWithMatra() = assertEquals(NameCheck.OK, checkName("रवि"))

    @Test fun devanagariWithAnusvara() = assertEquals(NameCheck.OK, checkName("अंकित"))

    @Test fun hyphenBetweenLetters() = assertEquals(NameCheck.OK, checkName("Mary-Jane"))

    @Test fun apostropheBetweenLetters() = assertEquals(NameCheck.OK, checkName("O'Neil"))

    @Test fun trailingSpacesAreTrimmed() = assertEquals(NameCheck.OK, checkName("Al  "))

    @Test fun leadingHyphen() = assertEquals(NameCheck.BAD_CHARS, checkName("-Al"))

    @Test fun digit() = assertEquals(NameCheck.BAD_CHARS, checkName("A1"))

    @Test fun doubleSpace() = assertEquals(NameCheck.BAD_CHARS, checkName("Ma  ry"))

    @Test fun emoji() = assertEquals(NameCheck.BAD_CHARS, checkName("Asha😀"))

    @Test fun thirtyOneLetters() = assertEquals(NameCheck.TOO_LONG, checkName("a".repeat(NAME_MAX + 1)))

    @Test fun thirtyLetters() = assertEquals(NameCheck.OK, checkName("a".repeat(NAME_MAX)))

    @Test fun empty() = assertEquals(NameCheck.EMPTY, checkName(""))

    @Test fun blank() = assertEquals(NameCheck.EMPTY, checkName("   "))

    @Test fun conjunctWithVirama() = assertEquals(NameCheck.OK, checkName("लक्ष्मी"))

    @Test fun controlCharacter() = assertEquals(NameCheck.BAD_CHARS, checkName("As\u0007ha"))
}
