   import * as functions from "firebase-functions/v1";
   import * as admin from "firebase-admin";

   const db = admin.firestore();

   export const searchUsers = functions.https.onCall(async (payload, context) => {
     const uid = context.auth?.uid;
     if (!uid) {
       throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
     }

     const { query, searchType, organizationId } = payload || {};
     const trimmed = (query ?? "").trim();
     if (!trimmed) {
       throw new functions.https.HttpsError("invalid-argument", "Query cannot be blank.");
     }

     let q: any = db.collection("users");
     if (searchType === "EMAIL") {
       q = q
         .where("email", ">=", trimmed)
         .where("email", "<", trimmed + "\uf8ff");
     } else if (searchType === "NAME") {
       q = q
         .where("displayName", ">=", trimmed)
         .where("displayName", "<", trimmed + "\uf8ff");
     } else {
       throw new functions.https.HttpsError("invalid-argument", "Invalid searchType.");
     }

     const snapshot = await q.limit(50).get();
     const results = snapshot.docs.map((d: any) => ({ id: d.id, ...d.data() }));

     // If organizationId is not null, exclude all users in the organization
     let exclude = new Set<string>();
     if (organizationId) {
       const membershipsSnap = await db
         .collection("memberships")
         .where("organizationId", "==", organizationId)
         .get();
       exclude = new Set(membershipsSnap.docs.map((d) => d.data().userId));
     }

     const users = results
       .filter((u: any) => !exclude.has(u.id))
       .map((u: any) => ({
         id: u.id,
         email: u.email ?? "",
         displayName: u.displayName ?? "",
         avatarUrl: u.avatarUrl ?? null,
       }));

     return { users };
   });