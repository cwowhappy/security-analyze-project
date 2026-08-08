import signal
import sys
import time

from loguru import logger

from src.config import settings
from src.scheduler import TaskScheduler

logger.add("logs/collector.log", rotation="10 MB", retention="7 days", level=settings.log_level)

scheduler = TaskScheduler()


def signal_handler(signum, frame):
    logger.info("Received signal {}, exiting...".format(signum))
    scheduler.shutdown()
    sys.exit(0)


signal.signal(signal.SIGTERM, signal_handler)
signal.signal(signal.SIGINT, signal_handler)


def main():
    scheduler.start()
    logger.info("Collector service is running")
    while True:
        time.sleep(1)


if __name__ == "__main__":
    main()
