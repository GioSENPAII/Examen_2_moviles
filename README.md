# Notifire - Sistema de Autenticación y Notificaciones Push con Firebase

## Descripción

Notifire es una aplicación móvil nativa para Android que implementa un sistema de autenticación mediante Firebase y gestión de notificaciones push entre usuarios. La aplicación permite dos tipos de usuarios con diferentes niveles de acceso: usuarios normales y administradores, quienes pueden enviar notificaciones a usuarios específicos o a todos los usuarios registrados.

## Características Principales

### Autenticación
- Registro e inicio de sesión mediante Firebase Authentication
- Dos tipos de usuarios: Usuario normal y Administrador
- Protección mediante contraseña maestra para la creación de cuentas de administrador
- Almacenamiento de información de usuarios en Firebase Firestore

### Notificaciones Push
- Implementación completa de Firebase Cloud Messaging (FCM)
- Visualización de notificaciones push en la aplicación
- Interfaz para que administradores envíen notificaciones
- Historial de notificaciones recibidas
- Cloud Functions para procesamiento serverless de notificaciones

### Interfaz y Experiencia de Usuario
- Diseño intuitivo adaptado a diferentes roles de usuario
- Navegación clara entre secciones
- Adaptación responsive a diferentes tamaños de pantalla
- Persistencia de sesión y estado

## Tecnologías Utilizadas

- **Lenguaje**: Kotlin
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Firebase**:
  - Authentication: Gestión de usuarios y autenticación
  - Firestore: Base de datos NoSQL para almacenamiento de datos
  - Cloud Messaging (FCM): Gestión de notificaciones push
  - Cloud Functions: Procesamiento serverless para notificaciones

## Estructura del Proyecto

```
app/src/main/java/com/example/notifire/
├── admin                      # Funcionalidades de administrador
│   ├── FCMSender.kt           # Envío de notificaciones FCM
│   ├── LocalNotificationManager.kt
│   └── NotificationPanelActivity.kt
├── auth                       # Sistema de autenticación
│   ├── AuthViewModel.kt       # ViewModel para autenticación
│   ├── LoginActivity.kt       # Pantalla de inicio de sesión
│   └── RegisterActivity.kt    # Pantalla de registro
├── common                     # Clases de utilidad
│   ├── Constants.kt           # Constantes globales
│   └── SessionManager.kt      # Gestión de sesiones
├── data                       # Capa de datos
│   ├── model                  # Modelos de datos
│   │   ├── Notification.kt
│   │   └── User.kt
│   └── FirebaseRepository.kt
├── fcm                        # Implementación de FCM
│   └── MyFirebaseMessagingService.kt
├── home                       # Pantalla principal
│   └── HomeActivity.kt
├── notifications              # Gestión de notificaciones
│   └── NotificationHistoryActivity.kt
├── profile                    # Perfil de usuario
│   ├── EditProfileActivity.kt
│   └── ProfileActivity.kt
└── utils                      # Utilidades
    ├── NetworkUtils.kt
    └── Validator.kt
```

## Roles de Usuario

### Usuario Normal
- Registro e inicio de sesión
- Visualización y edición de su información personal
- Recepción de notificaciones push
- Visualización del historial de notificaciones recibidas

### Administrador
- Todas las funcionalidades del usuario normal
- Visualización de la lista de usuarios registrados
- Envío de notificaciones push a:
  - Un usuario específico
  - Varios usuarios seleccionados
  - Todos los usuarios registrados

## Configuración e Instalación

### Requisitos Previos
- Android Studio Arctic Fox (2021.3.1) o superior
- JDK 11 o superior
- Cuenta de Firebase para la configuración del proyecto

### Pasos de Configuración

1. **Clonar el repositorio**:
   ```
   git clone https://github.com/GioSENPAII/Examen_2_moviles.git
   ```

2. **Configurar Firebase**:
   - Crear un proyecto en la consola de Firebase
   - Agregar una aplicación Android con el paquete `com.example.notifire`
   - Descargar el archivo `google-services.json` y colocarlo en la carpeta `app/`
   - Habilitar Authentication (Email/Password)
   - Configurar Firestore Database
   - Configurar Cloud Messaging
   - Desplegar Cloud Functions

3. **Ejecutar la aplicación**:
   - Abrir el proyecto en Android Studio
   - Sincronizar con Gradle
   - Ejecutar en un emulador o dispositivo físico

## Uso de la Aplicación

1. **Registro de Usuario**:
   - Abrir la aplicación
   - Seleccionar "Registrarse"
   - Completar los campos requeridos
   - Para crear un administrador, activar el switch y proporcionar la contraseña maestra (default: "2025")

2. **Inicio de Sesión**:
   - Ingresar correo electrónico y contraseña
   - La aplicación redirige al Home según el rol del usuario

3. **Envío de Notificaciones (Administrador)**:
   - Acceder al "Panel de Notificaciones"
   - Seleccionar destinatarios de la lista
   - Completar título y mensaje
   - Presionar "Enviar"

4. **Visualización de Historial**:
   - Acceder a "Historial de Notificaciones"
   - Revisar las notificaciones recibidas ordenadas cronológicamente

## Estructura de la Base de Datos

### Firestore

- **Colección "users"**: Almacena información de los usuarios
  - Documento ID: UID del usuario
  - Campos: name, email, role, uid

- **Colección "tokens"**: Almacena tokens FCM para notificaciones
  - Documento ID: UID del usuario
  - Campos: token, updatedAt

- **Colección "notifications"**: Almacena notificaciones por usuario
  - Documento ID: UID del usuario
  - Subcolección "items": Notificaciones individuales
    - Campos: title, message, timestamp

- **Colección "notification_requests"**: Solicitudes de notificaciones para Cloud Functions
  - Campos: title, message, tokens/topic, processed, timestamp

## Seguridad

- Autenticación segura mediante Firebase Authentication
- Contraseña maestra para la creación de cuentas de administrador
- Validación de entradas en cliente y servidor
- Persistencia de sesión segura

## Solución de Problemas

### Notificaciones no recibidas
- Verificar conexión a internet
- Comprobar que el token FCM esté correctamente registrado
- Revisar logs de Cloud Functions para errores

### Problemas de autenticación
- Verificar credenciales
- Comprobar que el usuario esté correctamente creado en Firebase
- Verificar el formato del correo electrónico

## Contribuciones

Las contribuciones son bienvenidas. Para contribuir:
1. Haz fork del repositorio
2. Crea una rama para tu funcionalidad (`git checkout -b feature/nueva-funcionalidad`)
3. Haz commit de tus cambios (`git commit -am 'Agrega nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crea un Pull Request

## Autor

Giovanni Javier Longoria Bunoust - Estudiante de Ingeniería en Sistemas Computacionales
Instituto Politécnico Nacional - Escuela Superior de Cómputo

---

*Este proyecto fue desarrollado como parte del segundo examen parcial de la materia de Desarrollo de Aplicaciones Móviles Nativas, Plan de Estudios 2020, Instituto Politécnico Nacional - ESCOM.*
