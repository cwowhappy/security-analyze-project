"""通用装饰器

提供重试、日志等横切关注点封装。
"""
import time
import logging
from functools import wraps
from typing import Callable, Type, Tuple

logger = logging.getLogger(__name__)


def retry(
    max_retries: int = 3,
    delay: float = 2.0,
    backoff: float = 2.0,
    exceptions: Tuple[Type[Exception], ...] = (Exception,),
):
    """指数退避重试装饰器。

    Args:
        max_retries: 最大重试次数（不含首次调用）。
        delay: 初始延迟秒数。
        backoff: 退避乘数。
        exceptions: 需要捕获并重试的异常类型元组。
    """

    def decorator(func: Callable):
        @wraps(func)
        def wrapper(*args, **kwargs):
            attempt = 0
            current_delay = delay
            while True:
                try:
                    return func(*args, **kwargs)
                except exceptions as e:
                    attempt += 1
                    func_name = getattr(func, "__name__", repr(func))
                    if attempt > max_retries:
                        raise
                    logger.warning(
                        f"{func_name} attempt {attempt}/{max_retries} failed: {e}, retrying in {current_delay:.1f}s..."
                    )
                    time.sleep(current_delay)
                    current_delay *= backoff

        return wrapper

    return decorator
