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
    db_password: str = Field(default="", alias="DB_PASSWORD")
    db_pool_min_size: int = Field(default=1, alias="DB_POOL_MIN_SIZE")
    db_pool_max_size: int = Field(default=5, alias="DB_POOL_MAX_SIZE")

    # Tushare
    tushare_token: str = Field(default="", alias="TUSHARE_TOKEN")

    # 采集策略
    source_request_delay_min: float = Field(default=1.0, alias="SOURCE_REQUEST_DELAY_MIN")
    source_request_delay_max: float = Field(default=3.0, alias="SOURCE_REQUEST_DELAY_MAX")

    # 批次失败率阈值
    batch_fail_threshold: float = Field(default=0.1, alias="BATCH_FAIL_THRESHOLD")

    # 日志
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    log_format: str = Field(default="text", alias="LOG_FORMAT")

    # 采集管道配置
    collection_ttl_hours: int = Field(default=24, alias="COLLECTION_TTL_HOURS")
    collection_batch_size: int = Field(default=20, alias="COLLECTION_BATCH_SIZE")
    adaptive_min_delay: float = Field(default=1.0, alias="ADAPTIVE_MIN_DELAY")
    adaptive_max_delay: float = Field(default=60.0, alias="ADAPTIVE_MAX_DELAY")
    adaptive_backoff_jitter: float = Field(default=0.5, alias="ADAPTIVE_BACKOFF_JITTER")
    adaptive_success_threshold: int = Field(default=10, alias="ADAPTIVE_SUCCESS_THRESHOLD")
    retry_max_attempts: int = Field(default=3, alias="RETRY_MAX_ATTEMPTS")

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
    if not _settings.db_password:
        raise ValueError(
            "数据库密码未配置，请设置 DB_PASSWORD 环境变量或在 .env 文件中配置。"
        )
    return _settings
