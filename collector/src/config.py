from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    database_url: str = "postgresql+psycopg://security:security@localhost:5432/security_analyze"
    log_level: str = "INFO"
    scheduler_timezone: str = "Asia/Shanghai"


settings = Settings()
