const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Función simple para probar
exports.helloWorld = functions.https.onCall((data, context) => {
  return {
    message: "¡Hola desde Cloud Functions!"
  };
});

// Enviar notificación a tokens específicos
exports.sendNotification = functions.https.onCall((data, context) => {
  // Verificar autenticación
  if (!context || !context.auth) {
    return {
      success: false,
      error: 'Necesitas estar autenticado para realizar esta acción'
    };
  }

  return admin.firestore().collection('users').doc(context.auth.uid).get()
    .then(userDoc => {
      // Verificar si es administrador
      if (!userDoc.exists || userDoc.data().role !== 'admin') {
        throw new functions.https.HttpsError(
          'permission-denied',
          'Solo los administradores pueden enviar notificaciones'
        );
      }

      const { title, message, tokens } = data;

      // Validar datos
      if (!title || !message || !tokens || !Array.isArray(tokens) || tokens.length === 0) {
        throw new functions.https.HttpsError(
          'invalid-argument',
          'Se requieren título, mensaje y al menos un token válido'
        );
      }

      // Crear payload de notificación
      const payload = {
        notification: {
          title: title,
          body: message
        }
      };

      // Enviar notificación
      return admin.messaging().sendMulticast({
        tokens: tokens,
        notification: payload.notification
      })
        .then(response => {
          // Guardar notificaciones en Firestore
          const batch = admin.firestore().batch();
          const promises = [];

          // Para cada token, buscar el usuario correspondiente
          tokens.forEach(token => {
            promises.push(
              admin.firestore().collection('tokens')
                .where('token', '==', token).get()
                .then(tokenSnapshot => {
                  if (!tokenSnapshot.empty) {
                    tokenSnapshot.forEach(doc => {
                      const userId = doc.id;
                      const notifRef = admin.firestore().collection('notifications')
                        .doc(userId).collection('items').doc();
                      
                      batch.set(notifRef, {
                        title: title,
                        message: message,
                        timestamp: admin.firestore.FieldValue.serverTimestamp()
                      });
                    });
                  }
                })
            );
          });

          // Esperar a que se resuelvan todas las promesas y hacer commit del batch
          return Promise.all(promises)
            .then(() => batch.commit())
            .then(() => {
              return {
                success: true,
                successCount: response.successCount,
                failureCount: response.failureCount
              };
            });
        });
    })
    .catch(error => {
      console.error("Error en sendNotification:", error);
      throw new functions.https.HttpsError('internal', error.message);
    });
});

// Función para enviar notificación a todos los usuarios
exports.sendNotificationToTopic = functions.https.onCall((data, context) => {
    // Verificar autenticación
    if (!context || !context.auth) {
      return {
        success: false,
        error: 'Necesitas estar autenticado para realizar esta acción'
      };
    }
  
    return admin.firestore().collection('users').doc(context.auth.uid).get()
      .then(userDoc => {
        // Verificar si es administrador
        if (!userDoc.exists || userDoc.data().role !== 'admin') {
          throw new functions.https.HttpsError(
            'permission-denied',
            'Solo los administradores pueden enviar notificaciones'
          );
        }
  
        const { title, message, topic } = data;
  
        // Validar datos
        if (!title || !message || !topic) {
          throw new functions.https.HttpsError(
            'invalid-argument',
            'Se requieren título, mensaje y un tema válido'
          );
        }
  
        // Crear payload de notificación
        const payload = {
          notification: {
            title: title,
            body: message
          }
        };
  
        // Enviar notificación al tema
        return admin.messaging().sendToTopic(topic, payload)
          .then(() => {
            // Guardar notificación para todos los usuarios
            return admin.firestore().collection('users').get()
              .then(usersSnapshot => {
                const batch = admin.firestore().batch();
                
                usersSnapshot.forEach(doc => {
                  if (doc && doc.id) {
                    const userId = doc.id;
                    const notifRef = admin.firestore().collection('notifications')
                      .doc(userId).collection('items').doc();
                    
                    batch.set(notifRef, {
                      title: title,
                      message: message,
                      timestamp: admin.firestore.FieldValue.serverTimestamp()
                    });
                  }
                });
                
                return batch.commit();
              })
              .then(() => {
                return { success: true };
              });
          });
      })
      .catch(error => {
        console.error("Error en sendNotificationToTopic:", error);
        throw new functions.https.HttpsError('internal', error.message);
      });
  });

  // Función que se ejecuta cuando un nuevo usuario se registra
exports.onUserCreated = functions.auth.user().onCreate(userRecord => {
    // Validar que userRecord existe
    if (!userRecord) {
      console.error("Error: userRecord es undefined");
      return Promise.resolve({ success: false, error: "Usuario indefinido" });
    }
  
    const uid = userRecord.uid;
    const email = userRecord.email || '';
    const displayName = userRecord.displayName || 'Usuario';
  
    // Verificar si el usuario ya existe en Firestore
    return admin.firestore().collection('users').doc(uid).get()
      .then(userDoc => {
        // Solo crear si no existe
        if (!userDoc.exists) {
          return admin.firestore().collection('users').doc(uid).set({
            uid: uid,
            email: email,
            name: displayName,
            role: 'user',
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
        return Promise.resolve();
      })
      .then(() => {
        // Intentar suscribir al usuario al tema 'all_users'
        return admin.firestore().collection('tokens').doc(uid).get()
          .then(tokenDoc => {
            if (tokenDoc.exists && tokenDoc.data() && tokenDoc.data().token) {
              return admin.messaging().subscribeToTopic(tokenDoc.data().token, 'all_users');
            }
            return Promise.resolve();
          })
          .catch(error => {
            console.error("Error al suscribir al tema:", error);
            return Promise.resolve(); // Continuar a pesar del error
          });
      })
      .then(() => {
        return { success: true };
      })
      .catch(error => {
        console.error("Error al crear usuario:", error);
        return { success: false, error: error.message };
      });
  });