import pytest
from src.collectors.http_collector import HttpCollector


class TestHttpCollector:
    def test_fetch_json(self, httpx_mock):
        httpx_mock.add_response(
            url="https://example.com/api/data",
            json={"cve_id": "CVE-2026-0001", "severity": "HIGH"},
        )

        collector = HttpCollector()
        data = collector.fetch("https://example.com/api/data")

        assert data["cve_id"] == "CVE-2026-0001"
        assert data["severity"] == "HIGH"
        collector.close()

    def test_fetch_raises_on_error(self, httpx_mock):
        httpx_mock.add_response(url="https://example.com/api/data", status_code=500)

        collector = HttpCollector()
        with pytest.raises(Exception):
            collector.fetch("https://example.com/api/data")
        collector.close()
