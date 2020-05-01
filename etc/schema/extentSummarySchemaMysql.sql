CREATE TABLE IF NOT EXISTS extentSummary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  productSummaryIndexId BIGINT NOT NULL,
  starttime BIGINT DEFAULT NULL,
  endtime BIGINT DEFAULT NULL,
  minlatitude DOUBLE DEFAULT NULL,
  maxlatitude DOUBLE DEFAULT NULL,
  minlongitude DOUBLE DEFAULT NULL,
  maxlongitude DOUBLE DEFAULT NULL,
) ENGINE = INNODB;
