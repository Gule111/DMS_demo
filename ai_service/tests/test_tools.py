import unittest
from unittest.mock import patch, MagicMock
from app.services.tools import (
    get_my_progress,
    get_my_instructor,
    get_my_appointments,
    book_training_session,
    cancel_appointment
)
import json

class TestTools(unittest.TestCase):
    def setUp(self):
        self.dummy_token = "dummy_jwt_token"

    @patch('app.services.tools.BackendClient')
    def test_get_my_progress(self, mock_client_class):
        mock_instance = MagicMock()
        mock_client_class.return_value = mock_instance
        # 模拟后端成功返回进度数据
        mock_instance.get.return_value = {
            "code": 200,
            "data": [{"subject": 2, "hours": 10.5, "status": 1}]
        }
        
        result = get_my_progress.invoke({"token": self.dummy_token})
        self.assertIn("10.5", result)
        mock_instance.get.assert_called_with("/progress/my")

    @patch('app.services.tools.BackendClient')
    def test_get_my_instructor_success(self, mock_client_class):
        mock_instance = MagicMock()
        mock_client_class.return_value = mock_instance
        mock_instance.get.return_value = {
            "code": 200,
            "data": {"realName": "王教练", "phone": "13800138000"}
        }
        
        result = get_my_instructor.invoke({"token": self.dummy_token})
        self.assertIn("王教练", result)
        self.assertIn("13800138000", result)

    @patch('app.services.tools.BackendClient')
    def test_get_my_appointments(self, mock_client_class):
        mock_instance = MagicMock()
        mock_client_class.return_value = mock_instance
        mock_instance.get.return_value = {
            "code": 200,
            "data": [{"appointmentDate": "2026-06-15", "timeSlot": "08:00-10:00", "status": 1}]
        }
        
        result = get_my_appointments.invoke({"token": self.dummy_token})
        self.assertIn("2026-06-15", result)
        self.assertIn("08:00-10:00", result)

    @patch('app.services.tools.BackendClient')
    def test_book_training_session(self, mock_client_class):
        mock_instance = MagicMock()
        mock_client_class.return_value = mock_instance
        mock_instance.post.return_value = {
            "code": 200,
            "msg": "预约成功"
        }
        
        args = {
            "token": self.dummy_token,
            "appointment_date": "2026-06-16",
            "time_slot": "10:00-12:00",
            "subject": 2
        }
        result = book_training_session.invoke(args)
        self.assertIn("预约成功", result)
        mock_instance.post.assert_called_once()

    @patch('app.services.tools.BackendClient')
    def test_cancel_appointment(self, mock_client_class):
        mock_instance = MagicMock()
        mock_client_class.return_value = mock_instance
        mock_instance.post.return_value = {
            "code": 200,
            "msg": "取消成功"
        }
        
        result = cancel_appointment.invoke({"token": self.dummy_token, "appointment_id": 101})
        self.assertIn("101", result)
        self.assertIn("已成功取消", result)
        mock_instance.post.assert_called_once_with("/appointment/cancel/101")

if __name__ == '__main__':
    unittest.main()
