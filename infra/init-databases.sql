-- One database per service. Runs once, the first time the postgres container starts.
CREATE DATABASE catalog;
CREATE DATABASE requests;
CREATE DATABASE provisioning;
CREATE DATABASE audit;
CREATE DATABASE notification;
