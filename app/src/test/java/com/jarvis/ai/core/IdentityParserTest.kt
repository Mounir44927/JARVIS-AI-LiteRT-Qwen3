package com.jarvis.ai.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdentityParserTest {
    @Test fun parses_required_arabic_forms() {
        assertEquals("أحمد", IdentityParser.parse("اسمي أحمد"))
        assertEquals("أحمد", IdentityParser.parse("اسمي هو أحمد"))
        assertEquals("أحمد", IdentityParser.parse("أنا اسمي أحمد"))
        assertEquals("أحمد", IdentityParser.parse("تذكر أن اسمي أحمد"))
        assertEquals("أحمد", IdentityParser.parse("تذكر ان اسمي أحمد"))
    }

    @Test fun parses_required_english_forms() {
        assertEquals("Ahmed", IdentityParser.parse("My name is Ahmed"))
        assertEquals("Ahmed", IdentityParser.parse("Remember that my name is Ahmed"))
    }

    @Test fun rejects_ordinary_sentences() {
        assertNull(IdentityParser.parse("أنا أحب الشاي"))
        assertNull(IdentityParser.parse("أنا ذاهب إلى العمل"))
        assertNull(IdentityParser.parse("I like coffee"))
    }

    @Test fun keeps_multiword_names_and_ignores_following_sentence() {
        assertEquals("عبد الرحمن", IdentityParser.parse("اسمي عبد الرحمن، وأنا أحب القهوة"))
        assertEquals("Ahmed Ali", IdentityParser.parse("My name is Ahmed Ali and I live here"))
    }
}
