"""集中配置管理模块

所有环境变量和默认值统一在此管理，避免各脚本硬编码重复。
"""
import os
from dataclasses import dataclass


@dataclass(frozen=True)
class DBConfig:
    host: str
    port: int
    database: str
    user: str
    password: str

    @classmethod
    def from_env(cls) -> "DBConfig":
        return cls(
            host=os.getenv("DB_HOST", "localhost"),
            port=int(os.getenv("DB_PORT", "5432")),
            database=os.getenv("DB_NAME", "security_analyze"),
            user=os.getenv("DB_USER", "stock"),
            password=os.getenv("DB_PASSWORD", "stock"),
        )


@dataclass(frozen=True)
class CollectorConfig:
    db: DBConfig
    # 连接池配置
    db_pool_min_size: int = 1
    db_pool_max_size: int = 5
    db_pool_max_idle: float = 300.0
    db_pool_max_lifetime: float = 3600.0
    # 采集重试配置
    source_max_retries: int = 3
    source_retry_delay: float = 2.0
    source_retry_backoff: float = 2.0
    # 财务采集默认批次
    finance_batch_size: int = 100
    # 并发配置（单只股票三张报表并发）
    finance_max_workers: int = 3
    # 批次内多股票并发配置
    finance_batch_concurrent_workers: int = 3

    @classmethod
    def from_env(cls) -> "CollectorConfig":
        return cls(
            db=DBConfig.from_env(),
            db_pool_min_size=int(os.getenv("DB_POOL_MIN_SIZE", "1")),
            db_pool_max_size=int(os.getenv("DB_POOL_MAX_SIZE", "5")),
            db_pool_max_idle=float(os.getenv("DB_POOL_MAX_IDLE", "300")),
            db_pool_max_lifetime=float(os.getenv("DB_POOL_MAX_LIFETIME", "3600")),
            source_max_retries=int(os.getenv("SOURCE_MAX_RETRIES", "3")),
            source_retry_delay=float(os.getenv("SOURCE_RETRY_DELAY", "2.0")),
            source_retry_backoff=float(os.getenv("SOURCE_RETRY_BACKOFF", "2.0")),
            finance_batch_size=int(os.getenv("FINANCE_BATCH_SIZE", "100")),
            finance_max_workers=int(os.getenv("FINANCE_MAX_WORKERS", "3")),
            finance_batch_concurrent_workers=int(os.getenv("FINANCE_BATCH_CONCURRENT_WORKERS", "3")),
        )
