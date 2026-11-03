# Sync setup (free Firebase Realtime Database)
1. console.firebase.google.com → Add project (Analytics off).
2. Build → Realtime Database → Create database (locked mode).
3. Rules tab → paste `firebase-rules.json` → Publish.
4. Copy the database URL (https://…-default-rtdb.firebaseio.com).
5. In the app on phone 1: Sync across devices → paste URL → Make a code → Save & sync.
6. On phone 2: same URL, same code.
Anyone with the URL and code can read and edit the data – keep the code private.
