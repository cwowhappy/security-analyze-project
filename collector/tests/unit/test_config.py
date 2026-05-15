"""配置管理单元测试。"""

import os

import pytest

from data_collector.config import Settings, get_settings


class TestSettings:
    """配置管理测试。"""

    def test_should_have_default_values(self) -> None:
        settings = Settings()
        assert settings.db_host == "localhost"
        assert settings.db_port == 5432
        assert settings.log_level == "INFO"

    def test_should_generate_database_url(self) -> None:
        settings = Settings()
        # 设置密码以验证 URL 生成
        settings.db_password = "test-password"
        url = settings.database_url
        assert url.startswith("postgresql://")
        assert "localhost" in url
        assert "test-password" in url

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

    def test_should_raise_when_password_empty(self) -> None:
        """当数据库密码为空时，get_settings 应抛出 ValueError。"""
        from data_collector import config as config_module

        # 重置全局单例，确保测试独立
        config_module._settings = None
        os.environ["DB_PASSWORD"] = ""
        try:
            with pytest.raises(ValueError, match="数据库密码未配置"):
                get_settings()
        finally:
            del os.environ["DB_PASSWORD"]
            config_module._settings = None


def test_collection_settings_defaults():
    from data_collector.config import Settings
    s = Settings()
    assert s.collection_ttl_hours == 24
    assert s.collection_batch_size == 20
    assert s.adaptive_min_delay == 1.0
    assert s.adaptive_max_delay == 60.0
    assert s.adaptive_backoff_jitter == 0.5
    assert s.adaptive_success_threshold == 10
    assert s.retry_max_attempts == 3
