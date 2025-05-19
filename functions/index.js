// Archivo: functions/index.js

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Enviar notificación a tokens específicos
exports.sendNotification = functions.https.onCall(async (data, context) => {
    // Verificar si el usuario está autenticado
    if (!context.auth) {
        throw new functions.https.HttpsError(
            'unauthenticated',
            'El usuario debe estar autenticado para enviar notificaciones'
        );
    }

    // Verificar si el usuario es administrador
    const userDoc = await admin.firestore().collection('users').doc(context.auth.uid).get();
    if (!userDoc.exists || userDoc.data().role !== 'admin') {
        throw new functions.https.HttpsError(
            'permission-denied',
            'Solo los administradores pueden enviar notificaciones'
        );
    }

    const { title, message, tokens } = data;

    if (!title || !message || !tokens || !Array.isArray(tokens) || tokens.length === 0) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Se requieren título, mensaje y al menos un token válido'
        );
    }

    try {
        // Crear mensaje de notificación
        const payload = {
            notification: {
                title: title,
                body: message
            }
        };

        // Enviar notificación a múltiples dispositivos
        const response = await admin.messaging().sendMulticast({
            tokens: tokens,
            notification: {
                title: title,
                body: message
            }
        });

        // Guardar notificación en Firestore para cada usuario
        const batch = admin.firestore().batch();
        for (const token of tokens) {
            // Buscar el usuario por token
            const tokenSnapshot = await admin.firestore().collection('tokens')
                .where('token', '==', token).get();
            
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
        }
        
        await batch.commit();

        return {
            success: true,
            successCount: response.successCount,
            failureCount: response.failureCount
        };
    } catch (error) {
        console.error("Error enviando notificación:", error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Enviar notificación a todos los usuarios suscritos a un tema
exports.sendNotificationToTopic = functions.https.onCall(async (data, context) => {
    // Verificar si el usuario está autenticado
    if (!context.auth) {
        throw new functions.https.HttpsError(
            'unauthenticated',
            'El usuario debe estar autenticado para enviar notificaciones'
        );
    }

    // Verificar si el usuario es administrador
    const userDoc = await admin.firestore().collection('users').doc(context.auth.uid).get();
    if (!userDoc.exists || userDoc.data().role !== 'admin') {
        throw new functions.https.HttpsError(
            'permission-denied',
            'Solo los administradores pueden enviar notificaciones'
        );
    }

    const { title, message, topic } = data;

    if (!title || !message || !topic) {
        throw new functions.https.HttpsError(
            'invalid-argument',
            'Se requieren título, mensaje y un tema válido'
        );
    }

    try {
        // Crear mensaje de notificación
        const payload = {
            notification: {
                title: title,
                body: message
            }
        };

        // Enviar notificación al tema
        await admin.messaging().sendToTopic(topic, payload);

        // Guardar la notificación en Firestore para todos los usuarios
        const usersSnapshot = await admin.firestore().collection('users').get();
        const batch = admin.firestore().batch();
        
        usersSnapshot.forEach(doc => {
            const userId = doc.id;
            const notifRef = admin.firestore().collection('notifications')
                .doc(userId).collection('items').doc();
            
            batch.set(notifRef, {
                title: title,
                message: message,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
        });
        
        await batch.commit();

        return { success: true };
    } catch (error) {
        console.error("Error enviando notificación:", error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});

// Función que se ejecuta cuando un nuevo usuario se registra
exports.onUserCreated = functions.auth.user().onCreate(async (user) => {
    try {
        // Verificar si el usuario ya existe en Firestore
        const userDoc = await admin.firestore().collection('users').doc(user.uid).get();
        
        // Si no existe, añadir un usuario con rol 'user' por defecto
        if (!userDoc.exists) {
            await admin.firestore().collection('users').doc(user.uid).set({
                uid: user.uid,
                email: user.email,
                name: user.displayName || 'Usuario',
                role: 'user',
                createdAt: admin.firestore.FieldValue.serverTimestamp()
            });
        }
        
        // Suscribir al usuario al tema 'all_users' para notificaciones globales
        const token = await admin.firestore().collection('tokens').doc(user.uid).get();
        if (token.exists && token.data().token) {
            await admin.messaging().subscribeToTopic(token.data().token, 'all_users');
        }
        
        return { success: true };
    } catch (error) {
        console.error("Error al crear usuario:", error);
        return { success: false, error: error.message };
    }
});