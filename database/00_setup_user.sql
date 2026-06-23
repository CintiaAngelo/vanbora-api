-- =====================================================================
--  VanBora — Setup inicial do banco e do usuário da aplicação
-- ---------------------------------------------------------------------
--  Execute UMA VEZ no MySQL Workbench conectado como root.
--  Cria o banco `vanbora` e um usuário dedicado que o backend usa por padrão
--  (DB_USERNAME=vanbora / DB_PASSWORD=vanbora), evitando usar o root da máquina.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vanbora
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'vanbora'@'localhost' IDENTIFIED BY 'Vanbora@123';
GRANT ALL PRIVILEGES ON vanbora.* TO 'vanbora'@'localhost';
FLUSH PRIVILEGES;

-- Em seguida (opcional): rode `vanbora_schema.sql` para criar as tabelas,
-- ou apenas inicie o backend — ele cria o schema automaticamente.
