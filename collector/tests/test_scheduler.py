from unittest.mock import MagicMock, patch, ANY
import pytest
from collector.scheduler import Scheduler


class TestScheduler:
    def _create_scheduler(self):
        mock_db = MagicMock()
        scheduler = Scheduler(db=mock_db, db_cfg=None)
        return scheduler

    def test_start_stop(self):
        scheduler = self._create_scheduler()
        with patch.object(scheduler._scheduler, "start") as mock_start, \
             patch.object(scheduler._scheduler, "shutdown") as mock_shutdown:
            scheduler.start()
            mock_start.assert_called_once()
            scheduler.stop()
            mock_shutdown.assert_called_once()

    def test_add_company_job(self):
        scheduler = self._create_scheduler()
        with patch.object(scheduler._scheduler, "get_job", return_value=None), \
             patch.object(scheduler._scheduler, "add_job") as mock_add:
            result = scheduler.add_company_job("0 9 * * *")
            assert result is True
            mock_add.assert_called_once()
            call_kwargs = mock_add.call_args.kwargs
            assert call_kwargs["id"] == "company_task"
            assert call_kwargs["replace_existing"] is True

    def test_reschedule_company_job(self):
        scheduler = self._create_scheduler()
        mock_job = MagicMock()
        with patch.object(scheduler._scheduler, "get_job", return_value=mock_job), \
             patch.object(scheduler._scheduler, "reschedule_job") as mock_reschedule:
            result = scheduler.add_company_job("0 10 * * *")
            assert result is True
            mock_reschedule.assert_called_once_with("company_task", trigger=ANY)

    def test_remove_job(self):
        scheduler = self._create_scheduler()
        with patch.object(scheduler._scheduler, "remove_job") as mock_remove:
            result = scheduler.remove_job("company_task")
            assert result is True
            mock_remove.assert_called_once_with("company_task")

    def test_pause_resume_job(self):
        scheduler = self._create_scheduler()
        with patch.object(scheduler._scheduler, "pause_job") as mock_pause, \
             patch.object(scheduler._scheduler, "resume_job") as mock_resume:
            assert scheduler.pause_job("company_task") is True
            mock_pause.assert_called_once_with("company_task")
            assert scheduler.resume_job("company_task") is True
            mock_resume.assert_called_once_with("company_task")

    def test_list_jobs(self):
        scheduler = self._create_scheduler()
        mock_job = MagicMock()
        mock_job.id = "company_task"
        mock_job.name = "Full Company Sync"
        mock_job.next_run_time = None
        with patch.object(scheduler._scheduler, "get_jobs", return_value=[mock_job]):
            jobs = scheduler.list_jobs()
            assert len(jobs) == 1
            assert jobs[0]["id"] == "company_task"
