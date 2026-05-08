"""Test refactored Scheduler with generic register method."""
from unittest.mock import MagicMock, patch, ANY
import pytest

from collector.scheduler import Scheduler


class TestSchedulerRefactored:
    def test_init_without_db_cfg(self):
        scheduler = Scheduler()
        assert scheduler._scheduler is not None

    def test_start_stop(self):
        scheduler = Scheduler()
        with patch.object(scheduler._scheduler, "start") as mock_start, \
             patch.object(scheduler._scheduler, "shutdown") as mock_shutdown:
            scheduler.start()
            mock_start.assert_called_once()
            scheduler.stop()
            mock_shutdown.assert_called_once_with(wait=True)

    def test_register_new_job(self):
        scheduler = Scheduler()
        with patch.object(scheduler._scheduler, "get_job", return_value=None), \
             patch.object(scheduler._scheduler, "add_job") as mock_add:
            dummy_func = lambda: None
            result = scheduler.register("test_job", "Test Job", "0 9 * * *", dummy_func)
            assert result is True
            mock_add.assert_called_once()
            call_kwargs = mock_add.call_args.kwargs
            assert call_kwargs["id"] == "test_job"
            assert call_kwargs["name"] == "Test Job"
            assert call_kwargs["replace_existing"] is True

    def test_reschedule_existing_job(self):
        scheduler = Scheduler()
        mock_job = MagicMock()
        with patch.object(scheduler._scheduler, "get_job", return_value=mock_job), \
             patch.object(scheduler._scheduler, "reschedule_job") as mock_reschedule:
            dummy_func = lambda: None
            result = scheduler.register("test_job", "Test Job", "0 10 * * *", dummy_func)
            assert result is True
            mock_reschedule.assert_called_once_with("test_job", trigger=ANY)

    def test_register_failure_returns_false(self):
        scheduler = Scheduler()
        with patch.object(scheduler._scheduler, "get_job", side_effect=Exception("boom")):
            dummy_func = lambda: None
            result = scheduler.register("test_job", "Test Job", "invalid", dummy_func)
            assert result is False

    def test_remove_job(self):
        scheduler = Scheduler()
        with patch.object(scheduler._scheduler, "remove_job") as mock_remove:
            result = scheduler.remove_job("test_job")
            assert result is True
            mock_remove.assert_called_once_with("test_job")

    def test_pause_resume_job(self):
        scheduler = Scheduler()
        with patch.object(scheduler._scheduler, "pause_job") as mock_pause, \
             patch.object(scheduler._scheduler, "resume_job") as mock_resume:
            assert scheduler.pause_job("test_job") is True
            mock_pause.assert_called_once_with("test_job")
            assert scheduler.resume_job("test_job") is True
            mock_resume.assert_called_once_with("test_job")

    def test_list_jobs(self):
        scheduler = Scheduler()
        mock_job = MagicMock()
        mock_job.id = "test_job"
        mock_job.name = "Test Job"
        mock_job.next_run_time = None
        with patch.object(scheduler._scheduler, "get_jobs", return_value=[mock_job]):
            jobs = scheduler.list_jobs()
            assert len(jobs) == 1
            assert jobs[0]["id"] == "test_job"
