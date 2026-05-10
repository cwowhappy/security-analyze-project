"""采集器统一入口。

直接运行模块时默认进入 CLI 模式：
    python -m data_collector --help

等价于：
    python -m data_collector.cli --help
"""

import sys

from data_collector.cli import main

if __name__ == "__main__":
    sys.exit(main())
