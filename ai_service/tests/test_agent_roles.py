import unittest
from unittest.mock import patch
from langchain_core.messages import AIMessage
from langchain_core.tools import tool
from app.services.agent import execute_tools, AgentState

@tool
def dummy_student_tool(token: str) -> str:
    """Mock student tool"""
    return "学员进度正常"

@tool
def dummy_instructor_tool(token: str) -> str:
    """Mock instructor tool"""
    return "学员列表"

@tool
def dummy_admin_tool(token: str) -> str:
    """Mock admin tool"""
    return "待审核列表"

class TestAgentRoles(unittest.TestCase):

    def setUp(self):
        self.student_tool_call = {
            "name": "get_my_progress",
            "args": {},
            "id": "call_stu"
        }
        self.instructor_tool_call = {
            "name": "get_instructor_students",
            "args": {},
            "id": "call_ins"
        }
        self.admin_tool_call = {
            "name": "get_pending_registrations",
            "args": {},
            "id": "call_adm"
        }
        
        self.mock_tools_map = {
            "get_my_progress": dummy_student_tool,
            "get_instructor_students": dummy_instructor_tool,
            "get_pending_registrations": dummy_admin_tool
        }
    
    def test_student_role_access(self):
        with patch.dict("app.services.agent.TOOLS_MAP", self.mock_tools_map):
            state: AgentState = {
                "messages": [AIMessage(content="", tool_calls=[self.student_tool_call])],
                "token": "token123",
                "role": 3,
                "context": ""
            }
            
            result = execute_tools(state)
            tool_message = result["messages"][0]
            self.assertEqual(tool_message.content, "学员进度正常")
            
            state["messages"] = [AIMessage(content="", tool_calls=[self.instructor_tool_call])]
            result = execute_tools(state)
            tool_message = result["messages"][0]
            self.assertTrue("无权访问" in tool_message.content)
            self.assertTrue("教练专属功能" in tool_message.content)
        
    def test_instructor_role_access(self):
        with patch.dict("app.services.agent.TOOLS_MAP", self.mock_tools_map):
            state: AgentState = {
                "messages": [AIMessage(content="", tool_calls=[self.instructor_tool_call])],
                "token": "token123",
                "role": 2,
                "context": ""
            }
            
            result = execute_tools(state)
            tool_message = result["messages"][0]
            self.assertEqual(tool_message.content, "学员列表")
            
            state["messages"] = [AIMessage(content="", tool_calls=[self.student_tool_call])]
            result = execute_tools(state)
            tool_message = result["messages"][0]
            self.assertTrue("无权访问" in tool_message.content)
            self.assertTrue("学员专属功能" in tool_message.content)

    def test_admin_role_access(self):
        with patch.dict("app.services.agent.TOOLS_MAP", self.mock_tools_map):
            state: AgentState = {
                "messages": [AIMessage(content="", tool_calls=[self.admin_tool_call])],
                "token": "token123",
                "role": 1,
                "context": ""
            }
            
            result = execute_tools(state)
            tool_message = result["messages"][0]
            self.assertEqual(tool_message.content, "待审核列表")
            
            state["messages"] = [AIMessage(content="", tool_calls=[self.student_tool_call])]
            result = execute_tools(state)
            tool_message = result["messages"][0]
            self.assertTrue("无权访问" in tool_message.content)
            self.assertTrue("学员专属功能" in tool_message.content)

if __name__ == "__main__":
    unittest.main()
