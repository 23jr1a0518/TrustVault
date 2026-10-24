# TrustVault Quantum Sentinel — Vercel Frontend Deployment

This directory contains the production static frontend for **TrustVault Quantum Sentinel**.

## 🚀 Deploying to Vercel

### Option 1: Automatic Reverse-Proxy (Recommended - First-Party Cookies)
1. In `vercel.json` (or your Vercel Project Settings $\rightarrow$ Environment Variables), update the destination URL to match your live Render backend:
   ```json
   {
     "rewrites": [
       {
         "source": "/api/:path*",
         "destination": "https://<your-render-service>.onrender.com/api/:path*"
       }
     ]
   }
   ```
2. In Vercel:
   - Import Git Repository `23jr1a0518/TrustVault`
   - Set **Root Directory** to `frontend` (or `src/main/resources/static`)
   - Click **Deploy**!

### Option 2: Direct Cross-Origin API
If you prefer direct cross-origin communication without Vercel rewrites:
1. Open your browser console on the Vercel site once deployed:
   ```javascript
   localStorage.setItem('TRUSTVAULT_API_BASE', 'https://<your-render-service>.onrender.com');
   ```
2. Or declare `window.TRUSTVAULT_API_BASE = 'https://<your-render-service>.onrender.com';` before `app.js` loads.
3. On Render, set environment variable:
   ```env
   SESSION_COOKIE_SAMESITE=none
   SESSION_COOKIE_SECURE=true
   ```
   *(The backend already permits all `https://*.vercel.app` origins via Spring Security CORS).*
