// Prépare le dossier www/ : application, hls.js (secours web) et logo intégré.
// La photo de l'équipe (resources/team.jpg) est copiée à part : le splash la charge par son URL.
const fs = require('fs');
const path = require('path');
const root = __dirname;
const www = path.join(root, 'www');

fs.rmSync(www, { recursive: true, force: true });
fs.mkdirSync(www, { recursive: true });

const tplPath = path.join(root, 'index_template.html');
if (!fs.existsSync(tplPath)) { console.error('index_template.html introuvable.'); process.exit(1); }
let html = fs.readFileSync(tplPath, 'utf8');

const logoPath = path.join(root, 'resources', 'logo.png');
if (!fs.existsSync(logoPath)) { console.error('resources/logo.png introuvable.'); process.exit(1); }
const logoB64 = fs.readFileSync(logoPath).toString('base64');
html = html.replace('%%LOGO%%', logoB64);
html = html.replace('%%HLS%%', '<script src="hls.min.js"></script>');

fs.writeFileSync(path.join(www, 'index.html'), html);

const hls = path.join(root, 'node_modules', 'hls.js', 'dist', 'hls.min.js');
if (!fs.existsSync(hls)) { console.error('hls.js introuvable : lancez "npm install" d\'abord.'); process.exit(1); }
fs.copyFileSync(hls, path.join(www, 'hls.min.js'));

const team = path.join(root, 'resources', 'team.jpg');
if (fs.existsSync(team)) { fs.copyFileSync(team, path.join(www, 'team.jpg')); console.log('Photo de l\'équipe intégrée.'); }
else console.log('Pas de resources/team.jpg : l\'écran de lancement utilisera un fond bleu uni.');

console.log('www/ prêt.');
