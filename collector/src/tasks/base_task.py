from abc import ABC, abstractmethod
from typing import Any

from loguru import logger


class BaseTask(ABC):
    name: str = "base"

    def __init__(self, **kwargs: Any):
        self.kwargs = kwargs

    def run(self) -> Any:
        logger.info(f"Task {self.name} started")
        try:
            result = self.execute()
            logger.info(f"Task {self.name} finished")
            return result
        except Exception as e:
            logger.error(f"Task {self.name} failed: {e}")
            raise

    @abstractmethod
    def execute(self) -> Any:
        pass
