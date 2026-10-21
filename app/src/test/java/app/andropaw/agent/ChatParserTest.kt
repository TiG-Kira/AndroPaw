package app.andropaw.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatParserTest {

    @Test
    fun `valid card parses`() {
        val reply = "好的，我看一下。\n<tool_call>\n  <tool_name>VM_STATE</tool_name>\n</tool_call>"
        val skills = ChatParser.parseSkillBlocks(reply)
        assertEquals(1, skills.size)
        assertEquals("VM_STATE", skills[0].first)
    }

    @Test
    fun `malformed reply with orphan closers still parses`() {
        // 用户截图同款：合法卡 + 孤儿闭合标签连发
        val reply = "抱歉！我现在只发一个动作：\n\n<tool_call>\n  <tool_name>VM_STATE</tool_name>\n  <parameter name=\"key\">state</parameter>\n</invoke>\n</parameter>\n</invoke>\n</antml:function_calls>\n</invoke>\n</invoke>\n</invoke>\n</invoke>\n</invoke>"
        val skills = ChatParser.parseSkillBlocks(reply)
        assertEquals(1, skills.size)
        assertEquals("VM_STATE", skills[0].first)
    }

    @Test
    fun `unterminated card at end still parses`() {
        val reply = "看一下。<tool_call>\n  <tool_name>VM_STATE</tool_name>\n"
        val skills = ChatParser.parseSkillBlocks(reply)
        assertEquals(1, skills.size)
        assertEquals("VM_STATE", skills[0].first)
    }

    @Test
    fun `sanitize removes orphan closers and antml`() {
        val dirty = "回复</invoke>\n</parameter>\n</antml:function_calls>\n正文"
        val clean = ChatParser.sanitizeReply(dirty)
        assertTrue(!clean.contains("</invoke>"))
        assertTrue(!clean.contains("antml"))
        assertTrue(clean.contains("回复") && clean.contains("正文"))
    }

    @Test
    fun `sanitize truncates repetition loops`() {
        val dirty = "开始\n</invoke>\n</invoke>\n</invoke>\n</invoke>\n</invoke>\n</invoke>\n</invoke>\n</invoke>\n结束"
        val clean = ChatParser.sanitizeReply(dirty)
        assertTrue("repeats remain: $clean", clean.lines().count { it.contains("invoke") } <= 2)
        assertTrue(clean.contains("结束"))
    }

    @Test
    fun `sanitize keeps valid card intact`() {
        val good = "<tool_call>\n  <tool_name>VM_STATE</tool_name>\n  <parameter name=\"key\">state</parameter>\n</tool_call>"
        val clean = ChatParser.sanitizeReply(good)
        assertTrue(clean.contains("<tool_call>"))
        assertTrue(clean.contains("</tool_call>"))
        assertEquals(1, ChatParser.parseSkillBlocks(clean).size)
    }
}
