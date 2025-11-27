const nacl = require('tweetnacl');
const util = require('tweetnacl-util');

const keypair = nacl.sign.keyPair();

console.log('=== Ed25519 Keypair ===');
console.log('Public Key (base64):', util.encodeBase64(keypair.publicKey));
console.log('Private Key (base64):', util.encodeBase64(keypair.secretKey));
console.log('\nCopy these to Firestore!');