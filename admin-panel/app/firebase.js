// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyC1RCezrBWrEXbu95JI2__yRkgwxEu5xqI",
  authDomain: "genghealth-b0ff6.firebaseapp.com",
  projectId: "genghealth-b0ff6",
  storageBucket: "genghealth-b0ff6.firebasestorage.app",
  messagingSenderId: "485873590510",
  appId: "1:485873590510:web:0461e0bcbf88ed43dd93ff",
  measurementId: "G-HKBY3H5MJ0"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

export { db };