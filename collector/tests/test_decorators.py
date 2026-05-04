import time
from unittest.mock import MagicMock
import pytest
from collector.decorators import retry


class TestRetry:
    def test_success_no_retry(self):
        mock = MagicMock(return_value=42)
        decorated = retry(max_retries=2)(mock)
        assert decorated() == 42
        assert mock.call_count == 1

    def test_retry_then_success(self):
        mock = MagicMock(side_effect=[ValueError("fail"), 42])
        decorated = retry(max_retries=2, delay=0.01)(mock)
        assert decorated() == 42
        assert mock.call_count == 2

    def test_max_retries_exceeded(self):
        mock = MagicMock(side_effect=ValueError("always fail"))
        decorated = retry(max_retries=2, delay=0.01)(mock)
        with pytest.raises(ValueError, match="always fail"):
            decorated()
        assert mock.call_count == 3  # initial + 2 retries

    def test_specific_exception_only(self):
        mock = MagicMock(side_effect=[RuntimeError("bad"), 42])
        decorated = retry(max_retries=2, delay=0.01, exceptions=(ValueError,))(mock)
        with pytest.raises(RuntimeError, match="bad"):
            decorated()
        assert mock.call_count == 1
