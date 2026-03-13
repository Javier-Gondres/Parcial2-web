Aplicación en: http://localhost:7000

## Usuarios por defecto

- admin / 123456 (Administrador)
- autor / 123456 (Autor)

## Variables de entorno

- JDBC_DATABASE_URL: URL de CockroachDB para audit de logins
- JASYPT_PASSWORD: Clave para encriptar cookie "recordar usuario" (opcional)

Ver env.example para más detalles

Para que se pueda encontrar el archivo ".env", este debe de estar
en la raiz del proyecto y llamarse ".env", con las correspondientes variables
de entorno dentro

Repositorio: https://github.com/Javier-Gondres/parcial2