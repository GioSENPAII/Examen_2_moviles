const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Función simple para probar
exports.helloWorld = functions.https.onCall((data, context) => {
  return {
    message: '¡Hola desde Cloud Functions!',
  };
});

// Función que se activa cuando se crea un documento en notification_requests
exports.processNotificationRequest = functions.firestore
  .document('notification_requests/{requestId}')
  .onCreate(async (snapshot, context) => {
    const requestData = snapshot.data();
    
    // Si el documento ya ha sido procesado, no hacer nada
    if (requestData.processed === true) {
      return null;
    }
    
    try {
      // Si la notificación es para un tema
      if (requestData.topic) {
        const payload = {
          notification: {
            title: requestData.title,
            body: requestData.message
          }
        };
        
        // Enviar notificación al tema
        await admin.messaging().sendToTopic(requestData.topic, payload);
        
        // Marcar como procesado
        await snapshot.ref.update({
          processed: true,
          processingTimestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        
        return {success: true, type: 'topic'};
      }
      
      // Si la notificación es para tokens específicos
      else if (requestData.tokens && Array.isArray(requestData.tokens)) {
        const payload = {
          notification: {
            title: requestData.title,
            body: requestData.message
          }
        };
        
        // Enviar notificación a los tokens
        const response = await admin.messaging().sendMulticast({
          tokens: requestData.tokens,
          notification: payload.notification
        });
        
        // Marcar como procesado
        await snapshot.ref.update({
          processed: true,
          processingTimestamp: admin.firestore.FieldValue.serverTimestamp(),
          successCount: response.successCount,
          failureCount: response.failureCount
        });
        
        return {
          success: true, 
          type: 'tokens', 
          successCount: response.successCount, 
          failureCount: response.failureCount
        };
      }
      
      return {success: false, error: 'Invalid request data'};
    } catch (error) {
      console.error('Error processing notification request:', error);
      
      // Marcar como fallido
      await snapshot.ref.update({
        processed: true,
        error: error.message,
        processingTimestamp: admin.firestore.FieldValue.serverTimestamp()
      });
      
      return {success: false, error: error.message};
    }
  });