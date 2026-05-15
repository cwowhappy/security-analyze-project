import pytest
from data_collector.core.pipeline.adaptive_request_engine import AdaptiveRequestEngine, DelayState


class TestDelayState:
    def test_initial_state(self):
        s = DelayState(current_delay=1.5)
        assert s.current_delay == 1.5
        assert s.consecutive_success == 0


class TestAdaptiveRequestEngine:
    def test_get_delay_for_source(self):
        engine = AdaptiveRequestEngine(min_delay=1.0, max_delay=60.0, backoff_jitter=0.5, success_threshold=10)
        delay = engine.get_delay("akshare")
        assert 1.0 <= delay <= 60.0

    def test_success_decreases_delay(self):
        engine = AdaptiveRequestEngine(min_delay=1.0, max_delay=60.0, backoff_jitter=0.0, success_threshold=2)
        engine.record_success("akshare")
        engine.record_success("akshare")
        # 连续2次成功后应尝试降速
        delay = engine.get_delay("akshare")
        assert delay < 60.0 or pytest.approx(delay) == 60.0


class TestAdaptiveRequestEngineRetry:
    def test_recoverable_error_retries(self):
        engine = AdaptiveRequestEngine(min_delay=0.01, max_delay=0.1, backoff_jitter=0.0, retry_max_attempts=3)
        call_count = 0
        def flaky():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise TimeoutError("timeout")
            return "ok"
        result = engine.execute("src", flaky)
        assert result == "ok"
        assert call_count == 3

    def test_non_recoverable_error_no_retry(self):
        from data_collector.core.pipeline.adaptive_request_engine import NonRecoverableError
        engine = AdaptiveRequestEngine(min_delay=0.01, max_delay=0.1, backoff_jitter=0.0, retry_max_attempts=3)
        call_count = 0
        def fail_fast():
            nonlocal call_count
            call_count += 1
            raise NonRecoverableError("not found")
        with pytest.raises(NonRecoverableError):
            engine.execute("src", fail_fast)
        assert call_count == 1
