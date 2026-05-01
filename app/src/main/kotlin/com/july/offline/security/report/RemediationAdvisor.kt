package com.july.offline.security.report

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemediationAdvisor @Inject constructor() {

    fun advise(finding: SecurityFinding): Remediation? {
        val banner = finding.rawData
            ?.let { Regex("server:\\s*([^,\\n]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
            ?.lowercase()?.trim() ?: ""

        return when {
            finding.id.contains("TLS_SELF_SIGNED")    -> selfSignedRemediation(banner)
            finding.id.contains("TLS_CERT_EXPIRED")   -> certRenewalRemediation(banner)
            finding.id.contains("TLS_CERT_EXPIRING")  -> certRenewalRemediation(banner)
            finding.id.contains("NET_PORT_") && finding.category == SecurityFinding.FindingCategory.OPEN_PORT -> {
                val port = finding.rawData
                    ?.let { Regex("port:\\s*(\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
                portRemediation(port ?: 0, banner)
            }
            finding.id == "NET_HTTP_NO_REDIRECT"       -> httpRedirectRemediation(banner)
            finding.id.contains("NET_TELNET")          -> telnetRemediation()
            finding.id == "APP_BUILD_001"              -> debugBuildRemediation()
            finding.id == "DEV_OPT_002"                -> usbDebugRemediation()
            finding.id == "DEV_LOCK_001" && finding.severity == SecurityFinding.Severity.CRITICAL -> lockScreenRemediation()
            finding.id == "APP_NET_001"                -> networkSecurityConfigRemediation()
            finding.id == "DEV_ENC_001" && finding.severity == SecurityFinding.Severity.CRITICAL  -> encryptionRemediation()
            else -> null
        }
    }

    // ── Herramientas de red ────────────────────────────────────────────────

    private fun portRemediation(port: Int, banner: String): Remediation = when (port) {
        80 -> httpRedirectRemediation(banner)
        22 -> sshHardenRemediation()
        21 -> ftpRemediation()
        23 -> telnetRemediation()
        3306 -> mysqlRemediation()
        5432 -> postgresRemediation()
        6379 -> redisRemediation()
        9200 -> elasticsearchRemediation()
        27017 -> mongoRemediation()
        3389 -> rdpRemediation()
        5900 -> vncRemediation()
        else -> genericPortRemediation(port)
    }

    private fun httpRedirectRemediation(banner: String) = Remediation(
        title = "Forzar HTTPS — redirigir todo el tráfico HTTP",
        steps = when {
            banner.contains("nginx") -> listOf(
                RemediationStep("Editar el bloque server { listen 80 } en nginx:"),
                RemediationStep(
                    "Agregar dentro del bloque server:",
                    "return 301 https://\$host\$request_uri;"
                ),
                RemediationStep("Verificar y recargar nginx:",
                    "sudo nginx -t && sudo systemctl reload nginx")
            )
            banner.contains("apache") -> listOf(
                RemediationStep("Habilitar módulo rewrite:",
                    "sudo a2enmod rewrite"),
                RemediationStep("En el VirtualHost *:80 agregar:",
                    "Redirect permanent / https://tu-dominio.com/"),
                RemediationStep("Reiniciar Apache:",
                    "sudo systemctl restart apache2")
            )
            else -> listOf(
                RemediationStep("Si el servidor es nginx o Apache, configurar redirect 301 a HTTPS."),
                RemediationStep("Si el puerto 80 no es necesario, cerrarlo:",
                    "sudo ufw deny 80/tcp"),
                RemediationStep("Con iptables:",
                    "sudo iptables -A INPUT -p tcp --dport 80 -j DROP && sudo iptables-save")
            )
        }
    )

    private fun selfSignedRemediation(banner: String) = Remediation(
        title = "Reemplazar certificado autofirmado con Let's Encrypt (gratis)",
        steps = when {
            banner.contains("nginx") -> listOf(
                RemediationStep("Instalar Certbot para nginx:",
                    "sudo apt install certbot python3-certbot-nginx"),
                RemediationStep("Obtener e instalar certificado:",
                    "sudo certbot --nginx -d tu-dominio.com"),
                RemediationStep("Verificar renovación automática:",
                    "sudo certbot renew --dry-run")
            )
            banner.contains("apache") -> listOf(
                RemediationStep("Instalar Certbot para Apache:",
                    "sudo apt install certbot python3-certbot-apache"),
                RemediationStep("Obtener e instalar certificado:",
                    "sudo certbot --apache -d tu-dominio.com"),
                RemediationStep("Verificar renovación automática:",
                    "sudo certbot renew --dry-run")
            )
            else -> listOf(
                RemediationStep("Instalar Certbot:",
                    "sudo apt install certbot"),
                RemediationStep("Obtener certificado (detener el servidor primero):",
                    "sudo certbot certonly --standalone -d tu-dominio.com"),
                RemediationStep("Certificados generados en:",
                    "/etc/letsencrypt/live/tu-dominio.com/"),
                RemediationStep("Configurar cron para renovación automática:",
                    "0 3 * * * certbot renew --quiet")
            )
        }
    )

    private fun certRenewalRemediation(banner: String) = Remediation(
        title = "Renovar certificado TLS",
        steps = listOf(
            RemediationStep("Si usas Let's Encrypt:",
                "sudo certbot renew --force-renewal"),
            RemediationStep("Si es autofirmado, regenerar (válido 365 días):",
                "sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 " +
                    "-keyout /etc/ssl/private/server.key -out /etc/ssl/certs/server.crt"),
            RemediationStep("Reiniciar el servidor web para aplicar el nuevo certificado.")
        )
    )

    private fun sshHardenRemediation() = Remediation(
        title = "Endurecer configuración SSH",
        steps = listOf(
            RemediationStep("Editar /etc/ssh/sshd_config — verificar estas líneas:"),
            RemediationStep("Deshabilitar auth por contraseña:",
                "PasswordAuthentication no"),
            RemediationStep("Deshabilitar login como root:",
                "PermitRootLogin no"),
            RemediationStep("Limitar intentos fallidos:",
                "MaxAuthTries 3"),
            RemediationStep("Aplicar cambios:",
                "sudo systemctl restart sshd"),
            RemediationStep("Limitar SSH a la red local:",
                "sudo ufw allow from 192.168.0.0/16 to any port 22 && sudo ufw deny 22")
        )
    )

    private fun ftpRemediation() = Remediation(
        title = "Migrar FTP → SFTP (SSH File Transfer)",
        steps = listOf(
            RemediationStep("SFTP viene incluido con OpenSSH. Deshabilitar FTP:"),
            RemediationStep("Detener y deshabilitar vsftpd/proftpd:",
                "sudo systemctl stop vsftpd && sudo systemctl disable vsftpd"),
            RemediationStep("Cerrar puerto 21:",
                "sudo ufw deny 21/tcp"),
            RemediationStep("Conectarse via SFTP en vez de FTP:",
                "sftp usuario@servidor")
        )
    )

    private fun telnetRemediation() = Remediation(
        title = "Eliminar Telnet — URGENTE",
        steps = listOf(
            RemediationStep("Detener y deshabilitar Telnet:",
                "sudo systemctl stop telnet.socket && sudo systemctl disable telnet.socket"),
            RemediationStep("Desinstalar si es posible:",
                "sudo apt remove telnetd inetutils-telnetd"),
            RemediationStep("Bloquear en firewall:",
                "sudo ufw deny 23/tcp"),
            RemediationStep("Instalar SSH como reemplazo si no está:",
                "sudo apt install openssh-server && sudo systemctl enable ssh")
        )
    )

    private fun mysqlRemediation() = Remediation(
        title = "Asegurar MySQL — limitar acceso remoto",
        steps = listOf(
            RemediationStep("En /etc/mysql/mysql.conf.d/mysqld.cnf, cambiar bind-address:",
                "bind-address = 127.0.0.1"),
            RemediationStep("Reiniciar MySQL:",
                "sudo systemctl restart mysql"),
            RemediationStep("Ejecutar el asistente de seguridad:",
                "sudo mysql_secure_installation"),
            RemediationStep("Si necesitas acceso remoto, usa túnel SSH:",
                "ssh -L 3306:127.0.0.1:3306 usuario@servidor")
        )
    )

    private fun postgresRemediation() = Remediation(
        title = "Asegurar PostgreSQL",
        steps = listOf(
            RemediationStep("En postgresql.conf, limitar a localhost:",
                "listen_addresses = '127.0.0.1'"),
            RemediationStep("En pg_hba.conf, restringir conexiones."),
            RemediationStep("Reiniciar PostgreSQL:",
                "sudo systemctl restart postgresql"),
            RemediationStep("Para acceso remoto, usar túnel SSH.")
        )
    )

    private fun redisRemediation() = Remediation(
        title = "Asegurar Redis",
        steps = listOf(
            RemediationStep("En /etc/redis/redis.conf:"),
            RemediationStep("Limitar a localhost:",
                "bind 127.0.0.1"),
            RemediationStep("Configurar contraseña fuerte:",
                "requirepass TU_CONTRASENA_AQUI"),
            RemediationStep("Deshabilitar comandos peligrosos:",
                "rename-command FLUSHALL \"\""),
            RemediationStep("Reiniciar Redis:",
                "sudo systemctl restart redis")
        )
    )

    private fun elasticsearchRemediation() = Remediation(
        title = "Asegurar Elasticsearch",
        steps = listOf(
            RemediationStep("En elasticsearch.yml, limitar a localhost:",
                "network.host: 127.0.0.1"),
            RemediationStep("Habilitar autenticación (X-Pack):",
                "xpack.security.enabled: true"),
            RemediationStep("Reiniciar Elasticsearch:",
                "sudo systemctl restart elasticsearch"),
            RemediationStep("Cerrar el puerto en el firewall:",
                "sudo ufw deny 9200/tcp")
        )
    )

    private fun mongoRemediation() = Remediation(
        title = "Asegurar MongoDB",
        steps = listOf(
            RemediationStep("En /etc/mongod.conf:"),
            RemediationStep("Limitar a localhost:",
                "  bindIp: 127.0.0.1"),
            RemediationStep("Habilitar autenticación:",
                "  authorization: enabled"),
            RemediationStep("Reiniciar MongoDB:",
                "sudo systemctl restart mongod"),
            RemediationStep("Crear usuario administrador en mongo shell:",
                "db.createUser({user:'admin', pwd:'CONTRASENA', roles:['root']})")
        )
    )

    private fun rdpRemediation() = Remediation(
        title = "Proteger RDP (escritorio remoto)",
        steps = listOf(
            RemediationStep("Limitar RDP a IPs específicas en el firewall:",
                "netsh advfirewall firewall add rule name=\"RDP\" protocol=TCP dir=in localport=3389 action=allow remoteip=192.168.100.0/24"),
            RemediationStep("Habilitar Network Level Authentication (NLA) — Propiedades del sistema → Acceso remoto."),
            RemediationStep("Considerar cambiar el puerto RDP del estándar 3389."),
            RemediationStep("Usar VPN en vez de exponer RDP directamente.")
        )
    )

    private fun vncRemediation() = Remediation(
        title = "Proteger VNC con túnel SSH",
        steps = listOf(
            RemediationStep("Configurar VNC para escuchar solo en localhost:",
                "vncserver -localhost yes"),
            RemediationStep("Conectarse siempre via túnel SSH:",
                "ssh -L 5900:127.0.0.1:5900 usuario@servidor"),
            RemediationStep("Luego conectar VNC a:",
                "127.0.0.1:5900"),
            RemediationStep("Cerrar puerto VNC en firewall:",
                "sudo ufw deny 5900/tcp")
        )
    )

    private fun genericPortRemediation(port: Int) = Remediation(
        title = "Revisar servicio en puerto $port",
        steps = listOf(
            RemediationStep("Identificar qué proceso usa el puerto:",
                "sudo ss -tlnp | grep :$port"),
            RemediationStep("Si no es necesario externamente, cerrar en firewall:",
                "sudo ufw deny $port/tcp"),
            RemediationStep("Verificar con:",
                "sudo ufw status numbered")
        )
    )

    // ── Herramientas de app/dispositivo ───────────────────────────────────

    private fun debugBuildRemediation() = Remediation(
        title = "Compilar en modo release para producción",
        steps = listOf(
            RemediationStep("Generar APK firmado:",
                "./gradlew assembleRelease"),
            RemediationStep("Verificar que en build.gradle tienes:",
                "minifyEnabled = true\nshrinkResources = true"),
            RemediationStep("Nunca distribuir builds debug a usuarios finales.")
        )
    )

    private fun usbDebugRemediation() = Remediation(
        title = "Deshabilitar USB Debugging",
        steps = listOf(
            RemediationStep("Ir a: Ajustes → Sistema → Opciones de desarrollador"),
            RemediationStep("Desactivar: 'Depuración USB'"),
            RemediationStep("Opcional: desactivar completamente las opciones de desarrollador.")
        )
    )

    private fun lockScreenRemediation() = Remediation(
        title = "Configurar pantalla de bloqueo",
        steps = listOf(
            RemediationStep("Ir a: Ajustes → Seguridad → Bloqueo de pantalla"),
            RemediationStep("Seleccionar PIN (mínimo 6 dígitos) o contraseña alfanumérica."),
            RemediationStep("Configurar bloqueo automático en máximo 5 minutos.")
        )
    )

    private fun encryptionRemediation() = Remediation(
        title = "Habilitar cifrado del dispositivo",
        steps = listOf(
            RemediationStep("Ir a: Ajustes → Seguridad → Cifrado"),
            RemediationStep("Activar 'Cifrar teléfono' o 'Cifrar dispositivo'."),
            RemediationStep("Requiere batería cargada y puede tardar 1 hora."),
            RemediationStep("Configurar PIN fuerte — es la clave de cifrado.")
        )
    )

    private fun networkSecurityConfigRemediation() = Remediation(
        title = "Agregar Network Security Config",
        steps = listOf(
            RemediationStep("Crear res/xml/network_security_config.xml:"),
            RemediationStep(
                "",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<network-security-config>\n" +
                "  <base-config cleartextTrafficPermitted=\"false\">\n" +
                "    <trust-anchors>\n" +
                "      <certificates src=\"system\"/>\n" +
                "    </trust-anchors>\n" +
                "  </base-config>\n" +
                "</network-security-config>"
            ),
            RemediationStep("Referenciar en AndroidManifest.xml dentro de <application>:",
                "android:networkSecurityConfig=\"@xml/network_security_config\"")
        )
    )
}
