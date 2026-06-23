-- =====================================================================
--  VanBora — Esquema do banco de dados (MySQL 8 / MySQL Workbench)
-- ---------------------------------------------------------------------
--  Uso:
--   1) Abra este arquivo no MySQL Workbench e execute (raio ⚡).
--   2) Ele cria o schema `vanbora` e todas as tabelas.
--
--  Observação: o backend (Spring Boot) também cria/atualiza estas tabelas
--  automaticamente (spring.jpa.hibernate.ddl-auto=update) e popula dados de
--  demonstração na primeira execução. Este script é o esquema de referência
--  e permite inspecionar a base no Workbench antes mesmo de subir a API.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vanbora
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE vanbora;

-- ---------- Identidade / Usuários ----------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL
);

-- ---------- Perfil Responsável ----------
CREATE TABLE IF NOT EXISTS guardian_profiles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL UNIQUE,
    cpf          VARCHAR(255),
    cep          VARCHAR(255),
    city         VARCHAR(255),
    neighborhood VARCHAR(255),
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT fk_guardian_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS dependents (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    guardian_id BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    school      VARCHAR(255) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT fk_dependent_guardian FOREIGN KEY (guardian_id) REFERENCES guardian_profiles (id)
);

-- ---------- Perfil Transportador ----------
CREATE TABLE IF NOT EXISTS transporter_profiles (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT NOT NULL UNIQUE,
    document          VARCHAR(255),
    cnh               VARCHAR(255),
    plate             VARCHAR(255),
    capacity          INT,
    years_experience  INT,
    rating_avg        DECIMAL(3,2),
    reviews_count     INT,
    base_monthly_fee  DECIMAL(10,2),
    available_seats   INT,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    CONSTRAINT fk_transporter_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS transporter_schools (
    transporter_id BIGINT NOT NULL,
    school         VARCHAR(255),
    CONSTRAINT fk_tschool_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

CREATE TABLE IF NOT EXISTS transporter_neighborhoods (
    transporter_id BIGINT NOT NULL,
    neighborhood   VARCHAR(255),
    CONSTRAINT fk_tneighborhood_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

CREATE TABLE IF NOT EXISTS price_zones (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transporter_id BIGINT NOT NULL,
    name           VARCHAR(255) NOT NULL,
    school         VARCHAR(255),
    monthly_fee    DECIMAL(10,2) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_pricezone_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

CREATE TABLE IF NOT EXISTS price_zone_neighborhoods (
    price_zone_id BIGINT NOT NULL,
    neighborhood  VARCHAR(255),
    CONSTRAINT fk_pzn_pricezone FOREIGN KEY (price_zone_id) REFERENCES price_zones (id)
);

CREATE TABLE IF NOT EXISTS helpers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transporter_id BIGINT NOT NULL,
    name           VARCHAR(255) NOT NULL,
    role           VARCHAR(255) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_helper_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transporter_id BIGINT NOT NULL,
    author_name    VARCHAR(255) NOT NULL,
    rating         INT NOT NULL,
    comment        VARCHAR(500),
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_review_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

-- ---------- Matrículas / Alunos ----------
CREATE TABLE IF NOT EXISTS enrollments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transporter_id BIGINT NOT NULL,
    dependent_id   BIGINT NOT NULL,
    monthly_fee    DECIMAL(10,2) NOT NULL,
    finance_status VARCHAR(20) NOT NULL,
    active         BIT(1) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_enrollment_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id),
    CONSTRAINT fk_enrollment_dependent   FOREIGN KEY (dependent_id)   REFERENCES dependents (id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    day_of_week   VARCHAR(12) NOT NULL,
    present       BIT(1) NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
);

CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id  BIGINT NOT NULL,
    reference_month VARCHAR(7) NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    due_date       DATE,
    paid_at        DATE,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_payment_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
);

-- ---------- Contratação ----------
CREATE TABLE IF NOT EXISTS hire_requests (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    guardian_id    BIGINT NOT NULL,
    transporter_id BIGINT NOT NULL,
    dependent_id   BIGINT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_hire_guardian    FOREIGN KEY (guardian_id)    REFERENCES guardian_profiles (id),
    CONSTRAINT fk_hire_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id),
    CONSTRAINT fk_hire_dependent   FOREIGN KEY (dependent_id)   REFERENCES dependents (id)
);

-- ---------- Avisos ----------
CREATE TABLE IF NOT EXISTS notices (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    transporter_id   BIGINT NOT NULL,
    message          VARCHAR(600) NOT NULL,
    recipients_count INT NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    CONSTRAINT fk_notice_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

-- ---------- Rota do dia ----------
CREATE TABLE IF NOT EXISTS route_stops (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transporter_id BIGINT NOT NULL,
    label          VARCHAR(255) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    position       INT NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_routestop_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

-- ---------- Mensageria ----------
CREATE TABLE IF NOT EXISTS conversations (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    guardian_id    BIGINT NOT NULL,
    transporter_id BIGINT NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT fk_conversation_guardian    FOREIGN KEY (guardian_id)    REFERENCES guardian_profiles (id),
    CONSTRAINT fk_conversation_transporter FOREIGN KEY (transporter_id) REFERENCES transporter_profiles (id)
);

CREATE TABLE IF NOT EXISTS messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id       BIGINT NOT NULL,
    text            VARCHAR(1000) NOT NULL,
    sent_at         DATETIME(6) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_message_sender       FOREIGN KEY (sender_id)       REFERENCES users (id)
);

-- Observação: o MySQL cria automaticamente índices para cada chave estrangeira
-- acima (colunas *_id), portanto não são necessários índices adicionais aqui.
