CREATE TABLE IF NOT EXISTS stock (
    id VARCHAR(32) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    market VARCHAR(20),
    current_price DECIMAL(18, 4),
    change_percent DECIMAL(18, 4),
    updated_at TIMESTAMP
);

CREATE INDEX idx_stock_symbol ON stock(symbol);
