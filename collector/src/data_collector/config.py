"""采集器配置管理，通过 pydantic-settings 读取环境变量。"""

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """采集器全局配置。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # 数据库
    db_host: str = Field(default="localhost", alias="DB_HOST")
    db_port: int = Field(default=5432, alias="DB_PORT")
    db_name: str = Field(default="db-security-analyze", alias="DB_NAME")
    db_user: str = Field(default="user-security-analyze", alias="DB_USER")
    db_password: str = Field(default="SecurityAnalyze@2026", alias="DB_PASSWORD")
    db_pool_min_size: int = Field(default=1, alias="DB_POOL_MIN_SIZE")
    db_pool_max_size: int = Field(default=5, alias="DB_POOL_MAX_SIZE")

    # Tushare
    tushare_token: str = Field(default="", alias="TUSHARE_TOKEN")

    # 采集策略
    source_max_retries: int = Field(default=3, alias="SOURCE_MAX_RETRIES")
    source_retry_delay: float = Field(default=1.0, alias="SOURCE_RETRY_DELAY")
    source_retry_backoff: float = Field(default=2.0, alias="SOURCE_RETRY_BACKOFF")
    source_request_delay_min: float = Field(default=1.0, alias="SOURCE_REQUEST_DELAY_MIN")
    source_request_delay_max: float = Field(default=3.0, alias="SOURCE_REQUEST_DELAY_MAX")

    # 财务采集并发
    finance_batch_size: int = Field(default=100, alias="FINANCE_BATCH_SIZE")
    finance_max_workers: int = Field(default=1, alias="FINANCE_MAX_WORKERS")
    finance_batch_concurrent_workers: int = Field(
        default=1, alias="FINANCE_BATCH_CONCURRENT_WORKERS"
    )

    # APScheduler
    collector_max_workers: int = Field(default=5, alias="COLLECTOR_MAX_WORKERS")
    collector_misfire_grace_time: int = Field(default=3600, alias="COLLECTOR_MISFIRE_GRACE_TIME")

    # 日志
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    log_format: str = Field(default="text", alias="LOG_FORMAT")

    @property
    def database_url(self) -> str:
        """生成 PostgreSQL 连接 URL。"""
        return (
            f"postgresql://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )


# 全局单例
_settings: Settings | None = None


def get_settings() -> Settings:
    """获取配置单例（懒加载）。"""
    global _settings
    if _settings is None:
        _settings = Settings()
    return _settings
