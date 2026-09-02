-- =====================================================================
--  VanBora — Setup inicial do banco e do usuário da aplicação
-- ---------------------------------------------------------------------
--  Execute UMA VEZ no MySQL Workbench conectado como root.
--  Cria o banco `vanbora` e um usuário dedicado para o backend, evitando
--  usar o root da máquina.
--
--  ANTES DE EXECUTAR: troque TROQUE_ESTA_SENHA por uma senha sua e use a
--  mesma em DB_PASSWORD (backend/.env — veja backend/.env.example).
--
--  Nenhuma senha real deve ser salva neste arquivo: ele é versionado, e o
--  repositório é o último lugar onde uma credencial deveria estar.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vanbora
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'vanbora'@'localhost' IDENTIFIED BY 'TROQUE_ESTA_SENHA';
GRANT ALL PRIVILEGES ON vanbora.* TO 'vanbora'@'localhost';
FLUSH PRIVILEGES;

-- Em seguida (opcional): rode `vanbora_schema.sql` para criar as tabelas,
-- ou apenas inicie o backend — ele cria o schema automaticamente.
