import os
import sys
from datetime import datetime, timezone
from pathlib import Path

# 将项目源码目录加入 Python 路径，确保测试能导入 src 包。
# pytest 的导入钩子在启动时会缓存 sys.path，插入新路径后需要清空缓存，
# 否则可能出现“No module named 'src'”错误。
PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT / "src"))
sys.path.insert(0, str(PROJECT_ROOT))
sys.path_importer_cache.clear()

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from src.db.session import Base
from tests.factories import SecurityEventFactory

os.environ.setdefault("DATABASE_URL", "sqlite:///./tests/test.db")
os.environ.setdefault("LOG_LEVEL", "DEBUG")


@pytest.fixture(scope="session")
def engine():
    db_url = os.environ["DATABASE_URL"]
    engine = create_engine(db_url, future=True)
    Base.metadata.create_all(bind=engine)
    yield engine
    Base.metadata.drop_all(bind=engine)


@pytest.fixture
def db_session(engine):
    connection = engine.connect()
    transaction = connection.begin()
    session = sessionmaker(bind=connection, join_transaction_mode="create_savepoint")()

    yield session

    session.close()
    transaction.rollback()
    connection.close()


@pytest.fixture
def sample_event(db_session):
    event = SecurityEventFactory()
    db_session.add(event)
    db_session.commit()
    return event


@pytest.fixture
def fixed_now():
    return datetime(2026, 8, 7, 12, 0, 0, tzinfo=timezone.utc)
