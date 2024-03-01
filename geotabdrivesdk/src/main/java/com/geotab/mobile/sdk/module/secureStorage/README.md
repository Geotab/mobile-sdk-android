# LocalStorage
Module for storing data with encryption. 

Even though the data stored locally in the device is only accessible by the app, it is always recommended to encrypt sensitive data.

## Approach
Exposed functions for web storage methods(https://developer.mozilla.org/en-US/docs/Web/API/Storage#methods) to store into device's SQLite database.

SQLite database has a table to store list of key, value pairs, similar to web Local storage. The key is unique and is used to retrieve the value.

Before, storing into the SQLite database, the value is encrypted with a secretkey from Android Keystore. The secretkey to encrypt or decrypt is only accessible by the app. Created a property for keyAlias which can be set by implementor to cipher the data. The keyAlias is used to create/retrieve the secretkey from the Android Keystore.

