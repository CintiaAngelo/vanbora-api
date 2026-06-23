-- =====================================================================
--  VanBora — Reset da senha do root (MySQL 8) + setup da aplicação
-- ---------------------------------------------------------------------
--  Aplicado pelo mysqld via --init-file (roda como root na inicialização).
--
--    1) Redefine a senha do root  -> Vanbora@2026  (cobre @localhost e @%)
--    2) Cria o banco `vanbora`
--    3) Cria o usuário da aplicação `vanbora` -> Vanbora@123
--
--  IF EXISTS evita que o arquivo aborte a inicialização caso um dos hosts
--  de root não exista nesta instalação.
-- =====================================================================

ALTER USER IF EXISTS 'root'@'localhost' IDENTIFIED BY 'Vanbora@2026';
ALTER USER IF EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'Vanbora@2026';
ALTER USER IF EXISTS 'root'@'%'         IDENTIFIED BY 'Vanbora@2026';

CREATE DATABASE IF NOT EXISTS vanbora
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'vanbora'@'localhost' IDENTIFIED BY 'Vanbora@123';
ALTER  USER IF EXISTS     'vanbora'@'localhost' IDENTIFIED BY 'Vanbora@123';
GRANT ALL PRIVILEGES ON vanbora.* TO 'vanbora'@'localhost';
FLUSH PRIVILEGES;
