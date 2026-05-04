#!/usr/bin/env python3
"""财务报告采集 Session 故障恢复测试 — 基于 Mock"""

import os
import sys
import logging
import unittest
from unittest.mock import MagicMock, patch
from dotenv import load_dotenv

load_dotenv()

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from collector.db.postgres import PostgresDB
from collector.tasks.finance_task import FinanceTask
from collector.monitor import Monitor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


class FinanceTaskRecoveryTest(unittest.TestCase):
    """测试 FinanceTask 的 Session 创建与恢复逻辑"""

    @classmethod
    def setUpClass(cls):
        cls.db = PostgresDB(
            host=os.getenv("DB_HOST", "localhost"),
            port=int(os.getenv("DB_PORT", "5432")),
            database=os.getenv("DB_NAME", "security_analyze"),
            user=os.getenv("DB_USER", "stock"),
            password=os.getenv("DB_PASSWORD", "stock"),
        )
        cls.monitor = Monitor(cls.db)
        # 清理测试数据
        cls.db.execute("DELETE FROM collector_task_progress WHERE session_id LIKE 'test-%'")
        cls.db.execute("DELETE FROM collector_task_log WHERE session_id LIKE 'test-%'")

    @classmethod
    def tearDownClass(cls):
        cls.db.execute("DELETE FROM collector_task_progress WHERE session_id LIKE 'test-%'")
        cls.db.execute("DELETE FROM collector_task_log WHERE session_id LIKE 'test-%'")

    def _create_task(self):
        source = MagicMock()
        source.infer_market = MagicMock(return_value="SH")
        source.get_balance_sheet = MagicMock(return_value=None)
        source.get_profit_sheet = MagicMock(return_value=None)
        source.get_cash_flow_sheet = MagicMock(return_value=None)
        return FinanceTask(db=self.db, source=source, monitor=self.monitor)

    def test_01_create_session_and_record_progress(self):
        """测试新 Session 创建，并记录逐只股票进度"""
        task = self._create_task()

        # Mock _get_stock_codes_from_db 返回固定列表
        with patch.object(task, '_get_stock_codes_from_db', return_value=['600001', '600002', '600003']):
            # Mock _collect_by_stock_code：前两只成功，第三只失败
            def mock_collect(stock_code, **kwargs):
                if stock_code == '600003':
                    raise ValueError("模拟采集失败")
                return 2, 1  # created, updated

            with patch.object(task, '_collect_by_stock_code', side_effect=mock_collect):
                task.run(batch_size=2)

        # 验证 task_log 中应有 session_id
        row = self.db.fetchone(
            "SELECT session_id, status FROM collector_task_log WHERE task_name = 'sync_finance_report' ORDER BY id DESC"
        )
        self.assertIsNotNone(row)
        session_id = row[0]
        self.assertIsNotNone(session_id)
        self.assertEqual(row[1], 'failed')  # 因为 600003 失败

        # 验证 progress 表
        progress_rows = self.db.fetchall(
            "SELECT stock_code, status, rows_created, rows_updated FROM collector_task_progress WHERE session_id = %s ORDER BY stock_code",
            (session_id,),
        )
        self.assertEqual(len(progress_rows), 3)
        self.assertEqual(progress_rows[0], ('600001', 'success', 2, 1))
        self.assertEqual(progress_rows[1], ('600002', 'success', 2, 1))
        self.assertEqual(progress_rows[2][0], '600003')
        self.assertEqual(progress_rows[2][1], 'failed')

        logger.info(f"test_01 passed, session_id={session_id}")
        self.__class__._last_session_id = session_id

    def test_02_resume_session_skip_success(self):
        """测试恢复 Session：跳过已成功的股票，只重试失败的"""
        session_id = getattr(self.__class__, '_last_session_id', None)
        if not session_id:
            self.skipTest("需要先运行 test_01 创建 Session")

        task = self._create_task()

        with patch.object(task, '_get_stock_codes_from_db', return_value=['600001', '600002', '600003']):
            collected_codes = []

            def mock_collect(stock_code, **kwargs):
                collected_codes.append(stock_code)
                if stock_code == '600003':
                    return 1, 0  # 这次成功了
                return 2, 1

            with patch.object(task, '_collect_by_stock_code', side_effect=mock_collect):
                task.run(session_id=session_id)

        # 已成功的 600001、600002 不应被再次采集
        self.assertNotIn('600001', collected_codes)
        self.assertNotIn('600002', collected_codes)
        self.assertIn('600003', collected_codes)

        # 验证最终 progress 状态
        progress_rows = self.db.fetchall(
            "SELECT stock_code, status FROM collector_task_progress WHERE session_id = %s ORDER BY stock_code",
            (session_id,),
        )
        statuses = {r[0]: r[1] for r in progress_rows}
        self.assertEqual(statuses['600001'], 'success')
        self.assertEqual(statuses['600002'], 'success')
        self.assertEqual(statuses['600003'], 'success')

        logger.info(f"test_02 passed, resumed session_id={session_id}")

    def test_03_resume_nonexistent_session(self):
        """测试恢复不存在的 Session 应抛出异常"""
        task = self._create_task()
        with self.assertRaises(ValueError) as ctx:
            task.run(session_id='test-nonexistent-uuid-1234')
        self.assertIn('不存在', str(ctx.exception))
        logger.info("test_03 passed")


if __name__ == "__main__":
    unittest.main(verbosity=2)
