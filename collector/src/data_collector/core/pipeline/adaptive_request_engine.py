"""自适应请求引擎：根据接口响应动态调节调用间隔。"""

import random
import time
from dataclasses import dataclass
from typing import Callable


class NonRecoverableError(Exception):
    """不可恢复的错误，触发后不再重试。"""


@dataclass
class DelayState:
    current_delay: float
    consecutive_success: int = 0


class AdaptiveRequestEngine:
    def __init__(
        self,
        min_delay: float = 1.0,
        max_delay: float = 60.0,
        backoff_jitter: float = 0.5,
        success_threshold: int = 10,
        retry_max_attempts: int = 3,
    ) -> None:
        self._min_delay = min_delay
        self._max_delay = max_delay
        self._backoff_jitter = backoff_jitter
        self._success_threshold = success_threshold
        self._retry_max_attempts = retry_max_attempts
        self._states: dict[str, DelayState] = {}

    def _get_state(self, source: str) -> DelayState:
        if source not in self._states:
            self._states[source] = DelayState(
                current_delay=random.uniform(self._min_delay, min(self._min_delay * 2, self._max_delay))
            )
        return self._states[source]

    def get_delay(self, source: str) -> float:
        return self._get_state(source).current_delay

    def record_success(self, source: str) -> None:
        state = self._get_state(source)
        state.consecutive_success += 1
        if state.consecutive_success >= self._success_threshold:
            state.current_delay = max(state.current_delay * 0.9, self._min_delay)
            state.consecutive_success = 0

    def record_failure(self, source: str, recoverable: bool = True) -> None:
        state = self._get_state(source)
        state.consecutive_success = 0
        if recoverable:
            jitter = random.uniform(0, self._backoff_jitter)
            state.current_delay = min(state.current_delay * 2 + jitter, self._max_delay)

    def sleep(self, source: str) -> None:
        time.sleep(self.get_delay(source))

    def execute(self, source: str, fn: Callable, *args, **kwargs):
        self.sleep(source)
        last_exception = None
        for attempt in range(self._retry_max_attempts):
            try:
                result = fn(*args, **kwargs)
                self.record_success(source)
                return result
            except NonRecoverableError:
                self.record_failure(source, recoverable=False)
                raise
            except Exception as e:
                last_exception = e
                self.record_failure(source, recoverable=True)
                if attempt < self._retry_max_attempts - 1:
                    self.sleep(source)
        raise last_exception
