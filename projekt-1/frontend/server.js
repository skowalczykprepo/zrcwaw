// Produkcyjny serwer Node: serwuje zbudowany frontend (dist/) i wstrzykuje konfiguracje srodowiskowa.
import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dist = path.join(__dirname, 'dist');

const PORT = process.env.PORT || 3000;
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';

const app = express();

// Konfiguracja czytana przez przegladarke przy starcie aplikacji - dzieki temu ten sam obraz Dockera
// dziala lokalnie i w chmurze, roznia sie tylko zmienna srodowiskowa.
app.get('/config.js', (_req, res) => {
  res.type('application/javascript');
  res.send(`window.APP_CONFIG = ${JSON.stringify({ API_BASE_URL })};`);
});

app.get('/health', (_req, res) => res.json({ status: 'UP' }));

app.use(express.static(dist));
app.get('*', (_req, res) => res.sendFile(path.join(dist, 'index.html')));

app.listen(PORT, () => {
  console.log(`Frontend listening on port ${PORT}, API_BASE_URL=${API_BASE_URL}`);
});
