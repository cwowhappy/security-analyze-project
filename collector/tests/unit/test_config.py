"""配置管理单元测试。"""

import os

from data_collector.config import Settings


class TestSettings:
    """配置管理测试。"""

    def test_should_have_default_values(self) -> None:
        settings = Settings()
        assert settings.db_host == "localhost"
        assert settings.db_port == 5432
        assert settings.log_level == "INFO"

    def test_should_generate_database_url(self) -> None:
        settings = Settings()
        url = settings.database_url
        assert url.startswith("postgresql://")
        assert "localhost" in url

    def test_should_override_from_env(self) -> None:
        os.environ["DB_HOST"] = "test-host"
        os.environ["LOG_LEVEL"] = "DEBUG"
        try:
            settings = Settings()
            assert settings.db_host == "test-host"
            assert settings.log_level == "DEBUG"
        finally:
            del os.environ["DB_HOST"]
            del os.environ["LOG_LEVEL"]
