from typing import Callable

from sqlalchemy.orm import Session

from src.tasks.base_task import BaseTask
from src.models import SecurityEvent
from src.db.session import SessionLocal


class CveCollectorTask(BaseTask):
    name = "cve_collector"

    def __init__(self, session_factory: Callable[[], Session] = SessionLocal, **kwargs):
        super().__init__(**kwargs)
        self.session_factory = session_factory

    def execute(self):
        # TODO: 实现从 NVD 或 CVE 数据源采集
        events = [
            SecurityEvent(
                title="CVE-2026-0001 示例漏洞",
                description="这是一个示例 CVE 采集结果",
                severity="HIGH",
                source="NVD",
                event_type="CVE",
                raw_data={"cve_id": "CVE-2026-0001"},
            )
        ]
        db = self.session_factory()
        try:
            for event in events:
                db.add(event)
            db.commit()
        finally:
            db.close()
        return {"collected": len(events)}
