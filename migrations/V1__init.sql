CREATE TABLE parents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  child VARCHAR(255) NOT NULL
);

CREATE TABLE payment_methods (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT,
  method VARCHAR(255) NOT NULL,
  is_active BOOLEAN NOT NULL,
  FOREIGN KEY (parent_id) REFERENCES parents (id)
);

CREATE TABLE invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT,
  amount DECIMAL(10, 2) NOT NULL,
  date DATE NOT NULL,
  FOREIGN KEY (parent_id) REFERENCES parents (id)
);

INSERT INTO
  parents (name, child)
VALUES
  ('Mary Rose', 'Jane Rose');

INSERT INTO
  payment_methods (parent_id, method, is_active)
VALUES
  (1, 'Mastercard', true),
  (1, 'Visa', false),
  (1, 'American Express', false);

INSERT INTO
  invoices (parent_id, amount, date)
VALUES
  (1, 100.00, '2020-01-01'),
  (1, 200.00, '2020-02-01'),
  (1, 300.00, '2020-03-01'),
  (1, 400.00, '2020-04-01'),
  (1, 500.00, '2020-05-01');

ALTER TABLE parent_profile.payment_methods ADD created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
