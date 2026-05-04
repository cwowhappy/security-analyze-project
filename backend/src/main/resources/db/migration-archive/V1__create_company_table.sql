CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL UNIQUE,
    stock_name VARCHAR(100) NOT NULL,
    industry VARCHAR(100),
    region VARCHAR(50),
    establish_date DATE,
    registered_capital DECIMAL(20,4),
    listing_date DATE,
    market VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_code ON company(stock_code);
CREATE INDEX idx_stock_name ON company(stock_name);
CREATE INDEX idx_market ON company(market);
