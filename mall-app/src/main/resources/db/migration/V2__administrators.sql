CREATE TABLE admin_account (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(32) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, auth_version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_role (id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(64) NOT NULL UNIQUE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_permission (id BIGINT PRIMARY KEY AUTO_INCREMENT,code VARCHAR(64) NOT NULL UNIQUE) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_account_role (id BIGINT PRIMARY KEY AUTO_INCREMENT,account_id BIGINT NOT NULL,role_id BIGINT NOT NULL,
 UNIQUE(account_id,role_id),FOREIGN KEY(account_id) REFERENCES admin_account(id),FOREIGN KEY(role_id) REFERENCES admin_role(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE admin_role_permission (id BIGINT PRIMARY KEY AUTO_INCREMENT,role_id BIGINT NOT NULL,permission_id BIGINT NOT NULL,
 UNIQUE(role_id,permission_id),FOREIGN KEY(role_id) REFERENCES admin_role(id),FOREIGN KEY(permission_id) REFERENCES admin_permission(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO admin_role(id,name) VALUES(1,'超级管理员'),(2,'运营'),(3,'客服');
INSERT INTO admin_permission(code) VALUES ('catalog:write'),('stock:adjust'),('order:read'),('shipment:write'),('aftersale:review'),('member:write'),('rbac:write'),('report:read'),('task:retry'),('audit:read');
INSERT INTO admin_role_permission(role_id,permission_id) SELECT 1,id FROM admin_permission;
INSERT INTO admin_role_permission(role_id,permission_id) SELECT 2,id FROM admin_permission WHERE code IN ('catalog:write','stock:adjust','order:read','shipment:write','report:read');
INSERT INTO admin_role_permission(role_id,permission_id) SELECT 3,id FROM admin_permission WHERE code IN ('order:read','aftersale:review','member:write');
