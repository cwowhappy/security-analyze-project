import httpx
from loguru import logger


class HttpCollector:
    def __init__(self, timeout: float = 30.0):
        self.timeout = timeout
        self.client = httpx.Client(timeout=timeout)

    def fetch(self, url: str, **kwargs):
        logger.debug(f"Fetching {url}")
        response = self.client.get(url, **kwargs)
        response.raise_for_status()
        return response.json()

    def close(self):
        self.client.close()
