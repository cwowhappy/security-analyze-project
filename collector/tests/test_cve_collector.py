from src.tasks.cve_collector import CveCollectorTask
from src.models import SecurityEvent


class TestCveCollectorTask:
    def test_task_name(self):
        task = CveCollectorTask()
        assert task.name == "cve_collector"

    def test_execute_writes_events(self, db_session):
        task = CveCollectorTask(session_factory=lambda: db_session)
        result = task.run()

        assert result["collected"] == 1
        events = db_session.query(SecurityEvent).all()
        assert len(events) == 1
        assert events[0].event_type == "CVE"
        assert events[0].severity == "HIGH"
