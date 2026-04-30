#!/usr/bin/env python3
"""
数据采集模块入口
"""
import os
import time
import logging
from dotenv import load_dotenv

from collector.scheduler import Scheduler
from collector.db.postgres import PostgresDB

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def main():
    logger.info("Starting security analyze collector...")

    db = PostgresDB(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        database=os.getenv("DB_NAME", "security_analyze"),
        user=os.getenv("DB_USER", "security"),
        password=os.getenv("DB_PASSWORD", "security"),
    )

    scheduler = Scheduler(db=db)
    scheduler.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Shutting down collector...")
        scheduler.stop()


if __name__ == "__main__":
    main()
