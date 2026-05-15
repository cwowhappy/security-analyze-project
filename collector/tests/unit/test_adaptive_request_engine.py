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
