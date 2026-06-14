import unittest
from unittest.mock import patch, MagicMock
from langchain_core.messages import HumanMessage, AIMessage
from app.services.agent import dms_agent

class TestAgent(unittest.TestCase):
    
    @patch('app.services.agent.ChatOpenAI')
    @patch('app.services.agent.Chroma')
    def test_pure_rag_conversation(self, mock_chroma, mock_chat_openai):
        """
        测试纯知识库问答场景（不需要调用工具）
        """
        # 1. 模拟 Chroma 检索结果
        mock_db_instance = MagicMock()
        mock_doc = MagicMock()
        mock_doc.page_content = "关于身体要求：视力必须达到 5.0"
        mock_db_instance.similarity_search.return_value = [mock_doc]
        mock_chroma.return_value = mock_db_instance

        # 2. 模拟大模型直接回复文本（不带 tool_calls）
        mock_llm_instance = MagicMock()
        mock_llm_instance.bind_tools.return_value = mock_llm_instance
        
        mock_ai_message = AIMessage(content="根据规定，视力必须达到 5.0。")
        mock_llm_instance.invoke.return_value = mock_ai_message
        
        mock_chat_openai.return_value = mock_llm_instance

        # 执行 Agent
        result = dms_agent.invoke({
            "messages": [HumanMessage(content="考驾照有什么身体要求？")],
            "token": "dummy_token"
        })

        messages = result["messages"]
        final_message = messages[-1]
        
        # 断言
        self.assertEqual(final_message.content, "根据规定，视力必须达到 5.0。")
        # 确保 Chroma 被调用了
        mock_db_instance.similarity_search.assert_called_once()
        # 确保 LLM 被调用了
        mock_llm_instance.invoke.assert_called_once()

    @patch('app.services.agent.ChatOpenAI')
    @patch('app.services.tools.BackendClient')
    def test_tool_calling_workflow(self, mock_backend_class, mock_chat_openai):
        """
        测试调用工具流程：大模型决策使用工具 -> 工具执行(含 token) -> 再次给大模型合并回答
        """
        # 1. 模拟 BackendClient 获取进度
        mock_backend_instance = MagicMock()
        mock_backend_instance.get.return_value = {
            "code": 200,
            "data": [{"subject": 2, "hours": 12.0, "status": 1}]
        }
        mock_backend_class.return_value = mock_backend_instance

        # 2. 模拟大模型行为
        mock_llm_instance = MagicMock()
        mock_llm_instance.bind_tools.return_value = mock_llm_instance
        
        # 第一次调用 LLM: 返回 tool_calls
        tool_call_msg = AIMessage(
            content="",
            tool_calls=[{
                "name": "get_my_progress",
                "args": {"token": ""},  # 模拟大模型传的空 token，后面图会将其覆盖为真实 token
                "id": "call_123"
            }]
        )
        
        # 第二次调用 LLM: 获取工具执行结果后给出总结
        final_msg = AIMessage(content="您目前的科目二学时为 12.0 小时。")
        
        # 使用 side_effect 模拟两次不同的返回值
        mock_llm_instance.invoke.side_effect = [tool_call_msg, final_msg]
        mock_chat_openai.return_value = mock_llm_instance

        # 执行 Agent
        result = dms_agent.invoke({
            "messages": [HumanMessage(content="查一下我的学习进度")],
            "token": "real_jwt_token_123"
        })

        messages = result["messages"]
        # 我们期待的流程：Human -> AI(tool_calls) -> ToolMessage -> AI(final)
        
        self.assertTrue(len(messages) >= 4)
        
        # 检查最终消息
        self.assertEqual(messages[-1].content, "您目前的科目二学时为 12.0 小时。")
        
        # 检查工具调用时，我们的真实 Token 是否被正确注入
        # 实际上我们这里通过 backend_client 的调用参数来验证：
        mock_backend_class.assert_called_with("real_jwt_token_123")
        mock_backend_instance.get.assert_called_with("/progress/my")

    @patch('app.services.agent.ChatOpenAI')
    def test_instructor_forbidden_from_student_tools(self, mock_chat_openai):
        """
        测试教练员(role=2)尝试调用学员工具时，被 execute_tools 拦截
        """
        mock_llm_instance = MagicMock()
        mock_llm_instance.bind_tools.return_value = mock_llm_instance
        
        # 模拟大模型试图越权调用工具
        tool_call_msg = AIMessage(
            content="",
            tool_calls=[{
                "name": "get_my_progress",
                "args": {"token": ""},
                "id": "call_999"
            }]
        )
        
        # 工具被拦截后返回错误消息反馈给大模型，大模型给出拒绝的最终回复
        final_msg = AIMessage(content="对不起，您目前是教练员身份登录，无权访问或操作学员专属的个人进度。")
        
        mock_llm_instance.invoke.side_effect = [tool_call_msg, final_msg]
        mock_chat_openai.return_value = mock_llm_instance

        # 执行 Agent，设置 role = 2 (教练员)
        result = dms_agent.invoke({
            "messages": [HumanMessage(content="帮我查一下进度")],
            "token": "instructor_token_xyz",
            "role": 2
        })

        messages = result["messages"]
        # 流程中应当包含 ToolMessage，且 ToolMessage 的内容应包含“无权访问”的错误提示
        tool_message_found = False
        for msg in messages:
            if msg.__class__.__name__ == 'ToolMessage':
                self.assertIn("无权访问", msg.content)
                tool_message_found = True
                break
        
        self.assertTrue(tool_message_found)
        self.assertEqual(messages[-1].content, "对不起，您目前是教练员身份登录，无权访问或操作学员专属的个人进度。")

if __name__ == '__main__':
    unittest.main()
