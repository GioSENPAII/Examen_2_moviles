const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendNotification = functions.https.onRequest(async (req, res) => {
    try {
        const { title, message, tokens } = req.body;

        if (!title || !message || !Array.isArray(tokens)) {
            return res.status(400).send("Datos incompletos");
        }

        const payload = {
            notification: {
                title,
                body: message
            },
            tokens: tokens
        };

        const response = await admin.messaging().sendMulticast(payload);

        return res.status(200).json({
            successCount: response.successCount,
            failureCount: response.failureCount
        });
    } catch (error) {
        console.error("Error enviando notificación:", error);
        return res.status(500).send("Error interno");
    }
});
